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
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.JDAUtilitiesInfo;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FormatUtil;
import java.awt.Color;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AboutCmd extends Command
{
    public AboutCmd()
    {
        this.name = "about";
        this.help = "shows info about the bot";
        this.guildOnly = false;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ShardManager sm = event.getJDA().asBot().getShardManager();
        event.reply(new MessageBuilder()
                .setContent(Constants.VORTEX_EMOJI + " **All about Vortex** " + Constants.VORTEX_EMOJI)
                .setEmbed(new EmbedBuilder()
                        .setColor(event.getGuild()==null ? Color.GRAY : event.getSelfMember().getColor())
                        .setDescription("Hello, I am **Vortex**#8540, a bot designed to keep your server safe and make moderating fast and easy!\n"
                                + "I was written in Java by **jagrosh**#4824 using [JDA](" + JDAInfo.GITHUB + ") and [JDA-Utilities](" + JDAUtilitiesInfo.GITHUB + ")\n"
                                + "Type `" + event.getClient().getPrefix() + event.getClient().getHelpWord() + "` for help and information.\n\n"
                                + FormatUtil.helpLinks(event))
                        .addField("Stats", sm.getShardsTotal()+ " Shards\n" + sm.getGuildCache().size() + " Servers", true)
                        .addField("", sm.getUserCache().size() + " Users\n" + Math.round(sm.getAveragePing()) + "ms Avg Ping", true)
                        .addField("", sm.getTextChannelCache().size() + " Text Channels\n" + sm.getVoiceChannelCache().size() + " Voice Channels", true)
                        .setFooter("Last restart", null)
                        .setTimestamp(event.getClient().getStartTime())
                        .build())
                .build());
    }
}
