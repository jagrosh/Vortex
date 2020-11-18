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
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DehoistCmd extends Command
{
    public DehoistCmd()
    {
        this.name = "dehoist";
        this.arguments = "[symbol]";
        this.help = "modifies users' nicknames to prevent using ascii characters as a hoist";
        this.category = new Category("Tools");
        this.botPermissions = new Permission[]{Permission.NICKNAME_MANAGE};
        this.userPermissions = new Permission[]{Permission.NICKNAME_MANAGE};
        this.guildOnly = true;
        this.cooldown = 10;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        char symbol;
        if(event.getArgs().isEmpty())
            symbol = OtherUtil.DEHOIST_ORIGINAL[0];
        else if(event.getArgs().length()==1)
            symbol = event.getArgs().charAt(0);
        else
            throw new CommandErrorException("Provided symbol must be one character of the following: "+OtherUtil.DEHOIST_JOINED);
        boolean allowed = false;
        for(char c: OtherUtil.DEHOIST_ORIGINAL)
            if(c==symbol)
                allowed = true;
        if(!allowed)
            throw new CommandErrorException("Provided symbol must be one character of the following: "+OtherUtil.DEHOIST_JOINED);
        
        long count = event.getGuild().getMembers().stream().filter(m -> OtherUtil.dehoist(m, symbol)).count();
        
        event.replySuccess("Dehoisting `"+count+"` members with names starting with `"+symbol+"` or higher.");
    }
    
}
