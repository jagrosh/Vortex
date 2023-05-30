package com.jagrosh.vortex.commands.moderation;

import java.util.List;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedList;


public class GravelCmd extends ModCommand
{
    public GravelCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "gravel";
        this.arguments = "<@users> [time] [reason]";
        this.help = "applies graveled role to users";
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Role gravelRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getGravelRole(event.getGuild());
        if(gravelRole == null)
        {
            event.replyError("No Graveled role exists!");
            return;
        }
        if(!event.getMember().canInteract(gravelRole))
        {
            event.replyError("You do not have permissions to assign the '"+gravelRole.getName()+"' role!");
            return;
        }
        if(!event.getSelfMember().canInteract(gravelRole))
        {
            event.reply(event.getClient().getError()+" I do not have permissions to assign the '"+gravelRole.getName()+"' role!");
            return;
        }

        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), true, event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to gravel (@mention or ID)!");
            return;
        }
        int minutes;
        if(args.time < -1) {
            System.out.println(args.time);
            throw new CommandErrorException("Timed gravels cannot be negative time!");
        }else if(args.time == 0) {
            minutes = 0;
        }else if(args.time > 60) {
            minutes = (int) Math.round(args.time / 60.0);
        }else {
            minutes = 0;
        }
        String reason = LogUtil.auditReasonFormat(event.getMember(), minutes, args.reason);
        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        StringBuilder builder = new StringBuilder();
        List<Member> toGravel = new LinkedList<>();

        args.members.forEach(m ->
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to gravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to gravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(m.getRoles().contains(gravelRole))
                builder.append("\n").append(event.getClient().getError()).append(" ").append(FormatUtil.formatUser(m.getUser())).append(" is already graveled!");
            else if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't gravel ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
            else
                toGravel.add(m);
        });

        args.unresolved.forEach(un -> builder.append("\n").append(event.getClient().getWarning()).append(" Could not resolve `").append(un).append("` to a member"));

        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append(" The user ").append(u.getAsMention()).append(" is not in this server."));

        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append(" The user <@").append(id).append("> is not in this server."));

        if(toGravel.isEmpty())
        {
            event.reply(builder.toString());
            return;
        }

        if(toGravel.size() > 5)
            event.reactSuccess();

        Instant ungravelTime = Instant.now().plus(minutes, ChronoUnit.MINUTES);
        String time = minutes==0 ? "" : " for "+FormatUtil.secondsToTimeCompact(minutes*60);
        for(int i=0; i<toGravel.size(); i++)
        {
            Member m = toGravel.get(i);
            boolean last = i+1 == toGravel.size();
            String user = FormatUtil.formatUser(m.getUser());
            String[] messages = {
                    " "+user+" was banished to the gravel pit",
                    " "+user+" was graveled",
                    " "+user+" was sent to find flint",
                    " Added gravel to " + user,
                    " "+user+" fell in a pit of gravel",
                    " Successfully poured some gravel on "+user
            };
            event.getGuild().addRoleToMember(m, gravelRole).reason(reason).queue(success ->
            {
                builder.append("\n").append(event.getClient().getSuccess()).append(messages[(int) (Math.random()*messages.length+0.5)]);
                if(minutes>0)
                    vortex.getDatabase().gravels.overrideGravel(event.getGuild(), m.getUser().getIdLong(), event.getAuthor().getIdLong(), ungravelTime, args.reason);
                else
                    vortex.getDatabase().gravels.overrideGravel(event.getGuild(), m.getUser().getIdLong(), event.getAuthor().getIdLong(), Instant.MAX, args.reason);
                if(last)
                    event.reply(builder.toString());
            }, failure ->
            {
                builder.append("\n").append(event.getClient().getError()).append(" Failed to gravel ").append(m.getUser().getAsMention());
                if(last)
                    event.reply(builder.toString());
            });
        }
    }
}
