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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.ArgsUtil.ResolvedArgs;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StrikeCmd extends ModCommand
{
    public StrikeCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "strike";
        this.arguments = "[number] <@users> <reason>";
        this.help = "applies strikes to users";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        int numstrikes;
        String[] parts = event.getArgs().split("\\s+", 2);
        String str;
        try
        {
            numstrikes = Integer.parseInt(parts[0]);
            str = parts[1];
        }
        catch(NumberFormatException | ArrayIndexOutOfBoundsException ex)
        {
            numstrikes = 1;
            str = event.getArgs();
        }
        if(numstrikes<1 || numstrikes>100)
        {
            event.replyError("Number of strikes must be between 1 and 100!");
            return;
        }
        ResolvedArgs args = ArgsUtil.resolve(str, event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please provide at least one user!");
            return;
        }
        if(args.reason==null || args.reason.isEmpty())
        {
            event.replyError("Please provide a reason for the strike(s)!");
            return;
        }
        StringBuilder builder = new StringBuilder();
        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't strike ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
            else
                args.users.add(m.getUser());
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a user ID"));
        
        List<Long> unknownIds = new LinkedList<>();
        args.ids.forEach(id -> 
        {
            User u = vortex.getShardManager().getUserById(id);
            if(u==null)
                unknownIds.add(id);
            else
                args.users.add(u);
        });
        
        int fnumstrikes = numstrikes;
        
        if(unknownIds.isEmpty())
            strikeAll(args.users, numstrikes, args.reason, builder, event);
        else
            event.async(() -> 
            {
                unknownIds.forEach((id) -> 
                {
                    try
                    {
                        User u = event.getJDA().retrieveUserById(id).complete();
                        if(u==null)
                            builder.append("\n").append(event.getClient().getError()).append(" `").append(id).append("` is not a valid user ID.");
                        else
                            args.users.add(u);
                    }
                    catch(Exception ex)
                    {
                        builder.append("\n").append(event.getClient().getError()).append(" `").append(id).append("` is not a valid user ID.");
                    }
                });
                strikeAll(args.users, fnumstrikes, args.reason, builder, event);
            });
    }
    
    private void strikeAll(Set<User> users, int numstrikes, String reason, StringBuilder builder, CommandEvent event)
    {
        users.forEach(u -> 
        {
            if(u.isBot())
                builder.append("\n").append(event.getClient().getError()).append(" Strikes cannot be given to bots (").append(FormatUtil.formatFullUser(u)).append(")");
            else
            {
                vortex.getStrikeHandler().applyStrikes(event.getMember(), event.getMessage().getCreationTime(), u, numstrikes, reason);
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully gave `").append(numstrikes)
                        .append("` strikes to ").append(FormatUtil.formatUser(u));
            }
        });
        event.reply(builder.toString());
    }
}
