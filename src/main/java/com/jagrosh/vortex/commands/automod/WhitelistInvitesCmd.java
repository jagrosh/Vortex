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
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.core.Permission;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Michael Ritter (Kantenkugel)
 */
public class WhitelistInvitesCmd extends Command
{
    private final Vortex vortex;
    private final static String DESCRIPTION = "Used to add/remove guilds from the invite whitelist. When an invite to a whitelisted guild is posted, no strikes are given.";

    public WhitelistInvitesCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "whitelist";
        this.guildOnly = true;
        this.category = new Category("AutoMod");
        this.arguments = "<ADD GUILD_ID|REMOVE GUILD_ID|SHOW>";
        this.help = "if strikes for invites are enabled, add/remove whitelisted guilds";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String[] args = event.getArgs().toLowerCase().split("\\s+");
        if(event.getArgs().equalsIgnoreCase("show") || (args.length == 2 && (args[0].equals("add") || args[0].equals("remove"))))
        {
            if(event.getArgs().equalsIgnoreCase("show"))
            {
                List<Long> currentWL = vortex.getDatabase().inviteWhitelist.readWhitelist(event.getGuild());
                event.replySuccess("Whitelisted Guild IDs:\n" + (currentWL.isEmpty() ? "None" :
                        currentWL.stream().map(String::valueOf).collect(Collectors.joining(", "))));
            }
            else
            {
                long guildId;
                try
                {
                    guildId = Long.parseUnsignedLong(args[1]);
                }
                catch(NumberFormatException ex)
                {
                    event.replyWarning("Invalid Guild-ID provided!");
                    return;
                }
                if(args[0].equals("add"))
                {
                    if(!vortex.getDatabase().inviteWhitelist.addToWhitelist(event.getGuild(), guildId))
                    {
                        event.replyWarning("Given Guild was already whitelisted");
                        return;
                    }
                }
                else
                {
                    if(!vortex.getDatabase().inviteWhitelist.removeFromWhitelist(event.getGuild(), guildId))
                    {
                        event.replyWarning("Given Guild was not whitelisted");
                        return;
                    }
                }
                event.replySuccess("Whitelist has been modified");
            }
        }
        else
        {
            event.replyWarning(DESCRIPTION+"\nValid options are `ADD GUILD_ID`, `REMOVE GUILD_ID` and `SHOW`");
        }
        
    }
}
