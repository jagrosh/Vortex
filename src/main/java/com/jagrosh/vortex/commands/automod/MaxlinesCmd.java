/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PunishmentManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MaxlinesCmd extends Command
{
    private final Vortex vortex;
    
    public MaxlinesCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "maxlines";
        this.guildOnly = true;
        this.aliases = new String[]{"maxnewlines"};
        this.category = new Category("AutoMod");
        this.arguments = "<maximum | OFF>";
        this.help = "sets maximum lines allowed per message";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please provide a maximum number of newlines allowed!");
            return;
        }
        int maxlines;
        try
        {
            maxlines = Integer.parseInt(event.getArgs());
        }
        catch(NumberFormatException ex)
        {
            if(event.getArgs().equalsIgnoreCase("none") || event.getArgs().equalsIgnoreCase("off"))
                maxlines = 0;
            else
            {
                event.replyError("`"+event.getArgs()+"` is not a valid integer!");
                return;
            }
        }
        if(maxlines<0)
        {
            event.replyError("The maximum number of lines must be a positive integer!");
            return;
        }
        vortex.getDatabase().automod.setMaxLines(event.getGuild(), maxlines);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Messages longer than `"+maxlines+"` lines will now be automatically deleted, "
                + "and users will receive strikes for every additional multiple of up to `"+maxlines+"` lines."+(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
