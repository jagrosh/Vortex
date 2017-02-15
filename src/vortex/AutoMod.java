/*
 * Copyright 2016 John Grosh (jagrosh).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package vortex;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.role.GenericRoleEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.entities.Invite;
import vortex.utils.RestUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutoMod extends ListenerAdapter {
    private final Pattern INVITES = Pattern.compile("discord(?:\\.gg|app.com\\/invite)\\/([A-Z0-9_]+)",Pattern.CASE_INSENSITIVE);
    private final int MENTION_MINIMUM = 6;
    private final SimpleLog LOG = SimpleLog.getLog("AutoMod");
    private final HashMap<String,Integer> antimention = new HashMap<>();
    private final HashMap<String,Action> antiinvite = new HashMap<>();
    private final HashMap<String,OffsetDateTime> warnings = new HashMap<>();
    private final HashMap<String,List<String>> nojoins = new HashMap<>();
    private final HashMap<String,Combo> antispam = new HashMap<>();
    private final HashMap<String,SpamStatus> spams = new HashMap<>();
    private final ScheduledExecutorService threadpool;
    private final List<JDA> jdas;
    private JDA thisJda;
        
    public AutoMod(ScheduledExecutorService threadpool, List<JDA> jdas)
    {
        this.threadpool = threadpool;
        this.jdas = jdas;
        jdas.forEach(jda -> jda.addEventListener(new ServerBlocker(this)));
    }
    
    public void reloadJDAs()
    {
        List<String> tokens = jdas.stream().map(jda -> jda.getToken()).collect(Collectors.toList());
        jdas.stream().forEach(jda -> jda.shutdown(false));
        jdas.clear();
        tokens.stream().forEach(str -> {
            try {
                jdas.add(new JDABuilder(AccountType.CLIENT).addListener(new ServerBlocker(this)).setToken(str).buildAsync());
            } catch (LoginException | IllegalArgumentException | RateLimitedException ex) {
                SimpleLog.getLog("Login").fatal(ex);
            }
        });
    }
    
    public String getSettings(Guild guild)
    {
        TextChannel channel = ModLogger.getLogChannel(guild);
        return "AntiMention: "+(antimention.containsKey(guild.getId()) ? "**"+antimention.get(guild.getId())+"**" : "not enabled")
                +"\nAntiInvite: "+(antiinvite.containsKey(guild.getId()) ? "**"+antiinvite.get(guild.getId()).name()+"**" : "not enabled")
                +"\nAntiSpam: "+(antispam.containsKey(guild.getId()) ? "**"+antispam.get(guild.getId())+"**" : "not enabled")
                +"\nModlog Channel: "+(channel==null ? "not enabled" : channel.getAsMention())
                ;
    }

    @Override
    public void onReady(ReadyEvent event) {
        thisJda = event.getJDA();
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onResume(ResumedEvent event) {
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        threadpool.shutdown();
        jdas.forEach(jda -> jda.shutdown());
    }

    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if(event.getMember().getUser().isBot())
            return;
        List<String> bannedServers = nojoins.get(event.getGuild().getId());
        if(bannedServers!=null)
        {
            for(String id: bannedServers)
            {
                for(JDA jda: jdas)
                {
                    Guild g = jda.getGuildById(id);
                    if(g!=null && g.getMemberById(event.getMember().getUser().getId())!=null)
                    {
                        event.getMember().getUser().openPrivateChannel()
                                .queue(pc -> pc.sendMessage("Sorry, you cannot join this server, **"+event.getGuild().getName()
                                        +"**, because you are a member of **"+g.getName()+"**. Please leave **"
                                        +g.getName()+"** if you would like to join this server.")
                                        .queue(m -> event.getGuild().getController().kick(event.getMember()).queue(v -> ModLogger.logAction(Action.KICK, event.getMember(), "being on "+g.getName()+" ("+g.getId()+")")), 
                                               t -> event.getGuild().getController().kick(event.getMember()).queue(v -> ModLogger.logAction(Action.KICK, event.getMember(), "being on "+g.getName()+" ("+g.getId()+")"))), 
                                t -> event.getGuild().getController().kick(event.getMember()).queue(v -> ModLogger.logAction(Action.KICK, event.getMember(), "being on "+g.getName()+" ("+g.getId()+")")));
                        return;
                    }
                }
            }
        }
    }
    
    public void onBlockedGuildJoin(GuildMemberJoinEvent event)
    {
        User user = thisJda.getUserById(event.getMember().getUser().getId());
        if(user==null)
            return;
        List<Guild> block = thisJda.getGuilds().stream().filter(g -> g.isMember(user) && nojoins.containsKey(g.getId()) && nojoins.get(g.getId()).contains(event.getGuild().getId()))
                .collect(Collectors.toList());
        if(!block.isEmpty())
        {
            String msg = "You have just joined the server **"+event.getGuild().getName()+"**. You will be removed from the following servers "
                    + "in __15__ minutes if you do not leave **"+event.getGuild().getName()+"**:";
            msg = block.stream().map((g) -> "\n> **"+g.getName()+"**").reduce(msg, String::concat);
            String str = msg;
            user.openPrivateChannel().queue(pc -> pc.sendMessage(str).queue(m -> scheduleRemoval(user.getId(), event.getGuild().getId()), 
                    t -> scheduleRemoval(user.getId(), event.getGuild().getId())), t -> scheduleRemoval(user.getId(), event.getGuild().getId()));
        }
    }
    
    private void scheduleRemoval(String userId, String blockedGuildId)
    {
        threadpool.schedule(() -> {
            jdas.stream().filter(jda -> jda.getGuildById(blockedGuildId)!=null).findAny().ifPresent(jda -> {
                String name = jda.getGuildById(blockedGuildId).getName();
                if(jda.getGuildById(blockedGuildId).getMemberById(userId)!=null)
                {
                    User user = thisJda.getUserById(userId);
                    if(user!=null)
                    {
                        List<Guild> block = thisJda.getGuilds().stream().filter(g -> g.isMember(user) && nojoins.containsKey(g.getId()) && nojoins.get(g.getId()).contains(blockedGuildId))
                            .collect(Collectors.toList());
                        block.stream().forEach(g -> {
                            try {
                                Member m = g.getMember(user);
                                g.getController().kick(m).queue(v -> ModLogger.logAction(Action.KICK, m, "being on "+name+" ("+blockedGuildId+")"));
                            }catch(PermissionException e){}
                        });
                    }
                }
            });
        }, 15, TimeUnit.MINUTES);
    }
    
    public List<String> getBlockedMembers(Guild guild)
    {
        if(!nojoins.containsKey(guild.getId()))
            return Collections.EMPTY_LIST;
        List<String> list = new LinkedList<>();
        nojoins.get(guild.getId()).stream().forEach(guildid -> {
            jdas.stream().filter(jda -> jda.getGuildById(guildid)!=null).findAny().ifPresent(jda -> {
                Guild blocked = jda.getGuildById(guildid);
                blocked.getMembers().stream().filter(mem -> guild.getMemberById(mem.getUser().getId())!=null)
                        .forEach(mem -> list.add("<@"+mem.getUser().getId()+"> ("+blocked.getName()+")"));
            });
        });
        return list;
    }
    
    public List<Member> getBlockedKickableMembers(Guild guild, String blockedId)
    {
        if(!nojoins.containsKey(guild.getId()))
            return Collections.EMPTY_LIST;
        if(!nojoins.get(guild.getId()).contains(blockedId))
            return Collections.EMPTY_LIST;
        List<Member> list = new LinkedList<>();
        jdas.stream().filter(jda -> jda.getGuildById(blockedId)!=null).findAny().ifPresent(jda -> {
            Guild blocked = jda.getGuildById(blockedId);
            blocked.getMembers().stream().filter(mem -> guild.getMemberById(mem.getUser().getId())!=null)
                    .forEach(mem -> list.add(guild.getMemberById(mem.getUser().getId())));
        });
        return list;
    }
    
    public Guild findGuildById(String id)
    {
        return jdas.stream().map(jda -> jda.getGuildById(id)).filter(g -> g!=null).findAny().orElse(null);
    }
    
    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        performAutomod(event);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        performAutomod(event);
    }
    
    public void performAutomod(GenericGuildMessageEvent event) {
        //simple automod
        LOG.trace("Message recieved: "+event.getMessage().getId()+" "+event.getGuild()+" "+event.getAuthor());
        //ignore bots
        if(event.getAuthor().isBot())
            return;
        
        Member me = event.getGuild().getSelfMember();
        
        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if(!PermissionUtil.canInteract(me, event.getMember()) ||
                event.getMember().getRoles().stream().anyMatch(r -> r.getName().toLowerCase().equals("vortexshield")) ||
                PermissionUtil.checkPermission(event.getChannel(), event.getMember(), Permission.MESSAGE_MANAGE) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.KICK_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.BAN_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.MANAGE_SERVER))
            return;
        LOG.trace("User not ignored.");
        /*
            check for automod actions
            * AntiSpam - prevent repeated messages
            * AntiMention - prevent mass-mention spammers
            * AntiInvite - prevent invite links to other servers
        */
        
        boolean shouldDelete = false;
        
        // anti-spam
        Combo combo = antispam.get(event.getGuild().getId());
        if(combo!=null && (event.getChannel().getTopic()==null || !event.getChannel().getTopic().toLowerCase().contains("{spam}")))
        {
            String key = event.getAuthor().getId()+"|"+event.getGuild().getId();
            SpamStatus status = spams.get(key);
            if(status==null)
            {
                spams.put(key, new SpamStatus(event.getMessage()));
            }
            else
            {
                int offenses = status.update(event.getMessage());
                switch(offenses) {
                    case 1:
                    case 2: break;
                    case 3:
                        shouldDelete = true;
                        break;
                    case 4:
                        shouldDelete = true;
                        event.getChannel().sendMessage(event.getMember().getAsMention()+": Please stop spamming. Your messages have been removed.").queue();
                        break;
                    default:
                        shouldDelete = true;
                        if(offenses >= combo.number)
                        {
                            switch(combo.action) {
                                case BAN:
                                    shouldDelete = false;
                                    event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                                        ModLogger.logAction(Action.BAN, event.getMessage(), "Spamming: "+event.getMessage().getRawContent());
                                    });
                                    break;
                                case KICK:
                                    event.getGuild().getController().kick(event.getMember()).queue(v -> {
                                        ModLogger.logAction(Action.KICK, event.getMessage(), "Spamming: "+event.getMessage().getRawContent());
                                    });
                                    break;
                                case MUTE:
                                    event.getGuild().getRoles().stream().filter(r -> r.getName().toLowerCase().equals("muted"))
                                            .findFirst().ifPresent(r -> event.getGuild().getController().addRolesToMember(event.getMember(), r).queue(s -> {
                                                ModLogger.logAction(Action.MUTE, event.getMessage(), "Spamming: "+event.getMessage().getRawContent());
                                            }));
                            }
                        }
                }
            }
        }
        
        // anti-mention
        int maxMentions = antimention.getOrDefault(event.getGuild().getId(), Integer.MAX_VALUE);
        LOG.trace("Valid maxMentions: "+maxMentions);
        long mentions = event.getMessage().getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(event.getAuthor())).count();
        if(mentions >= maxMentions && mentions >= MENTION_MINIMUM)
        {
            try{
                event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                        ModLogger.logAction(Action.BAN, event.getMessage(), "Mentioning "+mentions+" users");
                    });
                event.getChannel().sendMessage(event.getAuthor().getAsMention()+" has been banned for mentioning "+mentions+" users");
                LOG.trace("Banned mass-mentioner");
            } catch(Exception e){
                LOG.trace("Failed to ban mass-mentioner: "+e);
            }
        }
        
        // anti-invite
        Action type = antiinvite.get(event.getGuild().getId());
        if(type!=null && (event.getChannel().getTopic()==null || !event.getChannel().getTopic().toLowerCase().contains("{invites}")))
        {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(event.getMessage().getRawContent());
            while(m.find())
                invites.add(m.group(1));
            LOG.trace("Found "+invites.size()+" invites.");
            try{
                for(String inviteCode : invites)
                {
                    Invite invite = RestUtil.resolveInvite(inviteCode);
                    if(invite==null || !invite.getGuildId().equals(event.getGuild().getId()))
                    {
                        switch(type)
                        {
                            case BAN:
                                event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                                    ModLogger.logAction(Action.BAN, event.getMessage(), "Posting an invite link: http://discordapp.com/invite/"+inviteCode);
                                });
                                break;
                            case KICK:
                                shouldDelete = true;
                                event.getGuild().getController().kick(event.getMember()).queue(v -> {
                                    ModLogger.logAction(Action.KICK, event.getMessage(), "Posting an invite link: http://discordapp.com/invite/"+inviteCode);
                                });
                                break;
                            case MUTE:
                                shouldDelete = true;
                                event.getGuild().getRoles().stream().filter(r -> r.getName().toLowerCase().equals("muted"))
                                        .findFirst().ifPresent(r -> event.getGuild().getController().addRolesToMember(event.getMember(), r).queue(s -> {
                                            ModLogger.logAction(Action.MUTE, event.getMessage(), "Posting an invite link: http://discordapp.com/invite/"+inviteCode);
                                        }));
                                break;
                            case WARN:
                                shouldDelete = true;
                                String key = event.getAuthor().getId()+"|"+event.getGuild().getId();
                                OffsetDateTime lastWarning = warnings.get(key);
                                if(lastWarning==null || lastWarning.isBefore(event.getMessage().getCreationTime().minusMinutes(1)))
                                {
                                    event.getChannel().sendMessage(event.getMember().getAsMention()+": Please do not post invite links here.").queue();
                                    warnings.put(key, event.getMessage().getCreationTime());
                                }
                                break;
                        }
                        break;
                    }
                    else
                        LOG.trace("Invite resolved to current guild");
                }
            }catch(PermissionException ex){
                LOG.trace("Failed to perform action: "+ex);
            }
        }
        if(shouldDelete)
        {
            try{event.getMessage().deleteMessage().queue();}catch(PermissionException e){}
        }
    }

    @Override
    public void onGenericRole(GenericRoleEvent event) {
        updateRoleSettings(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            updateRoleSettings(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event)
    {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            updateRoleSettings(event.getGuild());
    }
    
    
    public void updateAllGuilds(JDA jda)
    {
        jda.getGuilds().stream().forEach(g -> {
            updateRoleSettings(g);
        });
    }
    
    public void updateRoleSettings(Guild guild)
    {
        antimention.remove(guild.getId());
        antiinvite.remove(guild.getId());
        nojoins.remove(guild.getId());
        antispam.remove(guild.getId());
        guild.getSelfMember().getRoles().stream().forEach(r -> {
            if(r.getName().toLowerCase().startsWith("antimention"))
            {
                try{
                    int maxmentions = Integer.parseInt(r.getName().split(":",2)[1].trim());
                    if(maxmentions>MENTION_MINIMUM)
                        antimention.put(guild.getId(), maxmentions);
                }catch(NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e){}
            }
            else if(r.getName().toLowerCase().startsWith("antiinvite"))
            {
                try{
                    String type = r.getName().split(":",2)[1].trim().toUpperCase();
                    switch(type) {
                        case "BAN":
                            antiinvite.put(guild.getId(), Action.BAN);
                            break;
                        case "KICK":
                            antiinvite.put(guild.getId(), Action.KICK);
                            break;
                        case "MUTE":
                            antiinvite.put(guild.getId(), Action.MUTE);
                            break;
                        case "WARN":
                            antiinvite.put(guild.getId(), Action.WARN);
                            break;
                        case "DELETE":
                            antiinvite.put(guild.getId(), Action.DELETE);
                            break;
                    }
                }catch(Exception e){}
            }
            else if(r.getName().toLowerCase().startsWith("antispam"))
            {
                try{
                    String[] parts = r.getName().split(":")[1].trim().split("\\|");
                    int num = Integer.parseInt(parts[1].trim());
                    if(num>4)
                    {
                        switch(parts[0].trim().toUpperCase()) {
                            case "BAN":
                                antispam.put(guild.getId(), new Combo(num, Action.BAN));
                                break;
                            case "KICK":
                                antispam.put(guild.getId(), new Combo(num, Action.KICK));
                                break;
                            case "MUTE":
                                antispam.put(guild.getId(), new Combo(num, Action.MUTE));
                                break;
                        }
                    }
                }catch(Exception e){}
            }
            else if(r.getName().toLowerCase().startsWith("blockserver"))
            {
                try{
                    String id = r.getName().split(":")[1].trim();
                    if(id.matches("\\d+"))
                    {
                        List<String> list = nojoins.get(guild.getId());
                        if(list==null)
                        {
                            list = new ArrayList<>();
                            nojoins.put(guild.getId(), list);
                        }
                        list.add(id);
                    }
                }catch(Exception e){}
            }
        });
    }
    
    private class Combo {
        public final int number;
        public final Action action;
        private Combo(int number, Action action)
        {
            this.number = number;
            this.action = action;
        }
        @Override
        public String toString() {
            return action.name()+" on "+number;
        }
    }
    
    private class SpamStatus {
        private String message;
        private OffsetDateTime time;
        private int count;
        private final Message[] list = new Message[2];
        private SpamStatus(Message message)
        {
            this.message = message.getRawContent().toLowerCase();
            list[0] = message;
            time = message.getCreationTime();
            count = 1;
        }
        private int update(Message message)
        {
            String lower = message.getRawContent().toLowerCase();
            if(lower.equals(this.message) && this.time.plusMinutes(1).isAfter(message.getCreationTime()))
            {
                count++;
                if(count==4)
                {
                    try{
                    list[0].deleteMessage().queue();
                    list[1].deleteMessage().queue();
                    }catch(Exception e){}
                }
                else if (count!=3)
                    list[1] = message;
                time = message.getCreationTime();
                return count;
            }
            else
            {
                this.message = lower;
                time = message.getCreationTime();
                count = 1;
                list[0] = message;
                list[1] = null;
                return 1;
            }
        }
    }
}
