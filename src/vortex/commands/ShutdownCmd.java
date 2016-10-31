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
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ShutdownCmd extends Command {

    public ShutdownCmd()
    {
        this.name = "shutdown";
        this.help = "safely shuts down the bot";
        this.ownerCommand = true;
    }
    
    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        reply(Constants.WARNING+"Shutting down...",event);
        event.getJDA().shutdown();
        return null;
    }
    
}
