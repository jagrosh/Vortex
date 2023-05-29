
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;

import java.time.Instant;
import java.time.temporal.ChronoUnit;


public class PingCmd extends SlashCommand {
    private final Vortex vortex;

    public PingCmd(Vortex vortex) {
        this.name = "ping";
        this.help = "checks the bot's latency";
        this.guildOnly = false;
        this.aliases = new String[]{"pong"};
        this.vortex = vortex;
    }

    @Override
    protected void execute(SlashCommandEvent slashCommandEvent) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, slashCommandEvent, Permission.MESSAGE_MANAGE)) {
            return;
        }


        long time0 = System.currentTimeMillis();
        slashCommandEvent.reply("Ping: ...").queue(hook -> {
            hook.editOriginal(calculatePing(slashCommandEvent.getJDA(), time0)).queue();
        });
    }

    @Override
    protected void execute(CommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        long time0 = System.currentTimeMillis();
        event.reply("Ping: ...", (m) -> {
            m.editMessage(calculatePing(event.getJDA(), time0)).queue();
        });
    }

    private String calculatePing(JDA jda, long time0) {
        return "Ping: " + (System.currentTimeMillis() - time0) + "ms | Websocket: " + jda.getGatewayPing() + "ms";
    }
}
