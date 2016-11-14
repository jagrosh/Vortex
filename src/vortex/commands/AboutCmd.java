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

import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import vortex.Command;
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AboutCmd extends Command {

    public AboutCmd()
    {
        this.name = "about";
        this.help = "shows info about the bot";
    }
    
    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        return reply("Hello. I am **"+event.getJDA().getSelfUser().getName()+"**, a simple moderation bot built by **jagrosh**#4824."
                + "\nI'm here to help keep your server safe and make moderating easy!"
                + "\nMy prefix is `"+Constants.PREFIX+"` and if you type `"+Constants.PREFIX+"help` I will DM you my commands."
                + "\nI was written in Java, using the JDA library ("+JDAInfo.VERSION+")"
                + "\nI am on **"+event.getJDA().getGuilds().size()+"** servers, and can see **"+event.getJDA().getUsers().size()+"** unique users!"
                + "\n\nFor additional help or suggestions, please join the support server: "+Constants.SERVER_INVITE,event);
    }
    
}
