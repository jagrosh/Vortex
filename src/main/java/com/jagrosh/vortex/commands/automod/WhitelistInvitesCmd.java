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

import java.util.ArrayList;
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
        this.arguments = "<ADD GUILD_ID[ GUILD_ID...]|REMOVE GUILD_ID[ GUILD_ID...]|SHOW>";
        this.help = "if strikes for invites are enabled, add/remove whitelisted guilds";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String[] args = event.getArgs().toLowerCase().split("\\s+");
        if(event.getArgs().equalsIgnoreCase("show") || (args.length > 1 && (args[0].equals("add") || args[0].equals("remove"))))
        {
            if(event.getArgs().equalsIgnoreCase("show"))
            {
                List<Long> currentWL = vortex.getDatabase().inviteWhitelist.readWhitelist(event.getGuild());
                event.replySuccess("Whitelisted Guild IDs:\n" + (currentWL.isEmpty() ? "None" :
                        currentWL.stream().map(String::valueOf).collect(Collectors.joining(", "))));
            }
            else
            {
                List<Long> guildIds = readIds(args);
                if(guildIds == null)
                {
                    event.replyWarning("Invalid Guild-ID(s) provided!");
                    return;
                }
                if(args[0].equals("add"))
                {
                    if(guildIds.size() > 1)
                    {
                        vortex.getDatabase().inviteWhitelist.addAllToWhitelist(event.getGuild(), guildIds);
                    }
                    else if(!vortex.getDatabase().inviteWhitelist.addToWhitelist(event.getGuild(), guildIds.get(0)))
                    {
                        event.replyWarning("Given Guild was already whitelisted");
                        return;
                    }
                }
                else
                {
                    if(guildIds.size() > 1)
                    {
                        vortex.getDatabase().inviteWhitelist.removeAllFromWhitelist(event.getGuild(), guildIds);
                    }
                    else if(!vortex.getDatabase().inviteWhitelist.removeFromWhitelist(event.getGuild(), guildIds.get(0)))
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
            event.replyWarning(DESCRIPTION+"\nValid options are `ADD GUILD_ID[ GUILD_ID...]`, `REMOVE GUILD_ID[ GUILD_ID...]` and `SHOW`");
        }
        
    }

    private List<Long> readIds(String[] args)
    {
        List<Long> guildIds = new ArrayList<>(args.length - 1);
        try
        {
            for(int i = 1; i < args.length; i++)
            {
                guildIds.add(Long.parseUnsignedLong(args[i]));
            }
        }
        catch(NumberFormatException ex)
        {
            return null;
        }
        return guildIds;
    }
}
