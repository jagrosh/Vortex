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
package vortex.commands;

import java.util.List;
import me.jagrosh.jdacommands.Command;
import me.jagrosh.jdacommands.CommandEvent;
import me.jagrosh.jdacommands.utils.EventWaiter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceMoveEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.utils.FinderUtil;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class MagnetCmd extends Command {

    private final EventWaiter waiter;
    public MagnetCmd(EventWaiter waiter)
    {
        this.waiter = waiter;
        this.name = "magnet";
        this.help = "mass-moves voice channel users";
        this.arguments = "[channel to connect to] (or just be in a channel)";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.VOICE_MOVE_OTHERS};
    }
    
    @Override
    protected void execute(CommandEvent event) {
        if(event.getGuild().getSelfMember().getVoiceState().inVoiceChannel())
        {
            event.reply(event.getClient().getWarning()+" I am already in a voice channel; move me to drag all users.");
            return;
        }
        if(event.getArgs().isEmpty() && !event.getMember().getVoiceState().inVoiceChannel())
        {
            event.reply(event.getClient().getError()+" You must specify a voice channel or be in a voice channel to connect");
            return;
        }
        VoiceChannel vc;
        if(!event.getArgs().isEmpty())
        {
            List<VoiceChannel> list = FinderUtil.findVoiceChannel(event.getArgs(), event.getGuild());
            if(list.isEmpty())
            {
                event.reply(event.getClient().getError()+" No voice channel found matching `"+event.getArgs()+"`!");
                return;
            }
            if(list.size()>1)
            {
                event.reply(FormatUtil.listOfVoice(list, event.getArgs()));
                return;
            }
            vc = list.get(0);
        }
        else
        {
            vc = event.getMember().getVoiceState().getChannel();
        }
        if(!PermissionUtil.checkPermission(vc, event.getMember(), Permission.VOICE_MOVE_OTHERS))
        {
            event.reply(event.getClient().getError()+" You don't have permission to move users out of **"+vc.getName()+"**!");
            return;
        }
        if(!PermissionUtil.checkPermission(vc, event.getSelfMember(), Permission.VOICE_MOVE_OTHERS))
        {
            event.reply(event.getClient().getError()+" I don't have permission to move users out of **"+vc.getName()+"**!");
            return;
        }
        try {
            event.getGuild().getAudioManager().openAudioConnection(vc);
        } catch(Exception e) {
            event.reply(event.getClient().getWarning()+" I could not connect to **"+vc.getName()+"**");
            return;
        }
        waiter.waitForEvent(GuildVoiceMoveEvent.class,
                (GuildVoiceMoveEvent e) -> 
                    e.getGuild().equals(event.getGuild()) && e.getMember().equals(event.getGuild().getSelfMember()), 
                (GuildVoiceMoveEvent e) -> {
                    event.getGuild().getAudioManager().closeAudioConnection();
                    e.getChannelLeft().getMembers().stream().forEach(m -> event.getGuild().getController().moveVoiceMember(m, e.getChannelJoined()).queue());
                });
        event.reply("\uD83C\uDF9B Now, move me and I'll drag users to a new voice channel.");
    }
    
}
