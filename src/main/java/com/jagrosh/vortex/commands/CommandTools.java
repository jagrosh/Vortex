package com.jagrosh.vortex.commands;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;

import java.security.Permissions;
import java.util.regex.Pattern;

public class CommandTools
{
    public static final String COMMAND_NO_PERMS = "You don't have the perms to do that!";
    public static final String MOD_COMMAND_NO_PERMS = "You can't do that, silly!";
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
        return hasGeneralCommandPerms(vortex, event.getMember(), (event.getGuildChannel()), perms);
    }

    public static boolean hasGeneralCommandPerms(Vortex vortex, SlashCommandEvent event, Permission... perms)
    {
        return hasGeneralCommandPerms(vortex, event.getMember(),  event.getGuildChannel(), perms);
    }

    public static boolean hasGeneralCommandPerms(Vortex vortex, Member member, GuildChannel channel, Permission... perms)
    {
        if (member == null) {
            return true; // Imples this is from DM
        }

        Guild g = member.getGuild();
        Role rtcRole = vortex.getDatabase().settings.getSettings(g).getRtcRole(g);
        Role modRole = vortex.getDatabase().settings.getSettings(g).getModeratorRole(g);

        if (modRole != null && member.getRoles().contains(modRole))
            return true;
        if (rtcRole != null && member.getRoles().contains(rtcRole))
            return true;
        for (Permission perm: perms)
            if (member.hasPermission(channel, perm))
                return true;

        return false;
    }

    /**
     * Extracts a possible user ID from an argument of a command. This number may or may not be a valid ID.
     * @param str The argument
     * @return The possible user ID, -1 if no ID was found
     */
    public static long getPossibleUserId(String str)
    {
        if (str.matches("<@!?\\d+>"))
            str = str.replaceAll("\\D", "");

        try
        {
            return Long.parseLong(str);
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }
}