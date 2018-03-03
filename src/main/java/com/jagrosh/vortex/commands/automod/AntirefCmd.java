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
import com.jagrosh.vortex.database.managers.AutomodManager;
import com.jagrosh.vortex.database.managers.PunishmentManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntirefCmd extends Command
{
    private final Vortex vortex;
    
    public AntirefCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "antireferral";
        this.guildOnly = true;
        this.aliases = new String[]{"antiref","antirefferal","antirefferral","anti-ref","anti-referral"};
        this.category = new Category("AutoMod");
        this.arguments = "<strikes>";
        this.help = "sets strikes for posting referral and malicious links";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please provide a number of strikes!");
            return;
        }
        int numstrikes;
        try
        {
            numstrikes = Integer.parseInt(event.getArgs());
        }
        catch(NumberFormatException ex)
        {
            if(event.getArgs().equalsIgnoreCase("none") || event.getArgs().equalsIgnoreCase("off"))
                numstrikes = 0;
            else
            {
                event.replyError("`"+event.getArgs()+"` is not a valid integer!");
                return;
            }
        }
        if(numstrikes<0 || numstrikes>AutomodManager.MAX_STRIKES)
        {
            event.replyError("The number of strikes must be between 0 and "+AutomodManager.MAX_STRIKES);
            return;
        }
        vortex.getDatabase().automod.setRefStrikes(event.getGuild(), numstrikes);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Users will now receive `"+numstrikes+"` strikes for posting referral links or other malicious links."+(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
