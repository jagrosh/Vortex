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
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PardonCmd extends ModCommand
{
    public PardonCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "pardon";
        this.arguments = "[numstrikes] <@users> <reason>";
        this.help = "removes strikes from users";
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
        if(args.reason==null || args.reason.isEmpty())
        {
            event.replyError("Please provide a reason!");
            return;
        }
        StringBuilder builder = new StringBuilder();
        
        args.members.forEach(m -> 
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to interact with ").append(FormatUtil.formatUser(m.getUser()));
            else if(m.getUser().isBot())
                builder.append("\n").append(event.getClient().getError()).append(" Strikes cannot be taken from bots (").append(FormatUtil.formatFullUser(m.getUser())).append(")");
            else
                args.ids.add(m.getUser().getIdLong());
        });
        
        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a user ID"));
        
        args.users.forEach(u ->
        {
            if(u.isBot())
                builder.append("\n").append(event.getClient().getError()).append(" Strikes cannot be taken from bots (").append(FormatUtil.formatFullUser(u)).append(")");
            else
                args.ids.add(u.getIdLong());
        });
        
        int fnumstrikes = numstrikes;
        
        args.ids.forEach(id -> 
        {
            String user = event.getJDA().getUserById(id)==null ? "<@"+id+">" : FormatUtil.formatUser(event.getJDA().getUserById(id));
            int strikes = vortex.getDatabase().strikes.getStrikes(event.getGuild(), id);
            if(strikes==0)
                builder.append("\n").append(event.getClient().getWarning()).append(" ").append(user).append(" has no strikes.");
            else
            {
                strikes = fnumstrikes<strikes ? fnumstrikes : strikes;
                vortex.getStrikeHandler().pardonStrikes(event.getMember(), event.getMessage().getTimeCreated(), id, strikes, args.reason);
                builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully pardoned `").append(strikes).append("` strikes from ").append(user);
            }
        });
        event.reply(builder.toString());
    }
}
