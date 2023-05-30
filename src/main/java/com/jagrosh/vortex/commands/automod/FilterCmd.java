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
import com.jagrosh.vortex.database.managers.FilterManager;
import com.jagrosh.vortex.database.managers.PremiumManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;


import java.util.Arrays;
import java.util.List;

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
        sb.append(" The filter command is used to add and remove words from the bad words filter and the very bad words filter. "
                + " The difference between the two filters is that the very bad words filter will be logged to the \"important modlogs\" channel,"
                + " as its meant to log messages deleted because of slurs or other things that require a mods attention, opposed to being drowned out by the countless other logs. "
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
    
    private static class FilterAddCmd extends Command
    {
        public FilterAddCmd()
        {
            this.guildOnly = true;
            this.name = "add";
            this.category = new Category("AutoMod");
            this.aliases = new String[]{"create"};
            this.arguments = "<name> <badWords | veryBadWords> <words to add>";
            this.help = "adds words to the filter";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }

        @Override
        protected void execute(CommandEvent event)
        {
                // TODO: IMplement? idk
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
            this.arguments = "<name> <badWords | veryBadWords> <words to add>";
            this.help = "removes words from the filter";
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        }

        @Override
        protected void execute(CommandEvent event)
        {
            long guildId = event.getGuild().getIdLong();
            String[] parts = event.getArgs().split("\\s+", 2);
            String filterName = parts[0].toLowerCase();
            if (parts.length < 2) {
                event.replyError("Please include a filter name and the words to filter!");
                return;
            }

            boolean isVeryBadFilter;

            if (filterName.startsWith("verybadword")) {
                isVeryBadFilter = true;
            } else if (filterName.startsWith("badword")) {
                isVeryBadFilter = false;
            } else {
                event.replyWarning("Please specify if you are trying to remove from the `veryBadWords` or `badWords` filter");
                return;
            }

            try {
                FilterManager filters = vortex.getDatabase().filters;
                if (isVeryBadFilter) {
                    filters.updateVeryBadWordsFilter(event.getGuild(), removeWordsFromFilter(filters.getVeryBadWordsFilter(guildId), parts[1]));
                } else {
                    filters.updateBadWordFilter(event.getGuild(), removeWordsFromFilter(filters.getBadWordsFilter(guildId), parts[1]));
                }
                event.reply("Successfully updated the " + (isVeryBadFilter ? "Very " : "") + "Bad Words filter!");
            }
            catch(IllegalArgumentException ex)
            {
                event.replyError(ex.getMessage());
            }
        }

        private Filter removeWordsFromFilter(Filter filter, String wordsToRemove) {
            String[] wordsToRemoveArray = wordsToRemove.split(" ");
            List<String> filteredWords = Arrays.asList(filter.printContent().split(" "));
            Arrays.stream(wordsToRemoveArray).forEach(filteredWords::remove);
            StringBuilder parsedFilteredWords = new StringBuilder();
            filteredWords.stream().forEachOrdered(s -> parsedFilteredWords.append(s).append(' '));
            return Filter.parseFilter(parsedFilteredWords.toString().trim());
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
            long guildId = event.getGuild().getIdLong();
            FilterManager filters = vortex.getDatabase().filters;
            Filter badWordsFilter = filters.getBadWordsFilter(guildId);
            Filter veryBadWordsFilter = filters.getVeryBadWordsFilter(guildId);

            String embedContent = String.format("**Bad Words:** %s%n**Very Bad Words:**%s",
                    badWordsFilter == null ? "_None_" : badWordsFilter.printContentEscaped(),
                    veryBadWordsFilter == null ? "_None_" : veryBadWordsFilter.printContentEscaped()
            ).trim();

            event.reply(new EmbedBuilder()
                    .setColor(event.getSelfMember().getColor())
                    .addField(new Field("\uD83D\uDEAF Filters", embedContent, true))
                    .build());
            // Todo: add page turning if the embeds are too big
        }
    }
}
