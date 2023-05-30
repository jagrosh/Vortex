package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.database.Database.Modlog;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

public class ModlogsCmd extends ModCommand
{
    public ModlogsCmd(Vortex vortex)
    {
        super(vortex, Permission.MANAGE_ROLES);
        this.name = "modlogs";
        this.arguments = "<@user>";
        this.help = "shows modlogs for a user or retrieves info about a modlog";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if (event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("This command is used to see a user's modlogs for the current server. Please mention a user or use a user ID to check their modlogs.");
            return;
        }

        long id = CommandTools.getPossibleUserId(event.getArgs().trim());

        if (id == -1)
        {
            if (event.getArgs().trim().isEmpty())
            {
                id = event.getAuthor().getIdLong();
            }
            else
            {
                event.reply("Please mention someone or enter a valid user ID");
                return;
            }
        }

        List<Modlog> modlogs = Database.getAllModlogs(event.getGuild().getIdLong(), id);

        int size = modlogs.size();

        if (size == 0)
        {
            event.reply("Could not find any modlogs for that user");
            return;
        }

        EmbedBuilder[] embeds = new EmbedBuilder[size / 25 + 1];
        for (int i = 0; i < embeds.length; i++) {
            embeds[i] = new EmbedBuilder();
        }

        if (event.getJDA().getUserById(id) != null)
            embeds[0].setTitle(
                String.format("%d modlog%s found for %s#%s (%d)",
                    size,
                    size == 1 ? "" : "s",
                    event.getJDA().getUserById(id).getName(),
                    event.getJDA().getUserById(id).getDiscriminator(),
                    id
                )
            );
        else
            embeds[0].setTitle(
                String.format("%d modlog%s found for %d",
                    size,
                    size == 1 ? "" : "s",
                    id
                )
            );

        for (int i = modlogs.size() - 1; i >= 0; i--)
        {
            Modlog modlog = modlogs.get(i);
            embeds[i / 25].addField("Case: " + modlog.getId(), FormatUtil.formatModlogCase(vortex, event.getGuild(), modlog), false);
        }

        for (EmbedBuilder embed : embeds)
            event.reply(embed.build());
    }
}