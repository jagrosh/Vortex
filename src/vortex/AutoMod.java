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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.data.DMSpamManager;
import vortex.data.DatabaseManager;
import vortex.data.DatabaseManager.GuildSettings;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutoMod extends ListenerAdapter {
    private final Pattern INVITES = Pattern.compile("discord(?:\\.gg|app.com\\/invite)\\/([A-Z0-9_]{2,16})",Pattern.CASE_INSENSITIVE);
    public static final int MENTION_MINIMUM = 6;
    private final SimpleLog LOG = SimpleLog.getLog("AutoMod");
    
    private final HashMap<String,OffsetDateTime> warnings = new HashMap<>();
    private final HashMap<String,SpamStatus> spams = new HashMap<>();
    private final HashMap<String,RaidmodeStatus> raids = new HashMap<>();
    
    private final HashMap<String,StringBuilder> raidmode = new HashMap<>();
    
    private final ModLogger modlog;
    private final ScheduledExecutorService threadpool;
    private final DatabaseManager manager;
    private final DMSpamManager dmspam;
    
    private JDA jda;
        
    public AutoMod(ModLogger modlog, ScheduledExecutorService threadpool, DatabaseManager manager, DMSpamManager dmspam)
    {
        this.modlog = modlog;
        this.threadpool = threadpool;
        this.manager = manager;
        this.dmspam = dmspam;
        dmspam.registerAutoMod(this);
    }
    
    public String getSettings(Guild guild)
    {
        GuildSettings gs = manager.getSettings(guild);
        TextChannel channel = guild.getTextChannelById(gs.modlogChannelId);
        return "AntiMention: "+(gs.maxMentions>MENTION_MINIMUM ? "**"+gs.maxMentions+"**" : "not enabled")
                +"\nAntiInvite: "+(gs.inviteAction!=Action.NONE ? "**"+gs.inviteAction.name()+"**" : "not enabled")
                +"\nAntiSpam: "+(gs.spamLimit>0 && gs.spamAction!=Action.NONE ? "**"+gs.spamAction.name()+" on "+gs.spamLimit+"**" : "not enabled")
                +"\nModlog Channel: "+(channel==null ? "not enabled" : channel.getAsMention())
                +"\nAuto RaidMode: "+(gs.autoRaidMode ? "**Enabled**" : "not enabled")
                +"\nAntiDMSpam: "+(gs.blockDmSpam ? "**Enabled**" : "not enabled")
                ;
    }
    
    @Override
    public void onShutdown(ShutdownEvent event) {
        threadpool.shutdown();
        manager.shutdown();
    }

    public void onDMSpamGuildJoin(User u) {
        if(jda==null)
            return;
        User user = jda.getUserById(u.getId());
        if(user==null)
            return;
        List<Guild> spammers = dmspam.getDMSpamServers(user);
        List<Guild> kicks = manager.getDMSpamPreventionGuilds(jda);
        if(kicks.isEmpty())
            return;
        kicks = kicks.stream().filter((Guild g) -> {
            Member m = g.getMemberById(user.getId());
            if(m==null)
                return false;
            return shouldPerformAutomod(m, null);
        }).collect(Collectors.toList());
        if(kicks.isEmpty())
            return;
        StringBuilder sb = new StringBuilder();
        sb.append("Sorry, it looks like you are a member of the following server(s):\n");
        spammers.forEach(g -> sb.append("\n  **").append(g.getName()).append("**"));
        sb.append("\n\nThese server(s) are known to be the origin of large amounts of direct-message "
                + "spam and advertisements. Therefore, you will be kicked from the following servers in "
                + "**10** minutes unless you leave the above servers:\n");
        kicks.forEach(g -> sb.append("\n  **").append(g.getName()).append("**"));
        
        try {
            user.openPrivateChannel().queue(pc -> pc.sendMessage(sb.toString()).queue());
        } catch(Exception e) {}
        threadpool.schedule(() -> {
            removeDMSpammer(user);
        }, 10, TimeUnit.MINUTES);
    }
    
    private void removeDMSpammer(User user) {
        List<Guild> spammers = dmspam.getDMSpamServers(user);
        List<Guild> kicks = manager.getDMSpamPreventionGuilds(jda);
        if(kicks.isEmpty() || spammers.isEmpty())
            return;
        kicks = kicks.stream().filter((Guild g) -> {
            Member m = g.getMemberById(user.getId());
            if(m==null)
                return false;
            return shouldPerformAutomod(m, null);
        }).collect(Collectors.toList());
        if(kicks.isEmpty())
            return;
        kicks.forEach(g -> {
            try{
                g.getController().kick(user.getId(), "Being on a DM-spam server.").queue();
                modlog.logAutomod(g.getMemberById(user.getId()), Action.KICK, "Joining a DM-spam server: "+spammers.get(0).getName()+" ("+spammers.get(0).getId()+")");
            } catch(Exception e) {}
        });
    }
    
    @Override
    public void onGuildMemberJoin(GuildMemberJoinEvent event) {
        if(event.getMember().getUser().isBot())
            return;
        if(manager.isDMSpamPrevention(event.getGuild()) && event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS))
        {
            List<Guild> guilds = dmspam.getDMSpamServers(event.getMember().getUser());
            if(!guilds.isEmpty())
            {
                StringBuilder sb = new StringBuilder();
                sb.append("Sorry, you cannot join **").append(event.getGuild().getName())
                        .append("** because you are a member of the following server(s):\n");
                guilds.forEach((g) -> {
                    sb.append("\n  **").append(g.getName()).append("**");
                });
                sb.append("\n\nThe listed server(s) are known to be the origin of large amounts of direct-message spam "
                        + "and advertisements, and therefore all members are prevented from joining. Please leave those "
                        + "server(s) if you would like to join **").append(event.getGuild().getName()).append("**.");
                try{
                    event.getMember().getUser().openPrivateChannel().queue(pc -> pc.sendMessage(sb.toString()).queue(m -> {
                        try{event.getGuild().getController().kick(event.getMember(), "Being on a DM-spam server.").queue();}catch(PermissionException ex){}
                        modlog.logAutomod(event.getMember(), Action.KICK, "Being on a DM-spam server: "+guilds.get(0).getName()+" ("+guilds.get(0).getId()+")");
                    }, v -> {
                        try{event.getGuild().getController().kick(event.getMember(), "Being on a DM-spam server.").queue();}catch(PermissionException ex){}
                        modlog.logAutomod(event.getMember(), Action.KICK, "Being on a DM-spam server: "+guilds.get(0).getName()+" ("+guilds.get(0).getId()+")");
                    }), v -> {
                        try{event.getGuild().getController().kick(event.getMember(), "Being on a DM-spam server.").queue();}catch(PermissionException ex){}
                        modlog.logAutomod(event.getMember(), Action.KICK, "Being on a DM-spam server: "+guilds.get(0).getName()+" ("+guilds.get(0).getId()+")");
                    });
                } catch(PermissionException e) {
                    try{event.getGuild().getController().kick(event.getMember(), "Being on a DM-spam server.").queue();}catch(PermissionException ex){}
                    modlog.logAutomod(event.getMember(), Action.KICK, "Being on a DM-spam server: "+guilds.get(0).getName()+" ("+guilds.get(0).getId()+")");
                }
                return;
            }
        }
        if(raidmode.keySet().contains(event.getGuild().getId()))
        {
            if(event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS))
            {
                event.getMember().getUser().openPrivateChannel().queue(pc -> {
                    pc.sendMessage("Sorry, **"+event.getGuild().getName()+"** is currently under lockdown. Please try joining again later. Sorry for the inconvenience.").queue(s -> {
                        event.getGuild().getController().kick(event.getMember(), "Anti-Raid Mode").queue();
                    }, f -> {
                        event.getGuild().getController().kick(event.getMember(), "Anti-Raid Mode").queue();
                    });
                }, v -> {
                    event.getGuild().getController().kick(event.getMember(), "Anti-Raid Mode").queue();
                });
                raidmode.get(event.getGuild().getId()).append(" <@").append(event.getMember().getUser().getId()).append(">");
            }
        }
        if(manager.isAutoRaidMode(event.getGuild()))
        {
            RaidmodeStatus rs = raids.get(event.getGuild().getId());
            if(rs==null)
            {
                rs = new RaidmodeStatus();
                raids.put(event.getGuild().getId(), rs);
            }
            else
                rs.update();
            if(rs.count > 9)
            {
                startRaidMode(event.getGuild(), null);
            }
        }
    }
    
    public boolean startRaidMode(Guild guild, Message iniator)
    {
        if(raidmode.keySet().contains(guild.getId()))
            return false;
        raidmode.put(guild.getId(), new StringBuilder("Disabled Raid Mode. Users kicked:\n"));
        if(iniator==null)
        {
            scheduleRaidModeCheck(guild);
            modlog.logEmbed(guild, new EmbedBuilder()
                .setColor(guild.getSelfMember().getColor())
                .setDescription("Raid mode automatically enabled. Verification will be set to maximum if possible, and any user that joins will be kicked.")
                .setTimestamp(OffsetDateTime.now())
                .setFooter(guild.getJDA().getSelfUser().getName()+" automod", guild.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .build());
        }
        else
            modlog.logCommand(iniator);
        try{
            guild.getManager().setVerificationLevel(Guild.VerificationLevel.HIGH).queue();
        } catch(PermissionException ex){}
        return true;
    }
    
    public boolean endRaidMode(Guild guild)
    {
        StringBuilder sb = raidmode.remove(guild.getId());
        if(sb==null)
            return false;
        modlog.logMessage(guild, sb.toString());
        return true;
    }
    
    public boolean isRaidModeEnabled(Guild guild)
    {
        return raidmode.containsKey(guild.getId());
    }
    
    public void shutdownAllRaidMode(JDA jda)
    {
        raidmode.keySet().forEach(id -> {
            Guild guild = jda.getGuildById(id);
            if(guild!=null)
                endRaidMode(guild);
        });
    }
    
    public void scheduleRaidModeCheck(Guild guild)
    {
        threadpool.schedule(()->{
            if(!raidmode.keySet().contains(guild.getId()))
            {
                RaidmodeStatus rs = raids.get(guild.getId());
                if(rs!=null)
                {
                    if(rs.secondSinceLastJoin()>240)
                        endRaidMode(guild);
                    else
                        scheduleRaidModeCheck(guild);
                }
            }
        }, 2, TimeUnit.MINUTES);
    }
    
    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        performAutomod(event.getMessage());
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        performAutomod(event.getMessage());
    }
    
    private boolean shouldPerformAutomod(Member member, TextChannel channel)
    {
        // ignore users not in the guild
        if(member==null || member.getGuild()==null)
            return false;
        
        // ignore bots
        if(member.getUser().isBot())
            return false;
        
        // ignore users vortex cant interact with
        if(!PermissionUtil.canInteract(member.getGuild().getSelfMember(), member))
            return false;
        
        // ignore users that can kick
        if(member.hasPermission(Permission.KICK_MEMBERS))
            return false;
        
        // ignore users that can ban
        if(member.hasPermission(Permission.BAN_MEMBERS))
            return false;
        
        // ignore users that can manage server
        if(member.hasPermission(Permission.MANAGE_SERVER))
            return false;
        
        // if a channel is specified, ignore users that can manage messages in that channel
        if(channel!=null && member.hasPermission(channel, Permission.MESSAGE_MANAGE))
            return false;
        
        // ignore members with a role called 'vortexshield'
        if(member.getRoles().stream().anyMatch(r -> r.getName().toLowerCase().equals("vortexshield")))
            return false;
        
        if(manager.isIgnored(channel))
            return false;
        
        if(manager.isIgnored(member))
            return false;
        
        return true;
    }
    
    public void performAutomod(Message message) 
    {
        //simple automod
        
        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if(!shouldPerformAutomod(message.getMember(), message.getTextChannel()))
            return;
        
        //get the settings
        GuildSettings settings = manager.getSettings(message.getGuild());
        if(settings==null)
            return;
        
        /*
            check for automod actions
            * AntiSpam - prevent repeated messages
            * AntiMention - prevent mass-mention spammers
            * AntiInvite - prevent invite links to other servers
        */
        
        boolean shouldDelete = false;
        
        // anti-spam
        if(settings.spamAction!=Action.NONE && (message.getTextChannel().getTopic()==null || !message.getTextChannel().getTopic().toLowerCase().contains("{spam}")))
        {
            String key = message.getAuthor().getId()+"|"+message.getGuild().getId();
            SpamStatus status = spams.get(key);
            if(status==null)
            {
                spams.put(key, new SpamStatus(message));
            }
            else
            {
                int offenses = status.update(message);
                switch(offenses) {
                    case 0:
                    case 1:
                    case 2: 
                        break;
                    case 3:
                        shouldDelete = true;
                        break;
                    case 4:
                        shouldDelete = true;
                        message.getTextChannel().sendMessage(message.getMember().getAsMention()+": Please stop spamming. Your messages have been removed.").queue();
                        break;
                    default:
                        if(offenses >= settings.spamLimit)
                        {
                            RestAction ra = null;
                            switch(settings.spamAction) {
                                case BAN:
                                    ra = message.getGuild().getController().ban(message.getMember(), 1, "Spamming: "+message.getRawContent());
                                    break;
                                case KICK:
                                    shouldDelete = true;
                                    ra = message.getGuild().getController().kick(message.getMember(), "Spamming: "+message.getRawContent());
                                    break;
                                case MUTE:
                                    shouldDelete = true;
                                    Role mutedRole = ModLogger.getMutedRole(message.getGuild());
                                    if(mutedRole!=null)
                                        ra = message.getGuild().getController().addRolesToMember(message.getMember(), mutedRole);
                                    break;
                                case DELETE:
                                    shouldDelete = true;
                                    break;
                            }
                            if(ra!=null)
                                ra.queue(v -> modlog.logAutomod(message, settings.spamAction, "spamming: ```\n"+message.getRawContent()+" ```"));
                        }
                }
            }
        }
        
        // anti-mention
        long mentions = message.getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(message.getAuthor())).count();
        if(mentions >= settings.maxMentions && mentions >= MENTION_MINIMUM)
        {
            try{
                message.getGuild().getController().ban(message.getMember(), 1, "Mentioning "+mentions+" users.").queue(v -> {
                    modlog.logAutomod(message, Action.BAN, "mentioning **"+mentions+"** users.");
                    });
                message.getTextChannel().sendMessage(message.getAuthor().getAsMention()+" has been banned for mentioning "+mentions+" users");
            } catch(Exception e){}
        }
        
        // anti-invite
        if(settings.inviteAction!=Action.NONE && (message.getTextChannel().getTopic()==null || !message.getTextChannel().getTopic().toLowerCase().contains("{invites}")))
        {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(message.getRawContent());
            while(m.find())
                invites.add(m.group(1));
            LOG.trace("Found "+invites.size()+" invites.");
            try{
                for(String inviteCode : invites)
                {
                    Invite invite = null;
                    try {
                        invite = Invite.resolve(message.getJDA(), inviteCode).complete();
                    } catch(Exception e) {}
                    if(invite==null || !invite.getGuild().getId().equals(message.getGuild().getId()))
                    {
                        if(settings.inviteAction == Action.DELETE)
                            shouldDelete = true;
                        else
                        {
                            String key = message.getAuthor().getId()+"|"+message.getGuild().getId();
                            OffsetDateTime lastWarning = warnings.get(key);
                            if(lastWarning==null || lastWarning.isBefore(message.getCreationTime().minusMinutes(1)))
                            {
                                shouldDelete = true;
                                message.getTextChannel().sendMessage(message.getMember().getAsMention()+": Please do not post invite links here.").queue();
                                warnings.put(key, message.getCreationTime());
                            }
                            else 
                            {
                                RestAction ra = null;
                                switch(settings.inviteAction) {
                                    case BAN:
                                        ra = message.getGuild().getController().ban(message.getMember(), 1, "Posting invite link: "+inviteCode);
                                        break;
                                    case KICK:
                                        shouldDelete = true;
                                        ra = message.getGuild().getController().kick(message.getMember(), "Posting invite link: "+inviteCode);
                                        break;
                                    case MUTE:
                                        shouldDelete = true;
                                        Role mutedRole = ModLogger.getMutedRole(message.getGuild());
                                        if(mutedRole!=null)
                                            ra = message.getGuild().getController().addRolesToMember(message.getMember(), mutedRole);
                                        break;
                                    case WARN:
                                        shouldDelete = true;
                                        break;
                                }
                                if(ra!=null)
                                    ra.queue(v -> modlog.logAutomod(message, settings.inviteAction, "posting an invite link: ```\n"+inviteCode+" ```"));
                            }
                        }
                        break;
                    }
                }
            }catch(PermissionException ex){}
        }
        if(shouldDelete)
        {
            try{message.delete().queue();}catch(PermissionException e){}
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
        jda = event.getJDA();
        event.getJDA().getGuilds().forEach(g -> updateRoleSettings(g));
        LOG.info("Done loading!");
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            updateRoleSettings(event.getGuild());
    }
    
    public void updateRoleSettings(Guild guild)
    {
        guild.getSelfMember().getRoles().stream().forEach(r -> {
            if(r.getName().toLowerCase().startsWith("antimention"))
            {
                try{
                    int maxmentions = Integer.parseInt(r.getName().split(":",2)[1].trim());
                    if(maxmentions>MENTION_MINIMUM)
                    {
                        manager.setMaxMentions(guild, (short)maxmentions);
                        r.delete().queue();
                        //antimention.put(guild.getId(), maxmentions);
                    }
                }catch(NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException | PermissionException e){}
            }
            else if(r.getName().toLowerCase().startsWith("antiinvite"))
            {
                try{
                    String type = r.getName().split(":",2)[1].trim().toUpperCase();
                    Action act = type==null ? Action.NONE : Action.of(type);
                    if(act!=Action.NONE)
                    {
                        manager.setInviteAction(guild, Action.of(type));
                        r.delete().queue();
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
                        manager.setSpam(guild, Action.of(parts[0].trim()), (short)num);
                        r.delete().queue();
                    }
                }catch(Exception e){}
            }
        });
        /*TextChannel modchan = guild.getTextChannels()
                .stream().filter(tc -> ((tc.getName().startsWith("mod") && tc.getName().endsWith("log")) || tc.getName().contains("modlog")) 
                        && PermissionUtil.checkPermission(tc, guild.getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                        .findFirst().orElse(null);
        if(modchan!=null)
            manager.setModlogChannel(guild, modchan);*/
    }
    
    private class RaidmodeStatus {
        private int count;
        private OffsetDateTime last;
        private RaidmodeStatus()
        {
            count = 1;
            last = OffsetDateTime.now();
        }
        private void update()
        {
            OffsetDateTime now = OffsetDateTime.now();
            if(last.until(now, ChronoUnit.SECONDS) < 3)
                count++;
            else
                count = 1;
            last = now;
        }
        private long secondSinceLastJoin()
        {
            return last.until(OffsetDateTime.now(), ChronoUnit.SECONDS);
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
                        list[0].delete().queue();
                        list[1].delete().queue();
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
