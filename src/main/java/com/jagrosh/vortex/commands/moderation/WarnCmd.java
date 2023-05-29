package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.ArgsUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

import java.util.LinkedList;
import java.util.List;

public class WarnCmd extends ModCommand
{
    public WarnCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "warn";
        this.aliases = new String[] {"warning"};
        this.arguments = "<@users> [reason]";
        this.help = "warns users";
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        ArgsUtil.ResolvedArgs args = ArgsUtil.resolve(event.getArgs(), event.getGuild());
        if(args.isEmpty())
        {
            event.replyError("Please include at least one user to warn (@mention or ID)!");
            return;
        }

        Role modrole = vortex.getDatabase().settings.getSettings(event.getGuild()).getModeratorRole(event.getGuild());
        StringBuilder builder = new StringBuilder();
        List<Member> toWarn = new LinkedList<>();

        args.members.forEach(m ->
        {
            if(modrole!=null && m.getRoles().contains(modrole))
                builder.append("\n").append(event.getClient().getError()).append(" I won't warn ").append(FormatUtil.formatUser(m.getUser())).append(" because they have the Moderator Role");
            else
                toWarn.add(m);
        });

        args.users.forEach(u -> builder.append("\n").append(event.getClient().getWarning()).append(" The user ").append(u.getAsMention()).append(" is not in this server."));
        args.ids.forEach(id -> builder.append("\n").append(event.getClient().getWarning()).append(" The user <@").append(id).append("> is not in this server."));

        for (Member m : toWarn)
        {
            vortex.getDatabase().warnings.logCase(vortex, event.getGuild(), event.getAuthor().getIdLong(), m.getUser().getIdLong(), args.reason);
            String user = FormatUtil.formatUser(m.getUser());
            builder.append("\n").append(event.getClient().getSuccess()).append(" ").append(user).append(" was warned");
        }

        event.reply(builder.toString());
    }
}