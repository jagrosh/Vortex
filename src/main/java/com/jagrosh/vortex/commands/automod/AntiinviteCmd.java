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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import net.dv8tion.jda.api.Permission;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.database.managers.AutomodManager;
import com.jagrosh.vortex.database.managers.PunishmentManager;
import com.jagrosh.vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntiinviteCmd extends Command
{
    private final Vortex vortex;
    
    public AntiinviteCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "antiinvite";
        this.guildOnly = true;
        this.aliases = new String[]{"antinvite","anti-invite"};
        this.category = new Category("AutoMod");
        this.arguments = "<strikes|whitelist...>";
        this.help = "sets strikes for posting invites";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.children = new Command[] {new WhitelistInvitesCmd(vortex)};
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
                event.replyError(FormatUtil.filterEveryone("`"+event.getArgs()+"` is not a valid integer!"));
                return;
            }
        }
        if(numstrikes<0 || numstrikes>AutomodManager.MAX_STRIKES)
            throw new CommandExceptionListener.CommandErrorException("The number of strikes must be between 0 and "+AutomodManager.MAX_STRIKES);
        if(numstrikes > 0 && !vortex.getDatabase().actions.hasPunishments(event.getGuild()))
            throw new CommandExceptionListener.CommandErrorException("Anti-Invite cannot be enabled without first setting at least one punishment.");
        
        vortex.getDatabase().automod.setInviteStrikes(event.getGuild(), numstrikes);
        event.replySuccess("Users will now receive `"+numstrikes+"` strikes for posting invite links.");
    }
}
