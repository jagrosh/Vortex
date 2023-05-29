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
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.util.LinkedList;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class UngravelCmd extends ModCommand
{
    public UngravelCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "ungravel";
        this.arguments = "<@users> [reason]";
        this.help = "removes graveled role from users";
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Role gravelRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getGravelRole(event.getGuild());
        if(gravelRole == null)
        {
            event.replyError("No Graveled role exists!");
            return;
        }
        if(!event.getMember().canInteract(gravelRole))
        {
            event.replyError("You do not have permissions to assign the '"+gravelRole.getName()+"' role!");
            return;
        }
        if(!event.getSelfMember().canInteract(gravelRole))
        {
            event.reply(event.getClient().getError()+" I do not have permissions to assign the '"+gravelRole.getName()+"' role!");
            return;
        }

        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to ungravel (@mention or ID)!");
            return;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        StringBuilder builder = new StringBuilder();
        List<Member> toUngravel = new LinkedList<>();

        args.members.forEach(m ->
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to ungravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to ungravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(!m.getRoles().contains(gravelRole))
                builder.append("\n").append(event.getClient().getError()).append(" ").append(FormatUtil.formatUser(m.getUser())).append(" is not graveled!");
            else
                toUngravel.add(m);
        });

        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a member"));

        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append("The user ").append(u.getAsMention()).append(" is not in this server."));

        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append("The user <@").append(id).append("> is not in this server."));

        if(toUngravel.isEmpty())
        {
            event.reply(builder.toString());
            return;
        }

        if(toUngravel.size() > 5)
            event.reactSuccess();

        for(int i=0; i<toUngravel.size(); i++)
        {
            Member m = toUngravel.get(i);
            boolean last = i+1 == toUngravel.size();
            event.getGuild().removeRoleFromMember(m, gravelRole).reason(reason).queue(success ->
            {
                vortex.getDatabase().gravels.removeGravel(event.getGuild(), m.getUser().getIdLong(), event.getAuthor().getIdLong());
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully ungraveled ").append(FormatUtil.formatUser(m.getUser()));
                if(last)
                    event.reply(builder.toString());
            }, failure ->
            {
                builder.append("\n").append(event.getClient().getError()).append(" Failed to ungravel ").append(m.getUser().getAsMention());
                if(last)
                    event.reply(builder.toString());
            });
        }
    }
}
