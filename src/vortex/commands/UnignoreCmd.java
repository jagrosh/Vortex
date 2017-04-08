/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.Action;
import vortex.ModLogger;
import vortex.data.DatabaseManager;
import vortex.utils.FinderUtil;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class UnignoreCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public UnignoreCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.guildOnly = true;
        this.name = "unignore";
        this.aliases = new String[]{"deignore","delignore","removeignore"};
        this.category = new Category("AutoMod");
        this.arguments = "<role or channel>";
        this.help = "sets the automod to stop ignoring a role or channel";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty())
        {
            event.replyWarning("Please include a #channel or role to stop ignoring!");
            return;
        }
        
        String id = event.getArgs().replaceAll("<#(\\d{17,20})>", "$1");
        TextChannel tc = event.getGuild().getTextChannelById(id);
        if(tc!=null)
        {
            if(manager.removeIgnore(tc))
                event.replySuccess("Automod is no longer ignoring channel <#"+tc.getId()+">");
            else
                event.replyError("Automod was not already ignoring <#"+tc.getId()+">!");
            return;
        }
        
        List<Role> roles = FinderUtil.findRole(event.getArgs(), event.getGuild());
        if(roles.isEmpty())
            event.replyError("No roles or text channels found for `"+event.getArgs()+"`");
        else if (roles.size()==1)
        {
            if(manager.removeIgnore(roles.get(0)))
                event.replySuccess("Automod is no longer ignoring role `"+roles.get(0).getName()+"`");
            else
                event.replyError("Automod was not ignoring role `"+roles.get(0).getName()+"`");
        }
        else
            event.reply(FormatUtil.listOfRoles(roles, event.getArgs()));
    }
}
