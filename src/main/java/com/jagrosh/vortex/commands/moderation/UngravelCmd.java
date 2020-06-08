package com.jagrosh.vortex.commands.moderation;

import java.util.List;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import java.util.LinkedList;


public class UngravelCmd extends ModCommand
{
    public UngravelCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "ungravel";
        this.arguments = "<@users> [reason]";
        this.help = "ungravels users";
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Role gravelRole = vortex.getDatabase().settings.getSettings(event.getGuild()).getGravelRole(event.getGuild());
        if(gravelRole == null)
        {
            event.replyError("No Gravel role exists!");
            return;
        }
        if(!event.getMember().canInteract(gravelRole))
        {
            event.replyError("You do not have permission to ungravel people!");
            return;
        }
        if(!event.getSelfMember().canInteract(gravelRole))
        {
            event.reply(event.getClient().getError()+" I do not have permissions to unassign the '"+gravelRole.getName()+"' role!");
            return;
        }

        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), false, event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to ungravel (@mention or ID)!");
            return;
        }

        String reason = LogUtil.auditReasonFormat(event.getMember(), args.reason);
        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        StringBuilder builder = new StringBuilder();
        List<Member> toGravel = new LinkedList<>();

        args.members.forEach(m ->
        {
            if(!event.getMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to ungravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(!event.getSelfMember().canInteract(m))
                builder.append("\n").append(event.getClient().getError()).append(" I am unable to ungravel ").append(FormatUtil.formatUser(m.getUser()));
            else if(!m.getRoles().contains(gravelRole))
                builder.append("\n").append(event.getClient().getError()).append(" ").append(FormatUtil.formatUser(m.getUser())).append(" isn't graveled");
            else if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't ungravel ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
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

        for(int i=0; i<toGravel.size(); i++)
        {
            Member m = toGravel.get(i);
            boolean last = i+1 == toGravel.size();
            event.getGuild().getController().removeSingleRoleFromMember(m, gravelRole).reason(reason).queue(success ->
            {
                vortex.getDatabase().gravels.ungravel(event.getGuild(), m.getUser().getIdLong());
                String user = FormatUtil.formatUser(m.getUser());

                builder.append("\n").append(event.getClient().getSuccess()).append(" "+user+" was ungraveled");
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
