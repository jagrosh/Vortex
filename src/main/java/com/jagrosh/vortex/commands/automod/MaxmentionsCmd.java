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
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.core.Permission;
import com.jagrosh.vortex.database.managers.AutomodManager;
import com.jagrosh.vortex.database.managers.PunishmentManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MaxmentionsCmd extends Command
{
    private final Vortex vortex;
    
    public MaxmentionsCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.guildOnly = true;
        this.name = "maxmentions";
        this.aliases = new String[]{"antimention","maxmention","mentionmax","mentionsmax"};
        this.category = new Category("AutoMod");
        this.arguments = "<number | OFF>";
        this.help = "sets max mentions a user can send";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.children = new Command[]{new AntirolementionCmd()};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().equalsIgnoreCase("off") || event.getArgs().equalsIgnoreCase("none"))
        {
            vortex.getDatabase().automod.disableMaxMentions(event.getGuild());
            event.replySuccess("Anti-Mention has been disabled.");
            return;
        }
        else if(event.getArgs().isEmpty())
        {
            event.replyError("Please include an integer value or `OFF`");
            return;
        }
        try
        {
            short num = Short.parseShort(event.getArgs());
            if(num<AutomodManager.MENTION_MINIMUM)
            {
                event.replyError("Maximum mentions must be at least `"+AutomodManager.MENTION_MINIMUM+"`");
                return;
            }
            vortex.getDatabase().automod.setMaxMentions(event.getGuild(), num);
            boolean also = vortex.getDatabase().actions.useDefaultSettings(event.getGuild());
            event.replySuccess("Set the maximum allowed mentions to **"+num+"** users. Messages containing more than **"
                    +num+"** mentions will be deleted and the user will obtain 1 strike for every mention above the maximum."
                    + "\n\nTo set the maximum allowed role mentions, use `"+Constants.PREFIX+name+" "+children[0].getName()+" "+children[0].getArguments()+"`"
                    + (also ? PunishmentManager.DEFAULT_SETUP_MESSAGE : ""));
        }
        catch(NumberFormatException e)
        {
            event.replyError("`<maximum>` must be a valid integer at least `"+AutomodManager.MENTION_MINIMUM+"`");
        }
    }
    
    private class AntirolementionCmd extends Command
    {
        public AntirolementionCmd()
        {
            this.guildOnly = true;
            this.name = "roles";
            this.aliases = new String[]{"role"};
            this.category = new Category("AutoMod");
            this.arguments = "<number>";
            this.help = "sets maximum number of unique role mentions a user can send";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }
        
        @Override
        protected void execute(CommandEvent event)
        {
            if(event.getArgs().equalsIgnoreCase("off") || event.getArgs().equalsIgnoreCase("none"))
            {
                vortex.getDatabase().automod.setMaxRoleMentions(event.getGuild(), 0);
                event.replySuccess("Anti-Mention for Role mentions has been disabled.");
                return;
            }
            if(event.getArgs().isEmpty())
            {
                event.replyError("Please include an integer value or `OFF`");
                return;
            }
            try
            {
                short num = Short.parseShort(event.getArgs());
                if(num<AutomodManager.ROLE_MENTION_MINIMUM)
                {
                    event.replyError("Maximum role mentions must be at least `"+AutomodManager.ROLE_MENTION_MINIMUM+"`");
                    return;
                }
                vortex.getDatabase().automod.setMaxRoleMentions(event.getGuild(), num);
                event.replySuccess("Set the maximum allowed role mentions to **"+num+"** roles.");
            }
            catch(NumberFormatException e)
            {
                event.replyError("`<maximum>` must be a valid integer at least `"+AutomodManager.ROLE_MENTION_MINIMUM+"`");
            }
        }
    }
}
