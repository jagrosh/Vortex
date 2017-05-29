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
public class AntimentionCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public AntimentionCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.guildOnly = true;
        this.name = "antimention";
        this.category = new Category("AutoMod");
        this.arguments = "<maximum or OFF>";
        this.help = "sets maximum number of unique, non-bot mentions a user can send";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER,Permission.BAN_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setMaxMentions(event.getGuild(), (short)0);
            event.replySuccess("Anti-Mention has been disabled.");
            return;
        }
        try {
            short num = Short.parseShort(event.getArgs());
            if(num<AutoMod.MENTION_MINIMUM)
            {
                event.replyError("Maximum must be higher than "+AutoMod.MENTION_MINIMUM);
                return;
            }
            manager.setMaxMentions(event.getGuild(), num);
            event.replySuccess("Set automod to ban users for mentioning at least **"+num+"** users.");
        }catch(NumberFormatException e)
        {
            event.replyError("Maximum must be a valid integer higher than "+AutoMod.MENTION_MINIMUM);
        }
    }
}
