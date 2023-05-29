package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;

import java.util.Collections;

public class TagCmd extends SlashCommand
{
    private final Vortex vortex;

    public TagCmd(Vortex vortex) {
        this.vortex = vortex;
        this.name = "tag";
        this.arguments = "<tagName>";
        this.help = "displays a tag";
        this.options = Collections.singletonList(new OptionData(OptionType.STRING, "name", "the tags name", true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        String tagName = event.getOption("name").getAsString();
        String tagValue = getTag(event.getGuild(), tagName);
        if (tagValue != null) {
            event.reply(tagValue).queue();
        } else {
            String errMessage = tagName.trim().isEmpty() ? "Please enter a valid tag name" : "Tag " + tagName + " not found";
            event.reply(errMessage).setEphemeral(true).queue();
        }
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        String tagValue = getTag(event.getGuild(), event.getArgs().trim().split(" ")[0]);
        if (tagValue != null) {
            event.reply(tagValue);
        }
    }

    private String getTag(Guild guild, String tagName) {
        return vortex.getDatabase().tags.getTagValue(guild, tagName.trim());
    }
}
