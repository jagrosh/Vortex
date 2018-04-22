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

import java.util.List;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.util.LinkedList;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class UnmuteCmd extends ModCommand
{
    public UnmuteCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "unmute";
        this.arguments = "<@users> [reason]";
        this.help = "removes muted role from users";
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Role muteRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getMutedRole(event.getGuild());
        if(muteRole == null)
        {
            event.replyError("No Muted role exists!");
            return;
        }
        if(!event.getMember().canInteract(muteRole))
        {
            event.replyError("You do not have permissions to assign the '"+muteRole.getName()+"' role!");
            return;
        }
        if(!event.getSelfMember().canInteract(muteRole))
        {
            event.reply(event.getClient().getError()+" I do not have permissions to assign the '"+muteRole.getName()+"' role!");
            return;
        }
        
        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to unmute (@mention or ID)!");
            return;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        StringBuilder builder = new StringBuilder();
        List<Member> toUnmute = new LinkedList<>();
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to unmute ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to unmute ").append(FormatUtil.formatUser(m.getUser()));
            else if(!m.getRoles().contains(muteRole))
                builder.append("\n").append(event.getClient().getError()).append(" ").append(FormatUtil.formatUser(m.getUser())).append(" is not muted!");
            else
                toUnmute.add(m);
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a member"));
        
        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append("The user ").append(u.getAsMention()).append(" is not in this server."));
        
        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append("The user <@").append(id).append("> is not in this server."));
        
        if(toUnmute.isEmpty())
        {
            event.reply(builder.toString());
            return;
        }
        
        if(toUnmute.size() > 5)
            event.reactSuccess();
        
        for(int i=0; i<toUnmute.size(); i++)
        {
            Member m = toUnmute.get(i);
            boolean last = i+1 == toUnmute.size();
            event.getGuild().getController().removeSingleRoleFromMember(m, muteRole).reason(reason).queue(success -> 
            {
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully unmuted ").append(FormatUtil.formatUser(m.getUser()));
                if(last)
                    event.reply(builder.toString());
            }, failure -> 
            {
                builder.append("\n").append(event.getClient().getError()).append(" Failed to unmute ").append(m.getUser().getAsMention());
                if(last)
                    event.reply(builder.toString());
            });
        }
    }
}
