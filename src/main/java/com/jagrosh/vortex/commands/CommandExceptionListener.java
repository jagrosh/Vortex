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

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CommandExceptionListener implements CommandListener
{
    @Override
    public void onCommandException(CommandEvent event, Command command, Throwable throwable)
    {
        if (throwable instanceof CommandErrorException)
            event.replyError(throwable.getMessage());
        else if (throwable instanceof CommandWarningException)
            event.replyWarning(throwable.getMessage());
        else
            throwable.printStackTrace();
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
}
