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
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandWarningException;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.List;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AnnounceCmd extends Command
{
    private final static String FORMAT = "Please include a channel and role name, a | as a separator, and a message to send!\n"
            + "Please see the full command reference for examples - <"+Constants.Wiki.COMMANDS+"#-tools-commands>";
    
    public AnnounceCmd()
    {
        this.name = "announce";
        this.aliases = new String[]{"announcement", "announcer"};
        this.arguments = "<#channel> <rolename> | <message>";
        this.help = "pings a role with an announcement in the specified channel";
        this.category = new Category("Tools");
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
        this.cooldown = 3;
        this.cooldownScope = CooldownScope.GUILD;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
            throw new CommandErrorException(FORMAT);
        String[] parts = event.getArgs().split("\\s+", 2);
        List<TextChannel> list = FinderUtil.findTextChannels(parts[0], event.getGuild());
        if(list.isEmpty())
            throw new CommandErrorException("I couldn't find any text channel called `"+parts[0]+"`.");
        if(list.size()>1)
            throw new CommandWarningException(FormatUtil.listOfText(list, parts[0]));
        TextChannel tc = list.get(0);
        if(!tc.canTalk())
            throw new CommandWarningException("I do not have permission to Send Messages in "+tc.getAsMention()+"!");
        if(!tc.canTalk(event.getMember()))
            throw new CommandErrorException("You do not have permission to Send Messages in "+tc.getAsMention()+"!");
        if(parts.length<2)
            throw new CommandErrorException(FORMAT);
        String[] parts2 = parts[1].split("\\s*\\|\\s*", 2);
        List<Role> rlist = FinderUtil.findRoles(parts2[0], event.getGuild());
        if(rlist.isEmpty())
            throw new CommandErrorException("I couldn't find any role called `"+parts2[0]+"`.");
        if(rlist.size()>1)
            throw new CommandWarningException(FormatUtil.listOfRoles(rlist, parts2[0]));
        Role role = rlist.get(0);
        if(!event.getSelfMember().canInteract(role))
            throw new CommandWarningException("I cannot modify the role `"+role.getName()+"`. Try moving my highest role above the `"+role.getName()+"` role.");
        if(!event.getMember().canInteract(role))
            throw new CommandErrorException("You cannot modify the `"+role.getName()+"` role.");
        if(parts2.length<2 || parts2[1].isEmpty())
            throw new CommandErrorException(FORMAT);
        
        String message = role.getAsMention()+": "+parts2[1];
        if(!event.getMember().hasPermission(tc, Permission.MESSAGE_MENTION_EVERYONE))
            message = FormatUtil.filterEveryone(message);
        if(message.length() > 2000)
            message = message.substring(0, 2000);
        String fmessage = message;
        if(!role.isMentionable())
        {
            String reason = "Announcement by "+event.getAuthor().getName()+"#"+event.getAuthor().getDiscriminator();
            role.getManager().setMentionable(true).reason(reason).queue(s -> 
            {
                tc.sendMessage(fmessage).queue(m -> 
                {
                    event.replySuccess("Announcement for `"+role.getName()+"` sent to "+tc.getAsMention()+"!");
                    role.getManager().setMentionable(false).reason(reason).queue(s2->{}, f2->{});
                }, f -> 
                {
                    event.replyError("Failed to send message.");
                    role.getManager().setMentionable(false).reason(reason).queue(s2->{}, f2->{});
                });
            }, f -> event.replyError("Failed to modify the role `"+role.getName()+"`."));
        }
        else
        {
            tc.sendMessage(fmessage).queue(
                    m -> event.replySuccess("Announcement for `"+role.getName()+"` sent to "+tc.getAsMention()+"!"), 
                    f -> event.replyError("Failed to send message."));
        }
    }
}
