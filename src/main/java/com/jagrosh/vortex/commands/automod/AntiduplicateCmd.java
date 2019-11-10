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
import com.jagrosh.vortex.database.managers.PunishmentManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntiduplicateCmd extends Command
{
    private final Vortex vortex;
    
    public AntiduplicateCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "antiduplicate";
        this.aliases = new String[]{"antidupe","anti-duplicate","anti-dupe"};
        this.guildOnly = true;
        this.category = new Category("AutoMod");
        this.arguments = "<strike threshold> [delete threshold] [strikes] or OFF";
        this.help = "prevents duplicate messages";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty() || event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("The Anti-Duplicate system prevents and punishes users for sending the same message repeatedly.\n"
                    + "Usage: `"+event.getClient().getPrefix()+name+" "+arguments+"`\n"
                    + "`<strike threshold>` - the number of duplicates at which strikes should start being assigned\n"
                    + "`[delete threshold]` - the number of duplicates at which a user's messages should start being deleted\n"
                    + "`[strikes]` - the number of strikes to assign on each duplicate after the strike threshold is met");
            return;
        }
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            vortex.getDatabase().automod.setDupeSettings(event.getGuild(), 0, 0, 0);
            event.replySuccess("Anti-Duplicate has been disabled.");
            return;
        }
        int strikeThreshold, deleteThreshold;
        int strikes = 1;
        String[] parts = event.getArgs().split("\\s+", 3);
        try
        {
            strikeThreshold = Integer.parseInt(parts[0]);
        }
        catch(NumberFormatException ex)
        {
            event.replyError("<strike threshold> must be an integer!");
            return;
        }
        if(parts.length==1)
            deleteThreshold = strikeThreshold==1 || strikeThreshold==2 ? 1 : strikeThreshold-2;
        else try
        {
            deleteThreshold = Integer.parseInt(parts[1]);
        }
        catch(NumberFormatException ex)
        {
            event.replyError("[delete threshold] must be an integer!");
            return;
        }
        if(parts.length==3) try
        {
            strikes = Integer.parseInt(parts[2]);
        }
        catch(NumberFormatException ex)
        {
            event.replyError("[strikes] must be an integer!");
            return;
        }
        if(strikeThreshold<=0 || deleteThreshold<=0 || strikes<=0)
        {
            vortex.getDatabase().automod.setDupeSettings(event.getGuild(), 0, 0, 0);
            event.replySuccess("Anti-Duplicate has been disabled.");
            return;
        }
        vortex.getDatabase().automod.setDupeSettings(event.getGuild(), strikes, deleteThreshold, strikeThreshold);
        boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
        event.replySuccess("Anti-Duplicate will now delete duplicates starting at duplicate **"+deleteThreshold
                +"** and begin assigning **"+strikes+"** strikes for each duplicate starting at duplicate **"+strikeThreshold+"**."
                +(also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
    }
}
