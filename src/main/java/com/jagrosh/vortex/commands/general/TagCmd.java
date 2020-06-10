package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;

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

        String tagName = event.getArgs().trim().split(" ")[0];
        String tagValue = vortex.getDatabase().tags.getTagValue(event.getGuild(), tagName);

        if (tagValue == null)
            return;

        event.reply(tagValue);
    }
}
