
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.core.Permission;

import java.time.temporal.ChronoUnit;


public class PingCmd extends Command {
    private final Vortex vortex;

    public PingCmd(Vortex vortex) {
        this.name = "ping";
        this.help = "checks the bot's latency";
        this.guildOnly = false;
        this.aliases = new String[]{"pong"};
        this.vortex = vortex;
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        event.reply("Ping: ...", (m) -> {
            long ping = event.getMessage().getCreationTime().until(m.getCreationTime(), ChronoUnit.MILLIS);
            m.editMessage("Ping: " + ping + "ms | Websocket: " + event.getJDA().getPing() + "ms").queue();
        });
    }
}
