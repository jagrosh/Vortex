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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RaidCmd extends ModCommand
{
    public RaidCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_SERVER, Permission.KICK_MEMBERS);
        this.name = "raidmode";
        this.aliases = new String[]{"raid","antiraidmode"};
        this.arguments = "[ON|OFF] [reason]";
        this.help = "view, enable, or disable raidmode";
        this.botPermissions = new Permission[]{Permission.MANAGE_SERVER, Permission.KICK_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        boolean active = vortex.getDatabase().settings.getSettings(event.getGuild()).isInRaidMode();
        String[] parts = event.getArgs().split("\\s+", 2);
        if(parts[0].equalsIgnoreCase("off") || parts[0].equalsIgnoreCase("stop") || parts[0].equalsIgnoreCase("disable"))
        {
            if(active)
            {
                vortex.getAutoMod().disableRaidMode(event.getGuild(), event.getMember(), event.getMessage().getTimeCreated(), parts.length>1 ? parts[1] : null);
                event.replySuccess("Anti-Raid Mode has been disabled.");
            }
            else
                event.replyError("Anti-Raid Mode is not currently enabled!");
        }
        else if (parts[0].equalsIgnoreCase("on") || parts[0].equalsIgnoreCase("start") || parts[0].equalsIgnoreCase("enable"))
        {
            if(!active)
            {
                vortex.getAutoMod().enableRaidMode(event.getGuild(), event.getMember(), event.getMessage().getTimeCreated(), parts.length>1 ? parts[1] : null);
                event.replySuccess("Anti-Raid Mode enabled. New members will be prevented from joining.");
            }
            else
                event.replyError("Anti-Raid Mode is already enabled!");
        }
        else
        {
            event.replySuccess("Anti-Raid Mode is currently `"+(active ? "ACTIVE" : "NOT ACTIVE")+"`\n\n"
                    + "`"+event.getClient().getPrefix()+name+" ON` to enable Anti-Raid Mode\n"
                    + "`"+event.getClient().getPrefix()+name+" OFF` to disable Anti-Raid Mode");
        }
    }
}
