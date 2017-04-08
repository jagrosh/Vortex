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

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutomodCmd extends Command {

    public AutomodCmd()
    {
        this.name = "automod";
        this.help = "shows details about the automod system";
        this.guildOnly = false;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        String response = "__**"+event.getJDA().getSelfUser().getName()+"** Automatic Moderator:__"
                + "\n"
                + "\n**NOTE: THE AUTOMATIC MODERATION SYSTEM HAS BEEN CHANGED**"
                + "\n"
                + "\nAll automoderator setup is done via commands in the 'AutoMod' section of the commands. See `"+Constants.PREFIX+"help` for a list of commands."
                + "\n"
                + "\nThe automoderator ignores all users with any of the following permissions: Administrator, Manage Server, Ban Members, Kick Members, Manage Messages"
                + "\nIt also ignores all bots, and any user with a role higher than its highest role.";
        event.reply(response);
    }
    
}
