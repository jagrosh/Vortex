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
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.GuildSettingsDataManager;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PrefixCmd extends Command
{
    private final Vortex vortex;
    
    public PrefixCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "prefix";
        this.help = "sets the server prefix";
        this.arguments = "<prefix or NONE>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a prefix. The server's current prefix can be seen via the `"+event.getClient().getPrefix()+"settings` command");
            return;
        }
        
        if(event.getArgs().equalsIgnoreCase("none"))
        {
            vortex.getDatabase().settings.setPrefix(event.getGuild(), null);
            event.replySuccess("The server prefix has been reset.");
            return;
        }
        
        if(event.getArgs().length()>GuildSettingsDataManager.PREFIX_MAX_LENGTH)
        {
            event.replySuccess("Prefixes cannot be longer than `"+GuildSettingsDataManager.PREFIX_MAX_LENGTH+"` characters.");
            return;
        }
        
        vortex.getDatabase().settings.setPrefix(event.getGuild(), event.getArgs());
        event.replySuccess("The server prefix has been set to `"+event.getArgs()+"`\n"
                + "Note that the default prefix (`"+event.getClient().getPrefix()+"`) cannot be removed and will work in addition to the custom prefix.");
    }
}
