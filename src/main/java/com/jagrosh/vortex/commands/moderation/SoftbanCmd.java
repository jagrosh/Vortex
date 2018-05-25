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

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.util.List;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SoftbanCmd extends ModCommand
{
    public SoftbanCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "softban";
        this.arguments = "<@users> [reason]";
        this.help = "bans and unbans users";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to softban (@mention or ID)!");
            return;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        String unbanreason = LogUtil.auditReasonFormat(event.getMember(), "Softban Unban");
        StringBuilder builder = new StringBuilder();
        List<Member> toSoftban = new LinkedList<>();
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to softban ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to softban ").append(FormatUtil.formatUser(m.getUser()));
            else if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't softban ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
            else
                toSoftban.add(m);
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a member"));
        
        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append("The user ").append(u.getAsMention()).append(" is not in this server."));
        
        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append("The user <@").append(id).append("> is not in this server."));
        
        if(toSoftban.isEmpty())
        {
            event.reply(builder.toString());
            return;
        }
        
        if(toSoftban.size() > 5)
            event.reactSuccess();
        
        for(int i=0; i<toSoftban.size(); i++)
        {
            Member m = toSoftban.get(i);
            boolean last = i+1 == toSoftban.size();
            event.getGuild().getController().ban(m, 1, reason).queue(success -> 
            {
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully softbanned ").append(FormatUtil.formatUser(m.getUser()));
                event.getGuild().getController().unban(m.getUser().getId()).reason(unbanreason).queueAfter(4, TimeUnit.SECONDS);
                if(last)
                    event.reply(builder.toString());
            }, failure -> 
            {
                builder.append("\n").append(event.getClient().getError()).append(" Failed to softban ").append(FormatUtil.formatUser(m.getUser()));
                if(last)
                    event.reply(builder.toString());
            });
        }
    }
}
