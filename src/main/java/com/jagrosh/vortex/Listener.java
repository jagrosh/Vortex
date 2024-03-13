/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex;

import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.channel.text.update.TextChannelUpdateSlowmodeEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateTimeOutEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageDeleteEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Listener implements EventListener
{
    private final static Logger LOG = LoggerFactory.getLogger("Listener");
    private final Vortex vortex;
    private final List<String> pruneTerms;
    
    public Listener(Vortex vortex)
    {
        this.vortex = vortex;
        this.pruneTerms = vortex.getConfig().getStringList("prune-terms");
    }

    @Override
    public void onEvent(GenericEvent event)
    {
        if (event instanceof GuildMessageReceivedEvent)
        {
            Message m = ((GuildMessageReceivedEvent)event).getMessage();
            
            if(!m.getAuthor().isBot()) // ignore bot messages
            {
                // Store the message
                vortex.getMessageCache().putMessage(m);
                
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
            }
        }
        else if (event instanceof GuildMessageUpdateEvent)
        {
            Message m = ((GuildMessageUpdateEvent)event).getMessage();
            
            if(!m.getAuthor().isBot()) // ignore bot edits
            {
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
                
                // Store and log the edit
                CachedMessage old = vortex.getMessageCache().putMessage(m);
                vortex.getBasicLogger().logMessageEdit(m, old);
            }
        }
        else if (event instanceof GuildMessageDeleteEvent)
        {
            GuildMessageDeleteEvent gevent = (GuildMessageDeleteEvent) event;
            
            // Log the deletion
            CachedMessage old = vortex.getMessageCache().pullMessage(gevent.getGuild(), gevent.getMessageIdLong());
            vortex.getBasicLogger().logMessageDelete(old);
        }
        else if (event instanceof MessageBulkDeleteEvent)
        {
            MessageBulkDeleteEvent gevent = (MessageBulkDeleteEvent) event;
            
            // Get the messages we had cached
            List<CachedMessage> logged = gevent.getMessageIds().stream()
                    .map(id -> vortex.getMessageCache().pullMessage(gevent.getGuild(), Long.parseLong(id)))
                    .filter(m -> m!=null)
                    .collect(Collectors.toList());
            
            // Log the deletion
            vortex.getBasicLogger().logMessageBulkDelete(logged, gevent.getMessageIds().size(), gevent.getChannel());
        }
        else if (event instanceof GuildMemberJoinEvent)
        {
            GuildMemberJoinEvent gevent = (GuildMemberJoinEvent) event;
            
            // Log the join
            vortex.getBasicLogger().logGuildJoin(gevent);
            
            // Perform automod on the newly-joined member
            vortex.getAutoMod().memberJoin(gevent);
        }
        else if (event instanceof GuildMemberRemoveEvent)
        {
            GuildMemberRemoveEvent gmle = (GuildMemberRemoveEvent)event;
            
            // Log the member leaving
            vortex.getBasicLogger().logGuildLeave(gmle);
            
            // Signal the modlogger because someone might have been kicked
            vortex.getModLogger().setNeedUpdate(gmle.getGuild());
        }
        else if (event instanceof GuildBanEvent)
        {
            // Signal the modlogger because someone was banned
            vortex.getModLogger().setNeedUpdate(((GuildBanEvent) event).getGuild());
        }
        else if (event instanceof GuildUnbanEvent)
        {
            GuildUnbanEvent gue = (GuildUnbanEvent) event;
            // Signal the modlogger because someone was unbanned
            vortex.getDatabase().tempbans.clearBan(gue.getGuild(), gue.getUser().getIdLong());
            vortex.getModLogger().setNeedUpdate((gue).getGuild());
        }
        else if (event instanceof GuildMemberRoleAddEvent)
        {
            GuildMemberRoleAddEvent gmrae = (GuildMemberRoleAddEvent) event;
            
            // Signal the modlogger if someone was muted
            Role mRole = vortex.getDatabase().settings.getSettings(gmrae.getGuild()).getMutedRole(gmrae.getGuild());
            if(mRole != null && gmrae.getRoles().contains(mRole))
                vortex.getModLogger().setNeedUpdate(gmrae.getGuild());
        }
        else if (event instanceof GuildMemberRoleRemoveEvent)
        {
            GuildMemberRoleRemoveEvent gmrre = (GuildMemberRoleRemoveEvent) event;
            
            // Signal the modlogger if someone was unmuted
            Role mRole = vortex.getDatabase().settings.getSettings(gmrre.getGuild()).getMutedRole(gmrre.getGuild());
            if(mRole != null && gmrre.getRoles().contains(mRole))
            {
                vortex.getDatabase().tempmutes.removeMute(gmrre.getGuild(), gmrre.getUser().getIdLong());
                vortex.getModLogger().setNeedUpdate(gmrre.getGuild());
            }
        }
        else if (event instanceof UserUpdateNameEvent)
        {
            // Make sure the name actually changed
            UserUpdateNameEvent unue = (UserUpdateNameEvent)event;
            if(!unue.getNewName().equals(unue.getOldName()))
            {
                // Log the name change
                vortex.getBasicLogger().logNameChange(unue);
                unue.getUser().getMutualGuilds().stream().map(g -> g.getMember(unue.getUser())).forEach(m -> vortex.getAutoMod().dehoist(m));
            }
        }
        else if (event instanceof GuildMemberUpdateNicknameEvent)
        {
            vortex.getAutoMod().dehoist(((GuildMemberUpdateNicknameEvent) event).getMember());
        }
        else if (event instanceof UserUpdateAvatarEvent)
        {
            UserUpdateAvatarEvent uaue = (UserUpdateAvatarEvent)event;
            
            // Log the avatar change
            if(!uaue.getUser().isBot())
                vortex.getBasicLogger().logAvatarChange(uaue);
        }
        else if (event instanceof GuildVoiceJoinEvent)
        {
            GuildVoiceJoinEvent gevent = (GuildVoiceJoinEvent)event;
            
            // Log the voice join
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceJoin(gevent);
        }
        else if (event instanceof GuildVoiceMoveEvent)
        {
            GuildVoiceMoveEvent gevent = (GuildVoiceMoveEvent)event;
            
            // Log the voice move
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceMove(gevent);
        }
        else if (event instanceof GuildVoiceLeaveEvent)
        {
            GuildVoiceLeaveEvent gevent = (GuildVoiceLeaveEvent)event;
            
            // Log the voice leave
            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceLeave(gevent);
        }
        else if (event instanceof TextChannelUpdateSlowmodeEvent)
        {
            vortex.getDatabase().tempslowmodes.clearSlowmode(((TextChannelUpdateSlowmodeEvent) event).getChannel());
        }
        else if (event instanceof GuildMemberUpdateTimeOutEvent)
        {
            // Signal the modlogger because someone was timed out
            vortex.getModLogger().setNeedUpdate(((GuildMemberUpdateTimeOutEvent) event).getGuild());
        }
        else if (event instanceof GuildJoinEvent)
        {
            Guild guild = ((GuildJoinEvent)event).getGuild();
            String lower = guild.getName().toLowerCase();
            for(String term: pruneTerms)
                if(lower.contains(term))
                {
                    LOG.info("Leaving " + guild + " due to filtered term '" + term + "'");
                    guild.leave().queue();
                    break;
                }
        }
        else if (event instanceof ReadyEvent)
        {
            // Log the shard that has finished loading
            ShardInfo si = event.getJDA().getShardInfo();
            String shardinfo = (si.getShardId()+1)+"/"+si.getShardTotal();
            LOG.info("Shard "+shardinfo+" is ready.");
            vortex.getLogWebhook().send("\uD83C\uDF00 Shard `"+shardinfo+"` has connected. Guilds: `" // 🌀
                    +event.getJDA().getGuildCache().size()+"` Users: `"+event.getJDA().getUserCache().size()+"`");
        }
    }
}
