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
package com.jagrosh.vortex.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.utils.TimeFormat;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class DebugCmd extends Command
{
    private final Vortex vortex;
    
    public DebugCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "debug";
        this.help = "shows some debug stats";
        this.ownerCommand = true;
        this.guildOnly = false;
        this.hidden = true;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        long totalMb = Runtime.getRuntime().totalMemory()/(1024*1024);
        long usedMb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())/(1024*1024);
        String sb = "**" + event.getSelfUser().getName() + "** statistics:"
                + "\nLast Startup: " + TimeFormat.RELATIVE.format(Constants.STARTUP)
                + "\nGuilds: **" + vortex.getJda().getGuildCache().size() + "**"
                + "\nMemory: **" + usedMb + "**Mb / **" + totalMb + "**Mb"
                + "\nGateway Ping: **" + vortex.getJda().getGatewayPing() + "**ms"
                + "\nShard Connectivity: ```diff" + "\n```";
        event.reply(sb.trim());
    }
}
