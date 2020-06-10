package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

import java.util.List;

public class ListTagsCmd extends Command
{
    private final Vortex vortex;

    public ListTagsCmd(Vortex vortex)
    {
        this.name = "listtags";
        this.arguments = "";
        this.help = "lists all tags";
        this.guildOnly = true;
        this.vortex = vortex;
    }

    @Override
    public void execute(CommandEvent event)
    {
        Role rtcRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getRtcRole(event.getGuild());
        Role modRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());

        /* Checks if the user has permission to use tags. People may use tags if they meet any of the following criteria:
            - Has the manage messages permission
            - Has the predesignated moderator role
            - Is an RTC
         */
        if (
                !event.getMember().hasPermission(event.getTextChannel(), Permission.MESSAGE_MANAGE) &&
                        !(modRole != null && event.getMember().getRoles().contains(modRole)) &&
                        !(rtcRole != null && event.getMember().getRoles().contains(rtcRole))
        )
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
