package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.api.Permission;

import java.util.List;

public class TagsCmd extends Command
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
    public void execute(CommandEvent event)
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        List<String> tags = vortex.getDatabase().tags.getTagNames(event.getGuild());

        if (tags.isEmpty())
        {
            event.reply("There are no tags on this server");
            return;
        }

        String response = tags.get(0).toUpperCase().substring(0,1) + tags.get(0).substring(1) + (tags.size() != 1 ? ", " : ".");
        for (int i = 1; i < tags.size(); i++)
        {
            response += tags.get(i) + (i == tags.size() - 1 ? "." : ", ");
        }

        event.reply(response);
    }
}
