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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.JDA.ShardInfo;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.events.channel.update.ChannelUpdateSlowmodeEvent;
import net.dv8tion.jda.api.events.guild.GuildBanEvent;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.events.guild.GuildUnbanEvent;
import net.dv8tion.jda.api.events.guild.member.*;
import net.dv8tion.jda.api.events.guild.member.update.GuildMemberUpdateNicknameEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.message.MessageBulkDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.jetbrains.annotations.NotNull;
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
    
    public Listener(Vortex vortex)
    {
        this.vortex = vortex;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event)
    {
        if (event instanceof MessageReceivedEvent)
        {
            Message m = ((MessageReceivedEvent) event).getMessage();
            
            if(!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot messages
            {
                // Store the message
                vortex.getMessageCache().putMessage(m);
                
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
            }
        }
        else if (event instanceof MessageUpdateEvent)
        {
            Message m = ((MessageUpdateEvent) event).getMessage();
            
            if(!m.getAuthor().isBot() && m.isFromGuild()) // ignore bot edits
            {
                // Run automod on the message
                vortex.getAutoMod().performAutomod(m);
                
                // Store and log the edit
                CachedMessage old = vortex.getMessageCache().putMessage(m);
                vortex.getBasicLogger().logMessageEdit(m, old);
            }
        }
        else if (event instanceof MessageDeleteEvent)
        {
            MessageDeleteEvent mevent = (MessageDeleteEvent) event;

            if (mevent.isFromGuild()) {
                // Log the deletion
                CachedMessage old = vortex.getMessageCache().pullMessage(mevent.getGuild(), mevent.getMessageIdLong());
                vortex.getModLogger().setNeedUpdate(mevent.getGuild());
                vortex.getBasicLogger().logMessageDelete(old);
            }
        }
        else if (event instanceof MessageBulkDeleteEvent)
        {
            MessageBulkDeleteEvent gevent = (MessageBulkDeleteEvent) event;

            // Get the messages we had cached
            List<CachedMessage> logged = gevent.getMessageIds().stream()
                    .map(id -> vortex.getMessageCache().pullMessage(gevent.getGuild(), Long.parseLong(id)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            // Log the deletion
            vortex.getBasicLogger().logMessageBulkDelete(logged, gevent.getMessageIds().size(), gevent.getChannel().asTextChannel());
        }
        else if (event instanceof GuildMemberJoinEvent)
        {
            OffsetDateTime now = OffsetDateTime.now();
            GuildMemberJoinEvent gevent = (GuildMemberJoinEvent) event;
            
            // Log the join
            vortex.getBasicLogger().logGuildJoin(gevent, now);
            
            // Perform automod on the newly-joined member
            vortex.getAutoMod().memberJoin(gevent);
        }
        else if (event instanceof GuildMemberRemoveEvent)
        {
            GuildMemberRemoveEvent gmre = (GuildMemberRemoveEvent)event;
            
            // Log the member leaving
            vortex.getBasicLogger().logGuildLeave(gmre);
            
            // Signal the modlogger because someone might have been kicked
            vortex.getModLogger().setNeedUpdate(gmre.getGuild());
        }
        else if (event instanceof GuildBanEvent)
        {
            // Signal the modlogger because someone was banned
            GuildBanEvent gbe = (GuildBanEvent) event;
            vortex.getModLogger().setNeedUpdate(gbe.getGuild());
        }
        else if (event instanceof GuildUnbanEvent)
        {
            GuildUnbanEvent gue = (GuildUnbanEvent) event;
            // Signal the modlogger because someone was unbanned
            vortex.getModLogger().setNeedUpdate((gue).getGuild());
        }
        else if (event instanceof GuildMemberRoleAddEvent)
        {
            GuildMemberRoleAddEvent gmrae = (GuildMemberRoleAddEvent) event;
            vortex.getModLogger().setNeedUpdate(gmrae.getGuild());
        }
        else if (event instanceof GuildMemberRoleRemoveEvent)
        {
            GuildMemberRoleRemoveEvent gmrre = (GuildMemberRoleRemoveEvent) event;
            vortex.getModLogger().setNeedUpdate(gmrre.getGuild());
        }
        else if (event instanceof UserUpdateNameEvent)
        {
            UserUpdateNameEvent unue = (UserUpdateNameEvent)event;
            // Log the name change
            vortex.getBasicLogger().logNameChange(unue);
            unue.getUser().getMutualGuilds().stream().map(g -> g.getMember(unue.getUser())).forEach(m -> vortex.getAutoMod().dehoist(m));
        }
        else if (event instanceof UserUpdateDiscriminatorEvent)
        {
            vortex.getBasicLogger().logNameChange((UserUpdateDiscriminatorEvent)event);
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
        else if (event instanceof GuildVoiceUpdateEvent) {
            GuildVoiceUpdateEvent gevent = (GuildVoiceUpdateEvent) event;

            if(!gevent.getMember().getUser().isBot()) // ignore bots
                vortex.getBasicLogger().logVoiceUpdate(gevent);
        }
        else if (event instanceof ChannelUpdateSlowmodeEvent)
        {
            // TODO: Check if this logic is correct, no funky thread things etc.
            vortex.getDatabase().tempslowmodes.clearSlowmode(((ChannelUpdateSlowmodeEvent) event).getChannel().asTextChannel());
        }
        else if (event instanceof ReadyEvent)
        {
            // Log the shard that has finished loading
            ShardInfo si = event.getJDA().getShardInfo();
            String shardinfo = si==null ? "N/A" : (si.getShardId()+1)+"/"+si.getShardTotal();
            LOG.info("Shard "+shardinfo+" is ready.");

            // TODO: Make sure gravels and mutes are checked from before the bot is on
            vortex.getLogWebhook().send("\uD83C\uDF00 Shard `"+shardinfo+"` has connected. Guilds: `" // 🌀
                    +event.getJDA().getGuildCache().size()+"` Users: `"+event.getJDA().getUserCache().size()+"`");
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempbans.checkUnbans(vortex, event.getJDA()), 0, 2, TimeUnit.MINUTES);
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempmutes.checkUnmutes(event.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().gravels.checkGravels(event.getJDA(), vortex.getDatabase().settings), 0, 45, TimeUnit.SECONDS);
            vortex.getThreadpool().scheduleWithFixedDelay(() -> vortex.getDatabase().tempslowmodes.checkSlowmode(event.getJDA()), 0, 45, TimeUnit.SECONDS);

        }
        else if (event instanceof GuildJoinEvent)
        {
            vortex.getModLogger().addNewGuild(((GuildJoinEvent) event).getGuild());
        }
    }
}
