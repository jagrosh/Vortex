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
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.core.utils.SimpleLog;
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
    
    private final ModLogger modlog;
    private final ScheduledExecutorService threadpool;
    private final DatabaseManager manager;
        
    public AutoMod(ModLogger modlog, ScheduledExecutorService threadpool, DatabaseManager manager)
    {
        this.modlog = modlog;
        this.threadpool = threadpool;
        this.manager = manager;
    }
    
    public String getSettings(Guild guild)
    {
        GuildSettings gs = manager.getSettings(guild);
        TextChannel channel = guild.getTextChannelById(gs.modlogChannelId);
        return "AntiMention: "+(gs.maxMentions>MENTION_MINIMUM ? "**"+gs.maxMentions+"**" : "not enabled")
                +"\nAntiInvite: "+(gs.inviteAction!=Action.NONE ? "**"+gs.inviteAction.name()+"**" : "not enabled")
                +"\nAntiSpam: "+(gs.spamLimit>0 && gs.spamAction!=Action.NONE ? "**"+gs.spamAction.name()+" on "+gs.spamLimit+"**" : "not enabled")
                +"\nModlog Channel: "+(channel==null ? "not enabled" : channel.getAsMention())
                ;
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        threadpool.shutdown();
        manager.shutdown();
    }
    
    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        performAutomod(event);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        performAutomod(event);
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
    
    public void performAutomod(GenericGuildMessageEvent event) 
    {
        //simple automod
        
        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if(!shouldPerformAutomod(event.getMember(), event.getChannel()))
            return;
        
        //get the settings
        GuildSettings settings = manager.getSettings(event.getGuild());
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
        if(settings.spamAction!=Action.NONE && (event.getChannel().getTopic()==null || !event.getChannel().getTopic().toLowerCase().contains("{spam}")))
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
                    case 0:
                    case 1:
                    case 2: 
                        break;
                    case 3:
                        shouldDelete = true;
                        break;
                    case 4:
                        shouldDelete = true;
                        event.getChannel().sendMessage(event.getMember().getAsMention()+": Please stop spamming. Your messages have been removed.").queue();
                        break;
                    default:
                        if(offenses >= settings.spamLimit)
                        {
                            RestAction ra = null;
                            switch(settings.spamAction) {
                                case BAN:
                                    ra = event.getGuild().getController().ban(event.getMember(), 1);
                                    break;
                                case KICK:
                                    shouldDelete = true;
                                    ra = event.getGuild().getController().kick(event.getMember());
                                    break;
                                case MUTE:
                                    shouldDelete = true;
                                    Role mutedRole = ModLogger.getMutedRole(event.getGuild());
                                    if(mutedRole!=null)
                                        ra = event.getGuild().getController().addRolesToMember(event.getMember(), mutedRole);
                                    break;
                                case DELETE:
                                    shouldDelete = true;
                                    break;
                            }
                            if(ra!=null)
                                ra.queue(v -> modlog.logAutomod(event.getMessage(), settings.spamAction, "spamming: ```\n"+event.getMessage().getRawContent()+" ```"));
                        }
                }
            }
        }
        
        // anti-mention
        long mentions = event.getMessage().getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(event.getAuthor())).count();
        if(mentions >= settings.maxMentions && mentions >= MENTION_MINIMUM)
        {
            try{
                event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                    modlog.logAutomod(event.getMessage(), Action.BAN, "mentioning **"+mentions+"** users.");
                    });
                event.getChannel().sendMessage(event.getAuthor().getAsMention()+" has been banned for mentioning "+mentions+" users");
            } catch(Exception e){}
        }
        
        // anti-invite
        if(settings.inviteAction!=Action.NONE && (event.getChannel().getTopic()==null || !event.getChannel().getTopic().toLowerCase().contains("{invites}")))
        {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(event.getMessage().getRawContent());
            while(m.find())
                invites.add(m.group(1));
            LOG.trace("Found "+invites.size()+" invites.");
            try{
                for(String inviteCode : invites)
                {
                    Invite invite = null;
                    try {
                        invite = Invite.resolve(event.getJDA(), inviteCode).complete();
                    } catch(Exception e) {}
                    if(invite==null || !invite.getGuild().getId().equals(event.getGuild().getId()))
                    {
                        if(settings.inviteAction == Action.DELETE)
                            shouldDelete = true;
                        else
                        {
                            String key = event.getAuthor().getId()+"|"+event.getGuild().getId();
                            OffsetDateTime lastWarning = warnings.get(key);
                            if(lastWarning==null || lastWarning.isBefore(event.getMessage().getCreationTime().minusMinutes(1)))
                            {
                                event.getChannel().sendMessage(event.getMember().getAsMention()+": Please do not post invite links here.").queue();
                                warnings.put(key, event.getMessage().getCreationTime());
                            }
                            else 
                            {
                                RestAction ra = null;
                                switch(settings.inviteAction) {
                                    case BAN:
                                        ra = event.getGuild().getController().ban(event.getMember(), 1);
                                        break;
                                    case KICK:
                                        shouldDelete = true;
                                        ra = event.getGuild().getController().kick(event.getMember());
                                        break;
                                    case MUTE:
                                        shouldDelete = true;
                                        Role mutedRole = ModLogger.getMutedRole(event.getGuild());
                                        if(mutedRole!=null)
                                            ra = event.getGuild().getController().addRolesToMember(event.getMember(), mutedRole);
                                        break;
                                }
                                if(ra!=null)
                                    ra.queue(v -> modlog.logAutomod(event.getMessage(), settings.inviteAction, "posting an invite link: ```\nhttp://discordapp.com/invite/"+inviteCode+" ```"));
                            }
                        }
                        break;
                    }
                }
            }catch(PermissionException ex){}
        }
        if(shouldDelete)
        {
            try{event.getMessage().delete().queue();}catch(PermissionException e){}
        }
    }

    @Override
    public void onReady(ReadyEvent event) {
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
