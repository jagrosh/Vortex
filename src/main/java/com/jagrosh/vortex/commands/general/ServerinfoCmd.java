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
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.utils.FormatUtil;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ServerinfoCmd extends Command
{
    private final String linestart = "\u25AB";
    public ServerinfoCmd()
    {
        this.name = "serverinfo";
        this.aliases = new String[]{"server","guildinfo"};
        this.help = "shows server info";
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        Guild guild = event.getGuild();
        long onlineCount = guild.getMembers().stream().filter((u) -> (u.getOnlineStatus()!=OnlineStatus.OFFLINE)).count();
        long botCount = guild.getMembers().stream().filter(m -> m.getUser().isBot()).count();
        EmbedBuilder builder = new EmbedBuilder();
        String title = FormatUtil.filterEveryone("\uD83D\uDDA5 Information about **"+guild.getName()+"**:");
        String verif;
        switch(guild.getVerificationLevel()) {
            case VERY_HIGH: verif = "┻━┻ミヽ(ಠ益ಠ)ノ彡┻━┻"; break;
            case HIGH:    verif = "(╯°□°）╯︵ ┻━┻"; break;
            default:      verif = guild.getVerificationLevel().name(); break;
        }
        String str = linestart+"ID: **"+guild.getId()+"**\n"
                +linestart+"Owner: "+FormatUtil.formatUser(guild.getOwner().getUser())+"\n"
                +linestart+"Location: "+guild.getRegion().getEmoji()+" **"+guild.getRegion().getName()+"**\n"
                +linestart+"Creation: **"+guild.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
                +linestart+"Users: **"+guild.getMembers().size()+"** ("+onlineCount+" online, "+botCount+" bots)\n"
                +linestart+"Channels: **"+guild.getTextChannels().size()+"** Text, **"+guild.getVoiceChannels().size()+"** Voice\n"
                +linestart+"Verification: **"+verif+"**";
        if(guild.getSplashId()!=null)
        {
            builder.setImage(guild.getSplashUrl()+"?size=1024");
            str += "\n<:partner:314068430556758017> **Discord Partner** <:partner:314068430556758017>";
        }
        if(guild.getIconUrl()!=null)
            builder.setThumbnail(guild.getIconUrl());
        builder.setColor(guild.getOwner().getColor());
        builder.setDescription(str);
        event.reply(new MessageBuilder().append(title).setEmbed(builder.build()).build());
    }
    
}
