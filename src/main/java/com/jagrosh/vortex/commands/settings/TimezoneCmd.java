/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import java.time.ZoneId;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TimezoneCmd extends Command
{
    private final Vortex vortex;
    
    public TimezoneCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "timezone";
        this.help = "sets the log timezone";
        this.arguments = "<zone>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include a time zone. A full list of timezones can be found here: <"+Constants.Wiki.LOG_TIMEZONE+">");
            return;
        }
        
        if(event.getArgs().equalsIgnoreCase("none"))
        {
            vortex.getDatabase().settings.setTimezone(event.getGuild(), null);
            event.replySuccess("The log timezone has been reset.");
            return;
        }
        
        try
        {
            ZoneId newzone = ZoneId.of(event.getArgs());
            vortex.getDatabase().settings.setTimezone(event.getGuild(), newzone);
            event.replySuccess("The log timezone has been set to `"+newzone.getId()+"`");
        }
        catch(Exception ex)
        {
            event.replyError("`"+event.getArgs()+"` is not a valid timezone! See <"+Constants.Wiki.LOG_TIMEZONE+"> for a full list.");
        }
    }
}
