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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.AutomodManager;
import com.jagrosh.vortex.database.managers.AutomodManager.AutomodSettings;
import com.jagrosh.vortex.utils.FixedCache;
import com.jagrosh.vortex.utils.OtherUtil;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.VerificationLevel;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutoMod
{
    private final static Pattern INVITES = Pattern.compile("discord\\s?(?:\\.\\s?gg|app\\s?.\\s?com\\s?\\/\\s?invite)\\s?\\/\\s?([A-Z0-9-]{2,18})",Pattern.CASE_INSENSITIVE);
    private final static Pattern REF     = Pattern.compile("");
    private final static String CONDENSER = "(.+?)\\s*(\\1\\s*)+";
    private final static Logger LOG = LoggerFactory.getLogger("AutoMod");
    
    private final Vortex vortex;
    
    private final FixedCache<String,DupeStatus> spams = new FixedCache<>(3000);
    private final HashMap<Long,OffsetDateTime> latestGuildJoin = new HashMap<>();
    
    public AutoMod(Vortex vortex)
    {
        this.vortex = vortex;
    }
    
    public void enableRaidMode(Guild guild, Member moderator, OffsetDateTime now, String reason)
    {
        vortex.getDatabase().settings.enableRaidMode(guild);
        if(guild.getVerificationLevel().getKey()<VerificationLevel.HIGH.getKey())
            try
            {
                guild.getManager().setVerificationLevel(VerificationLevel.HIGH).reason("Enabling Anti-Raid Mode").queue();
            } catch(PermissionException ex) {}
        vortex.getModLogger().postRaidmodeCase(moderator, now, true, reason);
    }
    
    public void disableRaidMode(Guild guild, Member moderator, OffsetDateTime now, String reason)
    {
        VerificationLevel last = vortex.getDatabase().settings.disableRaidMode(guild);
        if(guild.getVerificationLevel()!=last)
            try
            {
                guild.getManager().setVerificationLevel(last).reason("Disabling Anti-Raid Mode").queue();
            } catch(PermissionException ex) {}
        vortex.getModLogger().postRaidmodeCase(moderator, now, false, reason);
    }
    
    public void memberJoin(GuildMemberJoinEvent event)
    {
        // completely ignore bots for raidmode
        if(event.getMember().getUser().isBot())
            return;
        
        boolean inRaidMode = vortex.getDatabase().settings.getSettings(event.getGuild()).isInRaidMode();
        AutomodSettings ams = vortex.getDatabase().automod.getSettings(event.getGuild());
        OffsetDateTime now = event.getMember().getJoinDate();
        boolean kicking = false;
        
        // if we're in raid mode...
        if(inRaidMode)
        {
            // ...and this server uses auto raid mode, check if we should be turning it off automatically
            // this means that we should turn it off if the latest attempted join was more than 2 minutes ago
            if(ams.useAutoRaidMode() 
                    && latestGuildJoin.containsKey(event.getGuild().getIdLong()) 
                    && latestGuildJoin.get(event.getGuild().getIdLong()).until(now, ChronoUnit.SECONDS)>120)
            {
                disableRaidMode(event.getGuild(), event.getGuild().getSelfMember(), now, "No recent join attempts");
            }
            // otherwise, boot 'em
            else if(event.getGuild().getSelfMember().hasPermission(Permission.KICK_MEMBERS))
            {
                kicking = true;
            }
        }
        
        // now, if we're not in raid mode, and auto mode is enabled
        else if(ams.useAutoRaidMode())
        {
            // find the time that we should be looking after, and count the number of people that joined after that
            OffsetDateTime min = event.getMember().getJoinDate().minusSeconds(ams.raidmodeTime);
            long recent = event.getGuild().getMemberCache().stream().filter(m -> !m.getUser().isBot() && m.getJoinDate().isAfter(min)).count();
            if(recent>=ams.raidmodeNumber)
            {
                enableRaidMode(event.getGuild(), event.getGuild().getSelfMember(), now, "Maximum join rate exceeded ("+ams.raidmodeNumber+"/"+ams.raidmodeTime+"s)");
                kicking = true;
            }
        }
        
        if(kicking)
        {
            OtherUtil.safeDM(event.getUser(),
                    "Sorry, **"+event.getGuild().getName()+"** is currently under lockdown. Please try joining again later. Sorry for the inconvenience.", () -> 
                    {
                        try
                        {
                            event.getGuild().getController().kick(event.getMember(), "Anti-Raid Mode").queue();
                        }catch(Exception ex){}
                    });
        }
        else
        {
            if(vortex.getDatabase().tempmutes.isMuted(event.getMember()))
            {
                try
                {
                    event.getGuild().getController().addSingleRoleToMember(event.getMember(), OtherUtil.getMutedRole(event.getGuild())).reason("Restoring Muted Role").queue();
                } catch(Exception ex){}
            }
        }
        
        latestGuildJoin.put(event.getGuild().getIdLong(), now);
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
        if(!member.getGuild().getSelfMember().canInteract(member))
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
        
        if(vortex.getDatabase().ignores.isIgnored(channel))
            return false;
        
        if(vortex.getDatabase().ignores.isIgnored(member))
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
        AutomodSettings settings = vortex.getDatabase().automod.getSettings(message.getGuild());
        if(settings==null)
            return;
        
        /*
            check for automod actions
            * AntiDuplicate - prevent repeated messages
            * AntiMention - prevent mass-mention spammers
            * AntiInvite - prevent invite links to other servers
        */
        
        boolean shouldDelete = false;
        
        // anti-duplicate
        if(settings.useAntiDuplicate() && (message.getTextChannel().getTopic()==null || !message.getTextChannel().getTopic().toLowerCase().contains("{spam}")))
        {
            String key = message.getAuthor().getId()+"|"+message.getGuild().getId();
            String content = condensedContent(message);
            DupeStatus status = spams.get(key);
            if(status==null)
            {
                spams.put(key, new DupeStatus(content, latestTime(message)));
            }
            else
            {
                OffsetDateTime now = latestTime(message);
                int offenses = status.update(content, now);
                
                if(offenses==settings.dupeDeleteThresh)
                    purgeMessages(message.getGuild(), m -> m.getAuthor().getIdLong()==message.getAuthor().getIdLong() && m.getCreationTime().plusMinutes(2).isAfter(now));
                else if(offenses>settings.dupeDeleteThresh)
                    shouldDelete = true;
                
                if(offenses >= settings.dupeStrikeThresh)
                    vortex.getStrikeHandler().applyStrikes(message.getGuild().getSelfMember(), 
                            latestTime(message), message.getAuthor(), settings.dupeStrikes, "Duplicate messages");
            }
        }
        
        // anti-mention (users)
        if(settings.maxMentions>=AutomodManager.MENTION_MINIMUM)
        {
            long mentions = message.getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(message.getAuthor())).count();
            if(mentions > settings.maxMentions)
            {
                vortex.getStrikeHandler().applyStrikes(message.getGuild().getSelfMember(), latestTime(message), 
                        message.getAuthor(), (int)(mentions-settings.maxMentions), "Mentioning "+mentions+" users");
                shouldDelete = true;
            }
        }
        
        // anti-mention (roles)
        if(settings.maxRoleMentions >= AutomodManager.ROLE_MENTION_MINIMUM)
        {
            long mentions = message.getMentionedRoles().size();
            if(mentions > settings.maxRoleMentions)
            {
                vortex.getStrikeHandler().applyStrikes(message.getGuild().getSelfMember(), latestTime(message), 
                        message.getAuthor(), (int)(mentions-settings.maxRoleMentions), "Mentioning "+mentions+" roles");
                shouldDelete = true;
            }
        }
        
        // anti-invite
        if(settings.inviteStrikes > 0 && (message.getTextChannel().getTopic()==null || !message.getTextChannel().getTopic().toLowerCase().contains("{invites}")))
        {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(message.getContentRaw());
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
                        vortex.getStrikeHandler().applyStrikes(message.getGuild().getSelfMember(), latestTime(message), 
                                message.getAuthor(), settings.inviteStrikes, "Advertising");
                        shouldDelete = true;
                        break;
                    }
                }
            }catch(PermissionException ex){}
        }
        
        if(shouldDelete)
        {
            try
            {
                message.delete().reason("Automod").queue(v->{}, f->{});
            }catch(PermissionException e){}
        }
    }
    
    private void purgeMessages(Guild guild, Predicate<Message> predicate)
    {
        vortex.getMessageCache().getMessages(guild, predicate).forEach(m -> 
        {
            try
            {
                m.delete().queue();
            }
            catch(PermissionException ex) {}
        });
    }
    
    private static OffsetDateTime latestTime(Message m)
    {
        return m.isEdited() ? m.getEditedTime() : m.getCreationTime();
    }
    
    private static String condensedContent(Message m)
    {
        StringBuilder sb = new StringBuilder(m.getContentRaw());
        m.getAttachments().forEach(at -> sb.append("\n").append(at.getFileName()));
        return sb.toString().trim().replaceAll(CONDENSER, "$1");
    }
    
    private class DupeStatus
    {
        private String content;
        private OffsetDateTime time;
        private int count;
        
        private DupeStatus(String content, OffsetDateTime time)
        {
            this.content = content;
            this.time = time;
            count = 0;
        }
        
        private int update(String nextcontent, OffsetDateTime nexttime)
        {
            if(nextcontent.equals(content) && this.time.plusSeconds(30).isAfter(nexttime))
            {
                count++;
                this.time = nexttime;
                return count;
            }
            else
            {
                this.content = nextcontent;
                this.time = nexttime;
                count = 0;
                return count;
            }
        }
    }
}
