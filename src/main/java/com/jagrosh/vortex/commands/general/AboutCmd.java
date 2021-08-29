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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.FormatUtil;
import java.awt.Color;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDAInfo;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCmd extends Command
{
    private final Vortex vortex;

    public AboutCmd(Vortex vortex)
    {
        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
        this.vortex = vortex;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        ShardManager sm = event.getJDA().getShardManager();
        event.reply(new MessageBuilder()
                .setContent(Constants.VORTEX_EMOJI + " **All about Vortex** " + Constants.VORTEX_EMOJI)
                .setEmbed(new EmbedBuilder()
                        .setColor(!event.isFromType(ChannelType.TEXT) ? Color.GRAY : event.getSelfMember().getColor())
                        .setDescription("Hi! I'm Toybot!\n"
                                + "I'm a modified version of [Vortex](https://github.com/jagrosh/Vortex) which was written in Java by [jagrosh#4824](https://github.com/jagrosh)\n"
                                + "I was customised for this server by <@791520107939102730> with the help of <@384774787823828995>, <@523655829279342593> and <@105725338541101056> as well as some other contributers that can be found on [GitHub](https://github.com/ya64/Vortex)"
                                + "Type `" + event.getClient().getPrefix() + event.getClient().getHelpWord() + "` for help and information.\n\n"
                                + FormatUtil.helpLinks(event))
                        .addField("Stats", sm.getShardsTotal()+ " Shards\n" + sm.getGuildCache().size() + " Servers", true)
                        .addField("", sm.getUserCache().size() + " Users\n" + Math.round(sm.getAverageGatewayPing()) + "ms Avg Ping", true)
                        .addField("", sm.getTextChannelCache().size() + " Text Channels\n" + sm.getVoiceChannelCache().size() + " Voice Channels", true)
                        .setFooter("Last restart", null)
                        .setTimestamp(event.getClient().getStartTime())
                        .build())
                .build());
    }
}
