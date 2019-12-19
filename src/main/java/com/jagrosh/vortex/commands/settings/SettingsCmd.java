/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PremiumManager.PremiumInfo;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SettingsCmd extends Command
{
    private final Vortex vortex;
    
    public SettingsCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "settings";
        this.category = new Category("Settings");
        this.help = "shows current settings";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        PremiumInfo pi = vortex.getDatabase().premium.getPremiumInfo(event.getGuild());
        event.getChannel().sendMessage(new MessageBuilder()
                .append(FormatUtil.filterEveryone("**" + event.getSelfUser().getName() + "** settings on **" + event.getGuild().getName() + "**:"))
                .setEmbed(new EmbedBuilder()
                        //.setThumbnail(event.getGuild().getIconId()==null ? event.getSelfUser().getEffectiveAvatarUrl() : event.getGuild().getIconUrl())
                        .addField(vortex.getDatabase().settings.getSettingsDisplay(event.getGuild()))
                        .addField(vortex.getDatabase().actions.getAllPunishmentsDisplay(event.getGuild()))
                        .addField(vortex.getDatabase().automod.getSettingsDisplay(event.getGuild()))
                        .addField(vortex.getDatabase().filters.getFiltersDisplay(event.getGuild()))
                        .setFooter(pi.getFooterString(), null)
                        .setTimestamp(pi.getTimestamp())
                        .setColor(event.getSelfMember().getColor())
                        .build()).build()).queue();
    }
    
}
