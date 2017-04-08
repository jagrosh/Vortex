/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import vortex.Action;
import vortex.ModLogger;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntispamCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public AntispamCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.name = "antispam";
        this.guildOnly = true;
        this.category = new Category("AutoMod");
        this.arguments = "<action> <number>";
        this.help = "sets the action to perform at the specified number of repeated messages";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setSpam(event.getGuild(), Action.NONE, (short)0);
            event.replySuccess("Anti-Spam has been disabled.");
            return;
        }
        String[] parts = event.getArgs().split("\\s+", 2);
        if(parts.length<2)
        {
            event.replyError("Please include an action _and_ a number of messages to act on!");
            return;
        }
        Action act = Action.of(parts[0]);
        if(act==Action.NONE)
        {
            event.replyError("`"+parts[0]+"` is not a valid action! Valid: `BAN` `KICK` `MUTE` `WARN` `DELETE`");
            return;
        }
        short num = 0;
        try {
            num = Short.parseShort(parts[1]);
        } catch(NumberFormatException e)
        {}
        if(num<4 || num>10)
        {
            event.replyError("`"+parts[1]+"` is not a valid integer between 4 and 10!");
            return;
        }
        manager.setSpam(event.getGuild(), act, num);
        event.replySuccess("Set automod to **"+act.name()+"** on `"+num+"` duplicated messages!");
    }
}
