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
package com.jagrosh.vortex.commands.general;

import java.time.format.DateTimeFormatter;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class RoleinfoCmd extends Command
{
    private final String linestart = "\u25AB";
    
    public RoleinfoCmd()
    {
        this.name = "roleinfo";
        this.aliases = new String[]{"rinfo","rankinfo"};
        this.help = "shows info about a role";
        this.arguments = "<role>";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        Role role;
        if(event.getArgs().isEmpty())
            throw new CommandErrorException("Please provide the name of a role!");
        else
        {
            List<Role> found = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the role you were looking for!");
                return;
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.listOfRoles(found, event.getArgs()));
                return;
            }
            else
            {
                role = found.get(0);
            }
        }
        
        String title = "\uD83C\uDFAD Information about **"+role.getName()+"**:";
        List<Member> list = role.isPublicRole() ? event.getGuild().getMembers() : event.getGuild().getMembersWithRoles(role);
        StringBuilder desr = new StringBuilder(linestart+"ID: **"+role.getId()+"**\n"
                + linestart+"Creation: **"+role.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
                + linestart+"Position: **"+role.getPosition()+"**\n"
                + linestart+"Color: **#"+(role.getColor()==null ? "000000" : Integer.toHexString(role.getColor().getRGB()).toUpperCase().substring(2))+"**\n"
                + linestart+"Mentionable: **"+role.isMentionable()+"**\n"
                + linestart+"Hoisted: **"+role.isHoisted()+"**\n"
                + linestart+"Managed: **"+role.isManaged()+"**\n"
                + linestart+"Permissions: ");
        if(role.getPermissions().isEmpty())
            desr.append("None");
        else
            desr.append(role.getPermissions().stream().map(p -> "`, `"+p.getName()).reduce("", String::concat).substring(3)).append("`");
        desr.append("\n").append(linestart).append("Members: **").append(list.size()).append("**\n");
        if(list.size()*24<=2048-desr.length())
            list.forEach(m -> desr.append("<@").append(m.getUser().getId()).append("> "));
        
        event.reply(new MessageBuilder()
                .append(FormatUtil.filterEveryone(title))
                .setEmbed(new EmbedBuilder()
                        .setDescription(desr.toString().trim())
                        .setColor(role.getColor()).build())
                .build());
    }
}
