/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.automod.Filter;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.utils.FormatUtil;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FilterCmd extends Command
{
    private final Vortex vortex;
    
    public FilterCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.guildOnly = true;
        this.name = "filter";
        this.category = new Category("AutoMod");
        this.aliases = new String[]{"filters"};
        this.arguments = "<create | remove> [arguments...]";
        this.help = "modifies the word filter";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.children = new Command[]{new FilterAddCmd(), new FilterRemoveCmd(), new FilterListCmd()};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        StringBuilder sb = new StringBuilder(Constants.VORTEX_EMOJI);
        sb.append(" The filter command is used to create and remove word filters. Each filter can contain "
                + "multiple words which are individually checked for within every message sent on the server. "
                + "Additionally, a regex can be checked by surrounding the word in grave accents (\\`), or an exact "
                + "quote can be checked for by surrounding the word in double quotation marks (\"). \n");
        for(Command cmd: children)
        {
            sb.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName())
                    .append(cmd.getArguments() == null ? "" : " " + cmd.getArguments()).append("` - ").append(cmd.getHelp());
        }
        event.reply(sb.toString());
    }
    
    private class FilterAddCmd extends Command
    {
        public FilterAddCmd()
        {
            this.guildOnly = true;
            this.name = "add";
            this.category = new Category("AutoMod");
            this.aliases = new String[]{"create"};
            this.arguments = "<name> <strikes> <words to filter>";
            this.help = "adds a filter";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(!vortex.getDatabase().premium.getPremiumInfo(event.getGuild()).level.isAtLeast(PremiumManager.Level.PRO))
            {
                event.reply(PremiumManager.Level.PRO.getRequirementMessage());
                return;
            }
            
            String[] parts = event.getArgs().split("\\s+", 3);
            if(parts.length < 3)
            {
                event.replyError("Please include a filter name, number of strikes, and words to filter!");
                return;
            }
            
            int strikes;
            try
            {
                strikes = Integer.parseInt(parts[1]);
            }
            catch(NumberFormatException ex)
            {
                strikes = -1;
            }
            
            if(strikes < 0)
            {
                event.replyError("`<strikes>` must be a valid integer greater than zero!");
                return;
            }
            
            try
            {
                Filter filter = Filter.parseFilter(parts[0], strikes, parts[2]);
                if(vortex.getDatabase().filters.addFilter(event.getGuild(), filter))
                {
                    event.replySuccess(FormatUtil.filterEveryone("Filter *" + filter.name + "* (`" + filter.strikes + " " + Action.STRIKE.getEmoji() 
                            + "`) successfully created with filtered terms:\n" + filter.printContentEscaped()));
                }
                else
                {
                    event.replyError("Filter name contains no alphanumeric characters, or filter with the same name already exists!");
                }
            }
            catch(IllegalArgumentException ex)
            {
                event.replyError(ex.getMessage());
            }
        }
    }
    
    private class FilterRemoveCmd extends Command
    {
        public FilterRemoveCmd()
        {
            this.guildOnly = true;
            this.name = "remove";
            this.category = new Category("AutoMod");
            this.aliases = new String[]{"delete"};
            this.arguments = "<name>";
            this.help = "removes a filter";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            if(event.getArgs().isEmpty())
            {
                event.replyError("Please include the name of the filter to remove");
                return;
            }
            
            Filter filter = vortex.getDatabase().filters.deleteFilter(event.getGuild(), event.getArgs());
            if(filter == null)
            {
                event.replyError(FormatUtil.filterEveryone("Filter `" + event.getArgs() + "` could not be found"));
            }
            else
            {
                event.replySuccess(FormatUtil.filterEveryone("Removed filter `" + filter.name + "`"));
            }
        }
    }
    
    private class FilterListCmd extends Command
    {
        public FilterListCmd()
        {
            this.guildOnly = true;
            this.name = "list";
            this.category = new Category("AutoMod");
            this.aliases = new String[]{"show"};
            this.help = "shows the current filters";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
            this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            Field field = vortex.getDatabase().filters.getFiltersDisplay(event.getGuild());
            if(field == null)
            {
                event.replyWarning(FormatUtil.filterEveryone("There are no filters for **" + event.getGuild().getName() + "**"));
                return;
            }
            
            event.reply(new EmbedBuilder()
                    .setColor(event.getSelfMember().getColor())
                    .addField(field)
                    .build());
        }
    }
}
