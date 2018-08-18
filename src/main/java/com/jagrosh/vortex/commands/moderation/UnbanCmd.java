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

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.ArgsUtil.ResolvedArgs;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild.Ban;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class UnbanCmd extends ModCommand
{
    public UnbanCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "unban";
        this.arguments = "<@users> [reason]";
        this.help = "unbans users";
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to unban (@mention or ID)!");
            return;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        StringBuilder builder = new StringBuilder();
        
        event.getGuild().getBanList().queue(list -> 
        {
            List<Ban> toUnban = new LinkedList<>();
            args.members.forEach(m -> args.users.add(m.getUser()));
            args.users.forEach(u -> 
            {
                Ban ban = list.stream().filter(b -> b.getUser().getIdLong()==u.getIdLong()).findFirst().orElse(null);
                if(ban==null)
                    builder.append("\n").append(event.getClient().getError()).append(" ").append(FormatUtil.formatUser(u)).append(" is not banned!");
                else
                    toUnban.add(ban);
            });
            args.ids.forEach(id -> 
            {
                Ban ban = list.stream().filter(b -> b.getUser().getIdLong()==id).findFirst().orElse(null);
                if(ban==null)
                    builder.append("\n").append(event.getClient().getError()).append(" <@").append(id).append("> is not banned!");
                else
                    toUnban.add(ban);
            });
            args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a user ID"));
            
            if(toUnban.isEmpty())
            {
                event.reply(builder.toString());
                return;
            }
            
            if(toUnban.size() > 5)
                event.reactSuccess();
            
            for(int i=0; i<toUnban.size(); i++)
            {
                Ban ban = toUnban.get(i);
                boolean last = i+1 == toUnban.size();
                event.getGuild().getController().unban(ban.getUser()).reason(reason).queue(success -> 
                {
                    builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully unbanned ").append(FormatUtil.formatUser(ban.getUser()));
                    if(last)
                        event.reply(builder.toString());
                }, f -> 
                {
                    builder.append("\n").append(event.getClient().getError()).append(" Failed to unban ").append(FormatUtil.formatUser(ban.getUser()));
                    if(last)
                        event.reply(builder.toString());
                });
            }

        }, f -> event.replyError("Failed to retreive the ban list."));
    }
}
