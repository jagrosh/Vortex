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

import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import vortex.Command;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutomodCmd extends Command {

    public AutomodCmd()
    {
        this.name = "automod";
        this.help = "shows details about the automod system";
    }
    
    @Override
    protected void execute(String args, MessageReceivedEvent event) {
        event.getChannel().sendMessage("__**"+event.getJDA().getSelfInfo().getName()+"** Automatic Moderator:__"
                + "\n\nThe automoderator system is an easy and simple way to stop spammers. "
                + "Each feature can be enabled by assigning the bot a role with the correct formatting. "
                + "The automoderator ignores users with Manage Messages, Kick Members, Ban Members, Manage Server, and Administrator permissions."
                
                +"\n\n**Mod Logging** - To enable logging for all moderator commands and automoderator actions, create a channel that the bot can "
                + "read and write to, and name it `mod_log` or `modlog` or `moderation_log` (anything that contains 'modlog', _or_ starts with 'mod' "
                + "and ends with 'log')"
                
                + "\n\n**AntiMention** - The bot will automatically ban users that mention too many non-bot, non-self users in a single message. "
                + "To enable this, give the bot a role called `AntiMention:X`, where X is the number of mentions to ban at. X must be at least 6."
                + "\nExample role name: `AntiMention:10`").queue();
    }
    
}
