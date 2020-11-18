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
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

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
        this.arguments = "[time or OFF] | [time to disable slowmode]";
        this.help = "enables or disables slowmode";
        this.botPermissions = new Permission[]{Permission.MANAGE_CHANNEL};
    }

    @Override
    protected void execute(CommandEvent event)
    {

        if(event.getArgs().isEmpty())
        {
            int slowmodeDuration = vortex.getDatabase().tempslowmodes.timeUntilDisableSlowmode(event.getTextChannel());
            int slowmodeTime = event.getTextChannel().getSlowmode();

            if(slowmodeTime <= 0)
            {
                event.reply("Slowmode is disabled.");
                return;
            }

            if(slowmodeDuration <= 0)
                event.reply("Slowmode is enabled with 1 message every "+FormatUtil.secondsToTimeCompact(slowmodeTime)+".");
            else
                event.reply("Slowmode is enabled with 1 message every "+FormatUtil.secondsToTimeCompact(slowmodeTime) +
                        " for "+FormatUtil.secondsToTimeCompact(slowmodeDuration)+".");
            return;
        }

        String args = event.getArgs();

        if(args.equals("0") || args.equalsIgnoreCase("off"))
        {
            vortex.getDatabase().tempslowmodes.clearSlowmode(event.getTextChannel());
            event.getTextChannel().getManager()
                    .setSlowmode(0)
                    .reason(LogUtil.auditReasonFormat(event.getMember(), "Disabled slowmode"))
                    .queue();
            event.replySuccess("Disabled slowmode!");
            return;
        }

        String[] split = args.split("\\|",2);

        int slowmodeTime = OtherUtil.parseTime(split[0]);
        if(slowmodeTime == -1)
        {
            event.replyError("Invalid slowmode time!");
            return;
        }
        if(slowmodeTime > MAX_SLOWMODE)
        {
            event.replyError("You can only enable slowmode for up to 6 hours!");
            return;
        }
        if(slowmodeTime < -1)
        {
            event.replyError("Slowmode cannot use negative time!");
            return;
        }

        int slowmodeDuration = split.length == 1 ? 0 : OtherUtil.parseTime(split[1]);
        if(slowmodeDuration == -1)
        {
            event.replyError("Invalid slowmode duration time!");
            return;
        }
        if(slowmodeDuration < -1)
        {
            event.replyError("Slowmode duration cannot use negative time!");
            return;
        }

        event.getTextChannel().getManager()
                .setSlowmode(slowmodeTime)
                .reason(LogUtil.auditReasonFormat(event.getMember(), slowmodeDuration/60, "Enabled slowmode"))
                .queue(s ->
                {
                    if(slowmodeDuration <= 0) return;
                    vortex.getThreadpool().schedule(
                            () -> vortex.getDatabase().tempslowmodes.setSlowmode(event.getTextChannel(), Instant.now().plus(slowmodeDuration, ChronoUnit.SECONDS)),
                            10, TimeUnit.SECONDS
                    );
                });

        event.replySuccess("Enabled slowmode with 1 message every " + FormatUtil.secondsToTimeCompact(slowmodeTime) +
                (slowmodeDuration > 0 ? " for "+FormatUtil.secondsToTimeCompact(slowmodeDuration) : "")+".");
    }
}
