/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.database.managers.PremiumManager.PremiumInfo;
import com.jagrosh.vortex.utils.OtherUtil;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumCmd extends Command
{
    private final Vortex vortex;
    
    public PremiumCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "premium";
        this.help = "gives premium";
        this.arguments = "<guildId> <time>";
        this.ownerCommand = true;
        this.guildOnly = false;
        this.hidden = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String[] parts = event.getArgs().split("\\s+", 2);
        if(parts.length < 2)
        {
            event.replyError("Too few arguments");
            return;
        }
        int seconds = OtherUtil.parseTime(parts[1]);
        if(seconds == 0)
        {
            event.replyError("Invalid time");
            return;
        }
        Guild guild;
        try
        {
            guild = vortex.getShardManager().getGuildById(Long.parseLong(parts[0]));
        }
        catch(NumberFormatException ex)
        {
            event.replyError("Invalid guild ID");
            return;
        }
        if(guild == null)
        {
            event.replyError("No guild found with ID `" + parts[0] + "`");
            return;
        }
        PremiumInfo before = vortex.getDatabase().premium.getPremiumInfo(guild);
        vortex.getDatabase().premium.addPremium(guild, PremiumManager.Level.PRO, seconds, ChronoUnit.SECONDS);
        PremiumInfo after = vortex.getDatabase().premium.getPremiumInfo(guild);
        event.replySuccess("Before: " + before + "\n" + event.getClient().getSuccess() + " After: " + after);
    }
}
