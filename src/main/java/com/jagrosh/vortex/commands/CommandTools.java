package com.jagrosh.vortex.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Role;

import java.util.Locale;

public class CommandTools
{
    private CommandTools(){}

    /**
     * Checks if the user has permission to use tags. People may use tags if they meet any of the following criteria:
     *             - Has the manage messages permission
     *             - Is an RTC
     *             - Has any perm in perms
     * @param vortex The vortex object
     * @param event The event
     * @param perms Optional perms that someone can also have
     * @return Returns true if the person has perms to use a general command, false if not
     */
    public static boolean hasGeneralCommandPerms(Vortex vortex, CommandEvent event, Permission... perms)
    {
        Role rtcRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getRtcRole(event.getGuild());
        Role modRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());

        if (modRole != null && event.getMember().getRoles().contains(modRole))
            return true;
        if (rtcRole != null && event.getMember().getRoles().contains(rtcRole))
            return true;
        for (Permission perm: perms)
            if (event.getMember().hasPermission(event.getTextChannel(), perm))
                return true;

        return false;
    }

    /**
     * Checks if a user provided string is to enable or disable something
     * @return true if said something is to be enabled, false if its to be disabled
     * @throws IllegalArgumentException If it could not be properly parsed
     */
    public static boolean parseEnabledDisabled(String str) throws IllegalArgumentException {
        switch (str.toLowerCase().trim()) {
            case "enable":
            case "enabled":
            case "on":
                return true;
            case "disable":
            case "disabled":
            case "off":
                return false;
            default:
                throw new IllegalArgumentException("Could not parse string '" + str + "'");
        }
    }
}
