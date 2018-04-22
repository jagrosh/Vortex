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
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.database.managers.PunishmentManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AutodehoistCmd extends Command
{
    private final Vortex vortex;
    
    public AutodehoistCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "autodehoist";
        this.guildOnly = true;
        this.aliases = new String[]{"auto-dehoist"};
        this.category = new Category("AutoMod");
        this.arguments = "<character | OFF>";
        this.help = "prevents name-hoisting via usernames or nicknames";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
            throw new CommandExceptionListener.CommandErrorException("Please provide a valid dehoist character, or OFF");
        else if(event.getArgs().equalsIgnoreCase("none") || event.getArgs().equalsIgnoreCase("off"))
        {
            vortex.getDatabase().automod.setDehoistChar(event.getGuild(), (char)0);
            event.replySuccess("No action will be taken on name hoisting.");
            return;
        }
        char symbol;
        if(event.getArgs().length()==1)
            symbol = event.getArgs().charAt(0);
        else
            throw new CommandExceptionListener.CommandErrorException("Provided symbol must be one character of the following: "+AutoMod.DEHOIST_JOINED);
        boolean allowed = false;
        for(char c: AutoMod.VALID_DEHOIST_CHAR)
            if(c==symbol)
                allowed = true;
        if(!allowed)
            throw new CommandExceptionListener.CommandErrorException("Provided symbol must be one character of the following: "+AutoMod.DEHOIST_JOINED);
        
        vortex.getDatabase().automod.setDehoistChar(event.getGuild(), symbol);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Users will now be dehoisted if their effective name starts with `"+symbol+"` or higher."+(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
