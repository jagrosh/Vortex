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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.List;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class LogCommand extends Command
{
    public static Permission[] REQUIRED_PERMS = {Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY};
    public static String REQUIRED_ERROR = "I am missing the necessary permissions (Read Messages, Send Messages, Read Message History, and Embed Links) in %s!";
    protected final Vortex vortex;
    
    public LogCommand(Vortex vortex)
    {
        this.vortex = vortex;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.guildOnly = true;
        this.arguments = "<#channel or OFF>";
        this.category = new Category("Settings");
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            showCurrentChannel(event);
            return;
        }
        if(event.getArgs().equalsIgnoreCase("off") || event.getArgs().equalsIgnoreCase("none"))
        {
            setLogChannel(event, null);
            return;
        }
        List<TextChannel> list = FinderUtil.findTextChannels(event.getArgs(), event.getGuild());
        if(list.isEmpty())
        {
            event.replyError("I couldn't find any text channel called `"+event.getArgs()+"`.");
            return;
        }
        if(list.size()>1)
        {
            event.replyWarning(FormatUtil.listOfText(list, event.getArgs()));
            return;
        }
        
        TextChannel tc = list.get(0);
        
        if(!event.getSelfMember().hasPermission(tc, REQUIRED_PERMS))
        {
            event.replyError(String.format(REQUIRED_ERROR, tc.getAsMention()));
            return;
        }
        
        setLogChannel(event, tc);
    }
    
    protected abstract void showCurrentChannel(CommandEvent event);
    
    protected abstract void setLogChannel(CommandEvent event, TextChannel tc);
}
