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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ModroleCmd extends Command
{
    private final Vortex vortex;
    
    public ModroleCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "modrole";
        this.help = "sets the moderator role";
        this.aliases = new String[]{"moderatorrole"};
        this.arguments = "<role>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER, Permission.BAN_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include the name of a role to use as the Moderator role. Members with this role will be able to use all Moderation commands.");
            return;
        }
        
        else if(event.getArgs().equalsIgnoreCase("none") || event.getArgs().equalsIgnoreCase("off"))
        {
            vortex.getDatabase().settings.setModeratorRole(event.getGuild(), null);
            event.replySuccess("Moderation commands can now only be used by members that can perform the actions manually.");
            return;
        }
        
        List<Role> roles = FinderUtil.findRoles(event.getArgs(), event.getGuild());
        if(roles.isEmpty())
            event.replyError("No roles found called `"+event.getArgs()+"`");
        else if (roles.size()==1)
        {
            vortex.getDatabase().settings.setModeratorRole(event.getGuild(), roles.get(0));
            event.replySuccess("Users with the `"+roles.get(0).getName()+"` role can now use all Moderation commands.");
        }
        else
            event.replyWarning(FormatUtil.listOfRoles(roles, event.getArgs()));
    }
}
