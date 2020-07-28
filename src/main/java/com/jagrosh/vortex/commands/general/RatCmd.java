
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.core.Permission;

import java.time.temporal.ChronoUnit;
import java.util.Random;

public class RatCmd extends Command {
    private final Vortex vortex;

    public RatCmd(Vortex vortex) {
        this.name = "rat";
        this.help = "rat poggers";
        this.guildOnly = false;
        this.aliases = new String[]{"ratImage"};
        this.vortex = vortex;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] rats = OtherUtil.readLines("rats");
        Random rand = new Random();
        int rnd = rand.nextInt(rats.length);
        event.reply(rats[rnd]);
    }
}
