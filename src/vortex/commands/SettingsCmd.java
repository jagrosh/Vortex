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

import me.jagrosh.jdautilities.commandclient.Command;
import me.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import vortex.AutoMod;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SettingsCmd extends Command {

    private final AutoMod automod;
    public SettingsCmd(AutoMod automod)
    {
        this.automod = automod;
        this.name = "settings";
        this.help = "shows current settings";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }
    
    @Override
    protected void execute(CommandEvent event) {
        event.getChannel().sendMessage(new MessageBuilder()
                .append("**"+event.getSelfUser().getName()+"** settings on **"+event.getGuild().getName()+"**:")
                .setEmbed(new EmbedBuilder()
                        .setDescription(automod.getSettings(event.getGuild()))
                        .build()).build()).queue();
    }
    
}
