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
package com.jagrosh.vortex.commands.moderation;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import com.jagrosh.vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class VoicemoveCmd extends ModCommand
{
    public VoicemoveCmd(Vortex vortex)
    {
        super(vortex, Permission.VOICE_MOVE_OTHERS);
        this.name = "voicemove";
        this.aliases = new String[]{"magnet"};
        this.help = "mass-moves voice channel users";
        this.arguments = "[channel]";
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getGuild().getSelfMember().getVoiceState().inVoiceChannel())
        {
            event.replyWarning("I am already in a voice channel; move me to drag all users.");
            return;
        }
        if(event.getArgs().isEmpty() && !event.getMember().getVoiceState().inVoiceChannel())
        {
            event.replyError("You must be in or specify a voice channel to move users from!");
            return;
        }
        VoiceChannel vc;
        if(!event.getArgs().isEmpty())
        {
            List<VoiceChannel> list = FinderUtil.findVoiceChannels(event.getArgs(), event.getGuild());
            if(list.isEmpty())
            {
                event.replyError("No voice channel found matching `"+event.getArgs()+"`!");
                return;
            }
            if(list.size()>1)
            {
                event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfVoice(list, event.getArgs())));
                return;
            }
            vc = list.get(0);
        }
        else
        {
            vc = event.getMember().getVoiceState().getChannel();
        }
        if(!event.getMember().hasPermission(Permission.VOICE_MOVE_OTHERS))
        {
            event.replyError("You don't have permission to move users out of **"+vc.getName()+"**!");
            return;
        }
        if(!event.getSelfMember().hasPermission(Permission.VOICE_MOVE_OTHERS))
        {
            event.replyError("I don't have permission to move users out of **"+vc.getName()+"**!");
            return;
        }
        try 
        {
            event.getGuild().getAudioManager().openAudioConnection(vc);
        }
        catch(Exception e) 
        {
            event.replyWarning(FormatUtil.filterEveryone("I could not connect to **"+vc.getName()+"**"));
            return;
        }
        vortex.getEventWaiter().waitForEvent(GuildVoiceMoveEvent.class,
                (GuildVoiceMoveEvent e) -> 
                    e.getGuild().equals(event.getGuild()) && e.getMember().equals(event.getGuild().getSelfMember()), 
                (GuildVoiceMoveEvent e) -> {
                    event.getGuild().getAudioManager().closeAudioConnection();
                    e.getChannelLeft().getMembers().stream().forEach(m -> event.getGuild().getController().moveVoiceMember(m, e.getChannelJoined()).queue());
                }, 1, TimeUnit.MINUTES, () -> {
                    event.getGuild().getAudioManager().closeAudioConnection();
                    event.replyWarning("You waited too long, "+event.getMember().getAsMention());
                });
        event.reply("\uD83C\uDF9B Now, move me and I'll drag users to a new voice channel."); // 🎛
    }
    
}
