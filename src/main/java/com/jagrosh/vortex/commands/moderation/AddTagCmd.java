package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;

public class AddTagCmd extends ModCommand
{
    public AddTagCmd(Vortex vortex)
    {
        super(vortex, Permission.MESSAGE_MANAGE);
        this.name = "addtag";
        this.arguments = "<tagName> [tagValue]";
        this.help = "adds a tag";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String args = event.getArgs().trim();
        String[] argsArray = args.split(" ");
        String tagName;
        String tagValue;

        try
        {
            tagName = argsArray[0];
        }
        catch(IndexOutOfBoundsException e)
        {
            event.reply("Please enter a tag name to create");
            return;
        }

        tagValue = args.substring(argsArray[0].length() + 1);
        if (tagValue.isEmpty())
        {
            event.reply("Please enter a value for the tag");
            return;
        }

        vortex.getDatabase().tags.addTagValue(event.getGuild(), tagName, tagValue);
        event.reply("Successfully created the `" + tagName + "` tag!");
    }
}
