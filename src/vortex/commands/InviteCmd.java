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

import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import vortex.Command;
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class InviteCmd extends Command {

    public InviteCmd()
    {
        this.name = "invite";
        this.help = "shows how to invite the bot";
    }
    
    @Override
    protected void execute(String args, MessageReceivedEvent event) {
        event.getChannel().sendMessage("Hello. I am **"+event.getJDA().getSelfInfo().getName()+"**, a simple moderation bot built by **jagrosh**#4824."
                + "\nYou can add me to your server with the link below:"
                + "\n\n\uD83D\uDD17 **https://discordapp.com/oauth2/authorize?client_id=240254129333731328&permissions=403041342&scope=bot**"
                + "\n\nFor help or suggestions, please join the support server: "+Constants.SERVER_INVITE).queue();
    }
    
}
