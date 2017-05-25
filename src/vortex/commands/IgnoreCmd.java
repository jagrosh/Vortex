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
import vortex.Action;
import vortex.ModLogger;
import vortex.data.DatabaseManager;
import vortex.utils.FinderUtil;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class IgnoreCmd extends Command {
    
    private final DatabaseManager manager;
    private final ModLogger modlog;
    
    public IgnoreCmd(DatabaseManager manager, ModLogger modlog)
    {
        this.manager = manager;
        this.modlog = modlog;
        this.guildOnly = true;
        this.name = "ignore";
        this.aliases = new String[]{"addignore","ignored","ignores"};
        this.category = new Category("AutoMod");
        this.arguments = "<role or channel>";
        this.help = "shows ignores, or sets the automod to ignore a role or channel";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty())
        {
            EmbedBuilder ebuilder = new EmbedBuilder();
            ebuilder.setColor(event.getSelfMember().getColor());
            ebuilder.setTitle("Automod Ignores",null);
            StringBuilder builder = new StringBuilder();
            Set<Role> roles = manager.getIgnoredRoles(event.getGuild());
            Set<TextChannel> channels = manager.getIgnoredChannels(event.getGuild());
            event.getGuild().getRoles().stream().forEach(r -> {
                if(roles.contains(r))
                    builder.append("\n").append(r.getAsMention());
                else if(!event.getSelfMember().canInteract(r))
                    builder.append("\n").append(r.getAsMention()).append(" [can't interact]");
                else if(r.getPermissions().contains(Permission.ADMINISTRATOR) 
                        || r.getPermissions().contains(Permission.MANAGE_SERVER) 
                        || r.getPermissions().contains(Permission.BAN_MEMBERS)
                        || r.getPermissions().contains(Permission.KICK_MEMBERS)
                        || r.getPermissions().contains(Permission.MESSAGE_MANAGE))
                    builder.append("\n").append(r.getAsMention()).append(" [elevated perms]");
            });
            channels.forEach(c -> builder.append("\n").append(c.getAsMention()));
            ebuilder.setDescription(builder.toString());
            event.reply(ebuilder.build());
            return;
        }
        
        String id = event.getArgs().replaceAll("<#(\\d{17,20})>", "$1");
        TextChannel tc;
        try {
            tc = event.getGuild().getTextChannelById(id);
        } catch(Exception e) {
            tc = null;
        }
        if(tc!=null)
        {
            manager.addIgnore(tc);
            event.replySuccess("Automod is now ignoring channel <#"+tc.getId()+">");
            return;
        }
        
        List<Role> roles = FinderUtil.findRole(event.getArgs(), event.getGuild());
        if(roles.isEmpty())
            event.replyError("No roles or text channels found for `"+event.getArgs()+"`");
        else if (roles.size()==1)
        {
            manager.addIgnore(roles.get(0));
            event.replySuccess("Automod is now ignoring role `"+roles.get(0).getName()+"`");
        }
        else
            event.reply(FormatUtil.listOfRoles(roles, event.getArgs()));
    }
}
