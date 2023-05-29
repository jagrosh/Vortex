package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;

public class TagsCmd extends SlashCommand
{
    private final Vortex vortex;

    public TagsCmd(Vortex vortex)
    {
        this.name = "tags";
        this.arguments = "";
        this.help = "lists all tags";
        this.guildOnly = true;
        this.vortex = vortex;
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
        } else {
            event.reply(getTags(event.getGuild())).queue();
        }
    }

    @Override
    public void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            return;
        }

        event.reply(getTags(event.getGuild()));
    }

    private String getTags(Guild g) {
        List<String> tags = vortex.getDatabase().tags.getTagNames(g);
        tags.sort(String::compareTo);
        if (tags.isEmpty()) {
            return "There are no tags on this server";
        }

        return FormatUtil.capitalize(FormatUtil.formatList(tags, ", "));
    }
}
