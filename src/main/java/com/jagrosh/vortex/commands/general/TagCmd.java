package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.core.Permission;

public class TagCmd extends Command
{
    private final Vortex vortex;

    public TagCmd(Vortex vortex) {
        this.vortex = vortex;
        this.name = "tag";
        this.arguments = "<tagName>";
        this.help = "displays a tag";
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        String tagName = event.getArgs().trim().split(" ")[0];
        String tagValue = vortex.getDatabase().tags.getTagValue(event.getGuild(), tagName);

        if (tagValue == null)
            return;

        event.reply(tagValue);
    }
}
