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
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import java.util.List;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import com.jagrosh.vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class UnignoreCmd extends Command {
    
    private final Vortex vortex;
    
    public UnignoreCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.guildOnly = true;
        this.name = "unignore";
        this.aliases = new String[]{"deignore","delignore","removeignore"};
        this.category = new Category("AutoMod");
        this.arguments = "<role | channel>";
        this.help = "stop ignoring a role or channel";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty())
        {
            event.replyWarning("Please include a #channel or role to stop ignoring!");
            return;
        }
        
        String id = event.getArgs().replaceAll("<#(\\d{17,20})>", "$1");
        TextChannel tc;
        try {
            tc = event.getGuild().getTextChannelById(id);
        } catch(Exception e) {
            tc = null;
        }
        if(tc!=null)
        {
            if(vortex.getDatabase().ignores.unignore(tc))
                event.replySuccess("Automod is no longer ignoring channel <#"+tc.getId()+">");
            else
                event.replyError("Automod was not already ignoring <#"+tc.getId()+">!");
            return;
        }
        
        List<Role> roles = FinderUtil.findRoles(event.getArgs(), event.getGuild());
        if(roles.isEmpty())
            event.replyError(FormatUtil.filterEveryone("No roles or text channels found for `"+event.getArgs()+"`"));
        else if (roles.size()==1)
        {
            if(vortex.getDatabase().ignores.unignore(roles.get(0)))
                event.replySuccess("Automod is no longer ignoring role `"+roles.get(0).getName()+"`");
            else
                event.replyError(FormatUtil.filterEveryone("Automod was not ignoring role `"+roles.get(0).getName()+"`"
                        + "\n"+event.getClient().getWarning()+" If this role is still listed when using `"+event.getClient().getPrefix()+"ignore`:"
                        + "\n`[can't interact]` - the role is above "+event.getSelfUser().getName()+"'s highest role; try moving the '"+event.getSelfUser().getName()+"' role higher"
                        + "\n`[elevated perms]` - the role has one of the following permissions: Kick Members, Ban Members, Manage Server, Manage Messages, Administrator"));
        }
        else
            event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfRoles(roles, event.getArgs())));
    }
}
