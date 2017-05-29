/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.commands;

import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import vortex.AutoMod;
import vortex.ModLogger;
import vortex.data.DMSpamManager;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntidmspamCmd extends Command {
    
    private final DatabaseManager manager;
    private final DMSpamManager dmspam;
    
    public AntidmspamCmd(DatabaseManager manager, DMSpamManager dmspam)
    {
        this.manager = manager;
        this.dmspam = dmspam;
        this.guildOnly = true;
        this.name = "antidmspam";
        this.aliases = new String[]{"antidm","block"};
        this.category = new Category("AutoMod");
        this.help = "enables/disables anti-dm-spam";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER,Permission.KICK_MEMBERS};
        this.botPermissions = new Permission[]{Permission.KICK_MEMBERS};
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            manager.setBlockDmSpam(event.getGuild(), false);
            event.replySuccess("DM-Spam blocking has been disabled.");
        }
        else if(event.getArgs().equalsIgnoreCase("on"))
        {
            manager.setBlockDmSpam(event.getGuild(), true);
            event.replySuccess("DM-Spam blocking has been enabled.");
        }
        else if(event.getArgs().equalsIgnoreCase("list"))
        {
            List<Member> members = new LinkedList<>();
            for(Member member: event.getGuild().getMembers())
                if(member.getRoles().isEmpty() && !member.isOwner() && !dmspam.getDMSpamServers(member.getUser()).isEmpty())
                    members.add(member);
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = new EmbedBuilder().setColor(event.getGuild().getSelfMember().getColor());
            for(Member member: members)
            {
                if(sb.length()>3970)
                {
                    event.reply(eb.setDescription(sb.toString().trim()).build());
                    sb = new StringBuilder();
                }
                sb.append(" ").append(member.getUser().getAsMention());
            }
            event.reply(eb.setDescription(sb.toString().trim()).build());
        }
        else if(event.getArgs().equalsIgnoreCase("listall"))
        {
            List<Member> members = new LinkedList<>();
            for(Member member: event.getGuild().getMembers())
                if(!dmspam.getDMSpamServers(member.getUser()).isEmpty())
                    members.add(member);
            StringBuilder sb = new StringBuilder();
            EmbedBuilder eb = new EmbedBuilder().setColor(event.getGuild().getSelfMember().getColor());
            for(Member member: members)
            {
                if(sb.length()>3970)
                {
                    event.reply(eb.setDescription(sb.toString().trim()).build());
                    sb = new StringBuilder();
                }
                sb.append(" ").append(member.getUser().getAsMention());
            }
            event.reply(eb.setDescription(sb.toString().trim()).build());
        }
        else if(event.getArgs().equalsIgnoreCase("remove"))
        {
            int count = 0;
            for(Member member: event.getGuild().getMembers())
                if(member.getRoles().isEmpty() && !member.isOwner())
                {
                    List<Guild> spams = dmspam.getDMSpamServers(member.getUser());
                    String msg = "You have been kicked from **"+event.getGuild().getName()+"** for being in the following server(s):\n";
                    String msg2 = "\n\nThese servers are known to be the origin of large amounts of direct-message spam and advertisements, "
                            + "and therefore all members are prevented from joining. If you would like to rejoin **"+event.getGuild().getName()
                            + "**, please leave these servers.";
                    if(!spams.isEmpty())
                    {
                        count++;
                        String str = "";
                        for(Guild g: spams)
                            str+="\n  **"+g.getName()+"**";
                        String fmsg = msg+str+msg2;
                        try {
                            member.getUser().openPrivateChannel().queue(pc -> pc.sendMessage(fmsg).queue(
                                m -> event.getGuild().getController().kick(member, "Being in a DM-spam server").queue(), 
                                v -> event.getGuild().getController().kick(member, "Being in a DM-spam server").queue()), 
                                v -> event.getGuild().getController().kick(member, "Being in a DM-spam server").queue());
                        } catch(Exception e) {
                            event.getGuild().getController().kick(member, "Being in a DM-spam server").queue();
                        }
                    }
                }
            event.replySuccess("Kicking "+count+" members (without roles) that are in DM-spam servers.");
        }
        else
        {
            event.replyError("Valid values are:\n"
                    + "`OFF` - disables DM-Spam blocking\n"
                    + "`ON` - enables DM-Spam blocking\n"
                    + "`REMOVE` - removes any users in the server without roles that is in a DM-spam server\n"
                    //+ "`REMOVEALL` - removes any users in the server, even with roles (except those with elevated permissions), that is in a DM-spam server\n"
                    + "`LIST` - shows what users would be removed via REMOVE\n"
                    + "`LISTALL` - shows users in the server that are in a DM-spam server"
            );
        }
    }
}
