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

import java.time.OffsetDateTime;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.User;

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
        StringBuilder sb = new StringBuilder("**"+event.getSelfUser().getName()+"** statistics:"
                + "\nLast Startup: "+FormatUtil.secondsToTime(Constants.STARTUP.until(OffsetDateTime.now(), ChronoUnit.SECONDS))+" ago"
                + "\nMemory: **"+usedMb+"**Mb / **"+totalMb+"**Mb\n");
        vortex.getShardManager().getShardManagers().forEach(bot -> 
        //Stream.of(vortex.getShardManager()).forEach(bot -> 
        {
            User self = bot.getShards().get(0).getSelfUser();
            sb.append("\n__**").append(self.getName()).append("** (").append(self.getId()).append(")__")
                    .append("\nGuilds: **").append(bot.getGuildCache().size()).append("**")
                    .append("\nAverage Ping: **").append(bot.getAverageGatewayPing()).append("**ms")
                    .append("\nShard Total: **").append(bot.getShardsTotal()).append("**")
                    .append("\nShard Connectivity: ```diff");
            bot.getShards().forEach(jda -> sb.append("\n").append(jda.getStatus()==JDA.Status.CONNECTED ? "+ " : "- ")
                    .append(jda.getShardInfo().getShardId()<10 ? "0" : "").append(jda.getShardInfo().getShardId()).append(": ").append(jda.getStatus())
                    .append(" ~ ").append(jda.getGuildCache().size()).append(" guilds"));
            sb.append("\n```");
        });
        event.reply(sb.toString().trim());
    }
}
