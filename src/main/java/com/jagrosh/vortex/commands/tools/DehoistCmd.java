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
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;

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
            symbol = AutoMod.VALID_DEHOIST_CHAR[0];
        else if(event.getArgs().length()==1)
            symbol = event.getArgs().charAt(0);
        else
            throw new CommandErrorException("Provided symbol must be one character of the following: "+AutoMod.DEHOIST_JOINED);
        boolean allowed = false;
        for(char c: AutoMod.VALID_DEHOIST_CHAR)
            if(c==symbol)
                allowed = true;
        if(!allowed)
            throw new CommandErrorException("Provided symbol must be one character of the following: "+AutoMod.DEHOIST_JOINED);
        List<Member> toDehoist = event.getGuild().getMembers().stream()
                .filter(m -> event.getSelfMember().canInteract(m) && m.getEffectiveName().charAt(0)<=symbol)
                .collect(Collectors.toList());
        toDehoist.forEach(m -> 
        {
            String newname = AutoMod.DEHOIST_PREFIX+m.getEffectiveName();
            if(newname.length()>32)
                newname = newname.substring(0,32);
            event.getGuild().getController().setNickname(m, newname).queue();
        });
        event.replySuccess("Dehoisting `"+toDehoist.size()+"` members with names starting with `"+symbol+"` or higher.");
    }
    
}
