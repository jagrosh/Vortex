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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class SlowmodeCmd extends ModCommand
{
    private final static int MAX_SLOWMODE = 21600;

    public SlowmodeCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_CHANNEL);
        this.name = "slowmode";
        this.arguments = "[time or OFF]";
        this.help = "enables or disables slowmode";
        this.botPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String args = event.getArgs();
        TextChannel channel = event.getTextChannel();
        int time;

        // Event args is empty
        if (args.isEmpty())
        {
            if (channel.getSlowmode() > 0)
                time = 0;
            else
                time = 5;
        }

        // Event args isn't empty
        else
        {
            // Because parseTime returns 0 if the input is invalid, we'll just check if the argument is not "0",
            // then parse it & check again if it's 0. If it's 0, then it's likely invalid.
            if (args.equalsIgnoreCase("off") || args.equals("0"))
                time = 0;
            else
            {
                // Because parseTime will return 0 when you don't give a time unit (for example, if you give "10" as
                // argument, you expect for slowmode to be set to 10 seconds), we first try to simply parse the argument
                // and if that fails, use parseTime
                try
                {
                    time = Integer.parseInt(args);
                }
                catch (Exception e)
                {
                    time = OtherUtil.parseTime(event.getArgs());
                    if (time <= 0)
                    {
                        event.replyError("Invalid time");
                        return;
                    }
                }

            }
        }

        if (time > MAX_SLOWMODE)
        {
            event.replyError("You can only enable slowmode for up to 6 hours!");
            return;
        }
        if (time < 0)
        {
            event.replyError("Slowmode cannot use negative time!");
            return;
        }

        int finalTime = time;
        channel.getManager().setSlowmode(time).queue(
                success -> event.replySuccess(
                        finalTime > 0
                                ? "Enabled slowmode for " + FormatUtil.secondsToTimeCompact(finalTime) + "!"
                                : "Disabled slowmode!"),

                failure -> event.replyError("Couldn't " + (finalTime > 0 ? "enable" : "disable") + " slowmode!")
        );
    }
}
