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
public class AntiinviteCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public AntiinviteCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.name = "antiinvite";
        this.guildOnly = true;
        this.aliases = new String[]{"antinvite"};
        this.category = new Category("AutoMod");
        this.arguments = "<action or OFF>";
        this.help = "sets the action to perform (after a warning) for posting an invite";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setInviteAction(event.getGuild(), Action.NONE);
            event.replySuccess("Anti-Invite has been disabled.");
            return;
        }
        Action act = Action.of(event.getArgs());
        if(act==Action.NONE)
        {
            event.replyError("`"+event.getArgs()+"` is not a valid action! Valid: `BAN` `KICK` `MUTE` `WARN` `DELETE`");
            return;
        }
        manager.setInviteAction(event.getGuild(), act);
        event.replySuccess("Set automod to **"+act.name()+"** after a user is warned for posting an invite.");
    }
}
