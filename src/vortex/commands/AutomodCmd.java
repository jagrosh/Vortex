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
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutomodCmd extends Command {

    public AutomodCmd()
    {
        this.name = "automod";
        this.arguments = "[topic]";
        this.help = "shows details about the automod system";
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String response = "__**"+event.getJDA().getSelfUser().getName()+"** Automatic Moderator:__\n\n";
        switch(event.getArgs())
        {
            default:
                response += "Unknown automod topic `"+event.getArgs()+"`\n\n";
            case "":
                response += "The automoderator system is an easy and simple way to stop spammers. "
                    + "Each feature can be enabled by assigning the bot a role with the correct formatting. See the following topics for more information:\n"
                        + "\n`"+Constants.PREFIX+"automod ModLog` - log actions to a channel"
                        + "\n`"+Constants.PREFIX+"automod AntiMention` - prevent mass-mention spammers"
                        + "\n`"+Constants.PREFIX+"automod AntiInvite` - prevent invite links to other servers"
                        + "\n`"+Constants.PREFIX+"automod Shield` - protect users from the automoderator"
                        ;
                break;
            case "modlog":
                response += "**Mod Logging** - To enable logging for all moderator commands and automoderator actions, create a channel that the bot can "
                    + "read and write to, and name it `mod_log` or `modlog` or `moderation_log` (anything that contains 'modlog', _or_ starts with 'mod' "
                    + "and ends with 'log')";
                break;
            case "antimention":
                response += "**AntiMention** - The bot will automatically ban users that mention too many non-bot, non-self users in a single message. "
                    + "To enable this, give the bot a role called `AntiMention:X`, where X is the number of mentions to ban at. X must be at least 7."
                    + "\nExample role name: `AntiMention:10`";
                break;
            case "antiinvite":
                response += "**AntiInvite** - The bot will automatically remove messages with invite links, and warn or punish the user. "
                    + "To enable this, give the bot a role called `AntiInvite:X`, where X is one of the following actions: `Ban` (deletes the message and bans the user) "
                    + "`Kick` (deletes the message and kicks the user) `Warn` (deletes the message and warns the user) `Delete` (deletes the message)"
                    + "\nExample role name: `AntiInvite:Warn`\nAdditionally, any channel with `{invites}` in the topic will be ignored by the anti-invite system.";
                break;
            case "shield":
                response += "**VortexShield** (automod immunity) - The bot will not perform automoderator actions against users with any of the following permissions: "
                    + "Manage Messages, Kick Members, Ban Members, Manage Server, Administrator"
                    + "\nAlso, anyone with a role called `VortexShield` is immune to the automoderator.";
        }
        event.reply(response);
    }
    
}
