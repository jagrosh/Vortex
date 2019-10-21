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
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AutoraidmodeCmd extends Command
{
    private final Vortex vortex;
    
    public AutoraidmodeCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.guildOnly = true;
        this.name = "autoraidmode";
        this.aliases = new String[]{"autoraid","autoantiraid","autoantiraidmode"};
        this.category = new Category("AutoMod");
        this.arguments = "<ON | OFF | joins/seconds>";
        this.help = "enables/disables auto-raidmode";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.botPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            vortex.getDatabase().automod.setAutoRaidMode(event.getGuild(), 0, 0);
            event.replySuccess("Auto-Anti-Raid mode has been disabled.");
            return;
        }
        int joins;
        int seconds;
        if(event.getArgs().equalsIgnoreCase("on"))
        {
            joins = 10;
            seconds = 10;
        }
        else if(!event.getArgs().matches("\\d{1,8}\\s*\\/\\s*\\d{1,8}"))
        {
            event.replyError("Valid options are `OFF`, `ON`, or `<joins>/<seconds>`"
                    + "\nSetting to `OFF` means the bot will never automatically enable raid mode"
                    + "\nSetting to `ON` will use the recommended value of 10 joins per 10 seconds to trigger Anti-Raid mode"
                    + "\nSetting a customizable threshhold is possible; ex: `10/20` for 10 joins in 20 seconds"
                    + "\nFor more information, check out the wiki: <"+Constants.Wiki.RAID_MODE+">");
            return;
        }
        else
        {
            String[] parts = event.getArgs().split("\\s*\\/\\s*");
            joins = Integer.parseInt(parts[0]);
            seconds = Integer.parseInt(parts[1]);
        }
        vortex.getDatabase().automod.setAutoRaidMode(event.getGuild(), joins, seconds);
        event.replySuccess("Anti-Raid mode will be enabled automatically when there are `"+joins+"` joins in `"+seconds+"` seconds.");
    }
}
