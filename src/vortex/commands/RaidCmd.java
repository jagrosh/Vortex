/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.AutoMod;
import vortex.ModLogger;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RaidCmd extends Command {
    
    private final AutoMod automod;
    public RaidCmd(AutoMod automod)
    {
        this.automod = automod;
        this.name = "raidmode";
        this.aliases = new String[]{"raid"};
        this.category = new Category("Moderation");
        this.arguments = "[ON|OFF]";
        this.help = "views, enables, or disabled raidmode";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER, Permission.KICK_MEMBERS};
        this.botPermissions = new Permission[]{Permission.KICK_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            if(automod.endRaidMode(event.getGuild()))
                event.replySuccess("RaidMode has been disabled.");
            else
                event.replyError("RaidMode is not currently enabled!");
        }
        else if (event.getArgs().equalsIgnoreCase("on"))
        {
            if(automod.startRaidMode(event.getGuild(), event.getMessage()))
                event.replySuccess("RaidMode enabled. New members will be prevented from joining.");
            else
                event.replyError("RaidMode is already enabled!");
        }
        else
        {
            event.replySuccess("RaidMode is currently `"+(automod.isRaidModeEnabled(event.getGuild()) ? "ACTIVE" : "NOT ACTIVE")+"`");
        }
    }
}
