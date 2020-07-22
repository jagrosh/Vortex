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
package com.jagrosh.vortex.commands;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.CommandListener;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.Usage;
import net.dv8tion.jda.api.entities.ChannelType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CommandExceptionListener implements CommandListener
{
    private final Logger log = LoggerFactory.getLogger("Command");
    private final Usage usage = new Usage();
    
    @Override
    public void onCommandException(CommandEvent event, Command command, Throwable throwable)
    {
        if (throwable instanceof CommandErrorException)
            event.replyError(FormatUtil.filterEveryone(throwable.getMessage()));
        else if (throwable instanceof CommandWarningException)
            event.replyWarning(FormatUtil.filterEveryone(throwable.getMessage()));
        else
            log.error("An exception occurred in a command: "+command, throwable);
    }
    
    public static class CommandErrorException extends RuntimeException
    {
        public CommandErrorException(String message)
        {
            super(message);
        }
    }
    
    public static class CommandWarningException extends RuntimeException
    {
        public CommandWarningException(String message)
        {
            super(message);
        }
    }

    @Override
    public void onCommand(CommandEvent event, Command command)
    {
        if(event.isFromType(ChannelType.TEXT))
            usage.increment(event.getGuild().getIdLong());
    }
    
    public Usage getUsage()
    {
        return usage;
    }
}
