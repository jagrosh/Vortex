/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import vortex.AutoMod;
import vortex.ModLogger;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AutoraidmodeCmd extends Command {
    
    private final DatabaseManager manager;
    
    public AutoraidmodeCmd(DatabaseManager manager)
    {
        this.manager = manager;
        this.guildOnly = true;
        this.name = "autoraidmode";
        this.aliases = new String[]{"autoraid"};
        this.category = new Category("AutoMod");
        this.arguments = "<ON or OFF>";
        this.help = "enables or disabled auto-raidmode";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER,Permission.KICK_MEMBERS};
        this.botPermissions = new Permission[]{Permission.MANAGE_SERVER,Permission.KICK_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setAutoRaidmode(event.getGuild(), false);
            event.replySuccess("Auto-Raidmode has been disabled.");
        }
        else if(event.getArgs().equalsIgnoreCase("on"))
        {
            manager.setAutoRaidmode(event.getGuild(), true);
            event.replySuccess("Auto-Raidmode has been enabled.");
        }
        else
        {
            event.replyError("Valid values are `OFF` and `ON`.");
        }
    }
}
