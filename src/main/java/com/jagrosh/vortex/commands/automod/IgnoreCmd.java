/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class IgnoreCmd extends Command
{
    private final Vortex vortex;
    
    public IgnoreCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.guildOnly = true;
        this.name = "ignore";
        this.aliases = new String[]{"addignore","ignored","ignores"};
        this.category = new Category("AutoMod");
        this.arguments = "<role | channel>";
        this.help = "shows ignores, or sets automod to ignore a role or channel";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event) 
    {
        if(event.getArgs().isEmpty())
        {
            EmbedBuilder ebuilder = new EmbedBuilder();
            ebuilder.setColor(event.getSelfMember().getColor());
            ebuilder.setTitle("Automod Ignores",null);
            StringBuilder builder = new StringBuilder();
            List<Role> roles = vortex.getDatabase().ignores.getIgnoredRoles(event.getGuild());
            List<TextChannel> channels = vortex.getDatabase().ignores.getIgnoredChannels(event.getGuild());
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
            ebuilder.setDescription(builder.length() > 2045 ? builder.substring(0, 2048) + "..." : builder.toString());
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
            vortex.getDatabase().ignores.ignore(tc);
            event.replySuccess("Automod is now ignoring channel <#"+tc.getId()+">");
            return;
        }
        
        List<Role> roles = FinderUtil.findRoles(event.getArgs(), event.getGuild());
        if(roles.isEmpty())
            event.replyError(FormatUtil.filterEveryone("No roles or text channels found for `"+event.getArgs()+"`"));
        else if (roles.size()==1)
        {
            vortex.getDatabase().ignores.ignore(roles.get(0));
            event.replySuccess(FormatUtil.filterEveryone("Automod is now ignoring role `"+roles.get(0).getName()+"`"));
        }
        else
            event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfRoles(roles, event.getArgs())));
    }
}
