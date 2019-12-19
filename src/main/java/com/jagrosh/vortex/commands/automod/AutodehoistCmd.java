/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.core.Permission;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.database.managers.PunishmentManager;
import com.jagrosh.vortex.utils.OtherUtil;

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
            throw new CommandExceptionListener.CommandErrorException("Provided symbol must be one character of the following: "+OtherUtil.DEHOIST_JOINED);
        boolean allowed = false;
        for(char c: OtherUtil.DEHOIST_ORIGINAL)
            if(c==symbol)
                allowed = true;
        if(!allowed)
            throw new CommandExceptionListener.CommandErrorException("Provided symbol must be one character of the following: "+OtherUtil.DEHOIST_JOINED);
        
        vortex.getDatabase().automod.setDehoistChar(event.getGuild(), symbol);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Users will now be dehoisted if their effective name starts with `"+symbol+"` or higher."+(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
