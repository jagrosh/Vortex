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
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.FormatUtil;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;

import com.jagrosh.vortex.utils.ToycatPallete;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class RoleinfoCmd extends SlashCommand
{
    private final static String LINESTART = "\u25AB"; // ▫
    private final static String ROLE_EMOJI = "\uD83C\uDFAD"; // 🎭
    private final Vortex vortex;
    
    public RoleinfoCmd(Vortex vortex)
    {
        this.name = "roleinfo";
        this.aliases = new String[]{"rinfo","rankinfo"};
        this.help = "shows info about a role";
        this.arguments = "<role>";
        this.guildOnly = true;
        this.vortex = vortex;
        this.options = Collections.singletonList(new OptionData(OptionType.ROLE, "role", "The role", true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        event.reply(getRoleInfoEmbed(event.getOption("role").getAsRole())).queue();

    }

    @Override
    protected void execute(CommandEvent event) 
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        Role role;
        if(event.getArgs().isEmpty())
            throw new CommandErrorException("Please provide the name of a role!");
        else
        {
            List<Role> found = FinderUtil.findRoles(event.getArgs(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the role you were looking for!");
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.listOfRoles(found, event.getArgs()));
            }
            else
            {
                event.reply(getRoleInfoEmbed(found.get(0)));
            }
        }


    }

    public MessageCreateData getRoleInfoEmbed(Role role) {
        EmbedBuilder builder = new EmbedBuilder()
                .setColor(role.getColor() == null ? ToycatPallete.DEFAULT_ROLE_WHITE : role.getColor())
                .setDescription("## Showing Info For " + role.getAsMention())
                .addField("ID", role.getId(), true)
                .addField("Color", FormatUtil.formatRoleColor(role), true)
                .addField("Created", TimeFormat.DATE_SHORT.format(role.getTimeCreated()), true)
                .addField("Hoisted", role.isHoisted() ? "Yes" : "No", true)
                .addField("Position", role.getPosition() + "/" + role.getGuild().getRoles().size(), true)
                .addField("Permissions", FormatUtil.formatRolePermissions(role), false);

        if (role.isPublicRole()) {
            builder.appendDescription("\nThis is the special @everyone role, which everyone technically has");
        }else if (Objects.equals(role, role.getGuild().getBoostRole())) {
            builder.appendDescription("\nThis is the server booster role");
        } else if (role.isManaged()) {
            builder.appendDescription("\nThis role is managed by an integration");
        }

        return MessageCreateData.fromEmbeds(builder.build());
    }
}
