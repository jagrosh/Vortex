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
import net.dv8tion.jda.api.Permission;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.AutomodManager;
import com.jagrosh.vortex.database.managers.PunishmentManager;
import com.jagrosh.vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AnticopypastaCmd extends Command
{
    private final Vortex vortex;
    
    public AnticopypastaCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "anticopypasta";
        this.guildOnly = true;
        this.aliases = new String[]{"antipasta","anti-copypasta"};
        this.category = new Category("AutoMod");
        this.arguments = "<strikes>";
        this.help = "sets strikes for posting copypastas";
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
                event.replyError(FormatUtil.filterEveryone("`"+event.getArgs()+"` is not a valid integer!"));
                return;
            }
        }
        if(numstrikes<0 || numstrikes>AutomodManager.MAX_STRIKES)
        {
            event.replyError("The number of strikes must be between 0 and "+AutomodManager.MAX_STRIKES);
            return;
        }
        vortex.getDatabase().automod.setCopypastaStrikes(event.getGuild(), numstrikes);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Users will now receive `"+numstrikes+"` strikes for posting copypastas."+(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
