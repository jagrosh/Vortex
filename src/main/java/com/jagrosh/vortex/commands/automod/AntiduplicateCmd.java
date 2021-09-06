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
        this.arguments = "<delete threshold> or OFF";
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
                    + "`[delete threshold]` - the number of duplicates at which a user's messages should start being deleted\n");
            return;
        }
        if(event.getArgs().equalsIgnoreCase("off"))
        {
            vortex.getDatabase().automod.setDupeThresh(event.getGuild(), 0);
            event.replySuccess("Anti-Duplicate has been disabled.");
            return;
        }
        int deleteThreshold;
        try
        {
            deleteThreshold = Integer.parseInt(event.getArgs());
        }
        catch(NumberFormatException ex)
        {
            event.replyError("The delete threshold must be an integer!");
            return;
        }
        if(deleteThreshold <= 0)
        {
            vortex.getDatabase().automod.setDupeThresh(event.getGuild(), deleteThreshold);
            event.replySuccess("Anti-Duplicate has been disabled.");
            return;
        }
        vortex.getDatabase().automod.setDupeThresh(event.getGuild(), deleteThreshold);
        event.replySuccess("Anti-Duplicate will now delete duplicates starting at duplicate **"+deleteThreshold + "**.");
    }
}