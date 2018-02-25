/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.awt.Color;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.events.user.UserNameUpdateEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BasicLogger
{
    private final Vortex vortex;
    
    public BasicLogger(Vortex vortex)
    {
        this.vortex = vortex;
    }
    
    private void log(OffsetDateTime now, TextChannel tc, String emote, String message, MessageEmbed embed)
    {
        try
        {
            tc.sendMessage(new MessageBuilder()
                .append(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now, vortex.getDatabase().settings.getSettings(tc.getGuild()).getTimezone(), emote, message)))
                .setEmbed(embed)
                .build()).queue();
        }
        catch(PermissionException ex)
        {
            
        }
    }
    
    // Message Logs
    
    public void logMessageEdit(Message newMessage, Message oldMessage)
    {
        if(oldMessage==null)
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(newMessage.getGuild()).getMessageLogChannel(newMessage.getGuild());
        if(tc==null)
            return;
        EmbedBuilder edit = new EmbedBuilder()
                .setColor(Color.YELLOW)
                .appendDescription("**From:** ")
                .appendDescription(FormatUtil.formatMessage(oldMessage));
        String newm = FormatUtil.formatMessage(newMessage);
        if(edit.getDescriptionBuilder().length()+9+newm.length()>2048)
            edit.addField("To:", newm.length()>1024 ? newm.substring(0,1016)+" (...)" : newm, false);
        else
            edit.appendDescription("\n**To:** "+newm);
        log(newMessage.getEditedTime()==null ? newMessage.getCreationTime() : newMessage.getEditedTime(), tc, "\u26A0", 
                FormatUtil.formatUser(newMessage.getAuthor())+" edited a message in "+newMessage.getTextChannel().getAsMention()+":", edit.build());
    }
    
    public void logMessageDelete(Message oldMessage)
    {
        if(oldMessage==null)
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(oldMessage.getGuild()).getMessageLogChannel(oldMessage.getGuild());
        if(tc==null)
            return;
        String formatted = FormatUtil.formatMessage(oldMessage);
        if(formatted.isEmpty())
            return;
        EmbedBuilder delete = new EmbedBuilder()
                .setColor(Color.RED)
                .appendDescription(formatted);
        log(OffsetDateTime.now(), tc, "\u274C", 
                FormatUtil.formatUser(oldMessage.getAuthor())+"'s message has been deleted from "+oldMessage.getTextChannel().getAsMention()+":", delete.build());
    }
    
    public void logMessageBulkDelete(List<Message> messages, int count, TextChannel text)
    {
        if(count==0)
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(text.getGuild()).getMessageLogChannel(text.getGuild());
        if(tc==null)
            return;
        if(messages.isEmpty())
        {
            log(OffsetDateTime.now(), tc, "\uD83D\uDEAE", "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged)", null);
            return;
        }
        vortex.getTextUploader().upload(LogUtil.logMessages("Deleted Messages", messages), "DeletedMessages", (view, download) ->
        {
            log(OffsetDateTime.now(), tc, "\uD83D\uDEAE", "**"+count+"** messages were deleted from "+text.getAsMention()+" (**"+messages.size()+"** logged):", 
                new EmbedBuilder().setColor(Color.RED.darker().darker())
                .appendDescription("[`\uD83D\uDCC4 View`]("+view+")  |  [`\uD83D\uDCE9 Download`]("+download+")").build());
        });
    }
    
    
    // Server Logs
    
    public void logNameChange(UserNameUpdateEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getServerLogChannel(guild))
            .filter(tc -> tc!=null)
            .forEachOrdered(tc ->
            {
                log(now, tc, "\uD83D\uDCDB",
                "**"+event.getOldName()+"**#"+event.getOldDiscriminator()+" (ID:"+event.getUser().getId()+") has changed names to "+FormatUtil.formatUser(event.getUser()), null);
            });
    }
    
    public void logGuildJoin(GuildMemberJoinEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getUser().getCreationTime().until(now, ChronoUnit.SECONDS);
        log(now, tc, "\uD83D\uDCE5", FormatUtil.formatFullUser(event.getUser())+" joined the server. "
                +(seconds<16*60 ? "NEW" : "")
                +"\nCreation: "+event.getUser().getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)", null);
    }
    
    public void logGuildLeave(GuildMemberLeaveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getMember().getJoinDate().until(now, ChronoUnit.SECONDS);
        StringBuilder rlist;
        if(event.getMember().getRoles().isEmpty())
            rlist = new StringBuilder();
        else
        {
            rlist= new StringBuilder("\nRoles: `"+event.getMember().getRoles().get(0).getName());
            for(int i=1; i<event.getMember().getRoles().size(); i++)
                rlist.append("`, `").append(event.getMember().getRoles().get(i));
            rlist.append("`");
        }
        log(now, tc, "\uD83D\uDCE4", FormatUtil.formatFullUser(event.getUser())+" left or was kicked from the server. "
                +"\nJoined: "+event.getUser().getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+" ("+FormatUtil.secondsToTimeCompact(seconds)+" ago)"
                +rlist.toString(), null);
    }
    
    public void logVoiceJoin(GuildVoiceJoinEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voicejoin:314044543605407757>", 
                FormatUtil.formatFullUser(event.getMember().getUser())+" has joined voice channel _"+event.getChannelJoined().getName()+"_", null);
    }
    
    public void logVoiceMove(GuildVoiceMoveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voicechange:314043907992190987>", 
                FormatUtil.formatFullUser(event.getMember().getUser())+" has moved voice channels from _"+event.getChannelLeft().getName()+"_ to _"+event.getChannelJoined()+"_", null);
    }
    
    public void logVoiceLeave(GuildVoiceLeaveEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getServerLogChannel(event.getGuild());
        if(tc==null)
            return;
        log(OffsetDateTime.now(), tc, "<:voiceleave:314044543609864193>", 
                FormatUtil.formatFullUser(event.getMember().getUser())+" has left voice channel _"+event.getChannelLeft().getName()+"_", null);
    }
}
