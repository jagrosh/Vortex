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
import vortex.Action;
import vortex.ModLogger;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ModlogCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public ModlogCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.name = "modlog";
        this.category = new Category("Settings");
        this.arguments = "<#channel>";
        this.help = "sets the channel to log moderation actions";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setModlogChannel(event.getGuild(), null);
            event.replySuccess("MogLog channel has been disabled.");
            return;
        }
        String id = event.getArgs().replaceAll("<#(\\d+)>", "$1");
        TextChannel tc = event.getGuild().getTextChannelById(id);
        if(tc==null)
        {
            event.replyError("No text channel found from `"+event.getArgs()+"`");
            return;
        }
        
        if(!PermissionUtil.checkPermission(tc, event.getSelfMember(), Permission.MESSAGE_READ, Permission.MESSAGE_WRITE, Permission.MESSAGE_EMBED_LINKS))
        {
            event.replyError("I need Message Read, Message Write, and Message Embed Links permissions in "+tc.getAsMention()+"!");
            return;
        }
        
        manager.setModlogChannel(event.getGuild(), tc);
        event.replySuccess("Moderation actions will now be logged to "+tc.getAsMention());
    }
}
