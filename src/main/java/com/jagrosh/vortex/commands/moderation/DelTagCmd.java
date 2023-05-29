package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.api.Permission;

public class DelTagCmd extends ModCommand
{
    public DelTagCmd(Vortex vortex)
    {
        super(vortex, Permission.MESSAGE_MANAGE);
        this.name = "deltag";
        this.arguments = "<tagName>";
        this.help = "deletes a tag";
        this.guildOnly = true;
    }

    @Override
    public void execute(CommandEvent event)
    {
        String tagName;
        try {
            tagName = event.getArgs().trim().toLowerCase().split(" ")[0];
        } catch (IndexOutOfBoundsException e) {
            event.reply("Please enter a tag to delete");
            return;
        }

        if (vortex.getDatabase().tags.deleteTag(event.getGuild(), tagName))
            event.reply("Successfully deleted the `"+tagName+"` tag");
        else
            event.reply("Oops! The tag `"+tagName+"` could not be found.");
    }
}
