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
package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.automod.Filter;
import com.jagrosh.vortex.utils.FixedCache;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import org.json.JSONObject;

/**
 * A database manager for storing a guilds bad words and very bad words filter.
 * The distinction between the two is that while messages that violate the bad word filter will be logged in
 * the regular modlogs, messages that violate the very bad words filter (eg., slurs opposed to normal curses),
 * will be logged to the important modlogs channel for attention from the mod team.
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FilterManager extends DataManager
{
    private final static String SETTINGS_TITLE = "\uD83D\uDEAF Filters"; // 🚯
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L);
    public final static SQLColumn<String> BAD_WORDS = new StringColumn("BAD_WORDS", false, "", Filter.MAX_CONTENT_LENGTH);
    public final static SQLColumn<String> VERY_BAD_WORDS = new StringColumn("VERY_BAD_WORDS", false, "", Filter.MAX_CONTENT_LENGTH);

    private final FixedCache<Long, Filter> BAD_WORDS_CACHE = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
    private final FixedCache<Long, Filter> VERY_BAD_WORDS_CACHE = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);

    public FilterManager(DatabaseConnector connector)
    {
        super(connector, "FILTERS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID.toString();
    }

    public Filter getBadWordsFilter(long guildId) {
        Filter filter = BAD_WORDS_CACHE.get(guildId);
        if (filter == null) {
            filter = read(selectAll(GUILD_ID.is(guildId)), rs -> {
                if (rs.next()) {
                    try {
                        return Filter.parseFilter(BAD_WORDS.getValue(rs));
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
                return null;
            });
        }

        return filter;
    }

    public Filter getVeryBadWordsFilter(long guildId) {
        Filter filter = VERY_BAD_WORDS_CACHE.get(guildId);
        if (filter == null) {
            filter = read(selectAll(GUILD_ID.is(guildId)), rs -> {
                if (rs.next()) {
                    try {
                        return Filter.parseFilter(VERY_BAD_WORDS.getValue(rs));
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                }
                return null;
            });
        }

        return filter;
    }

    public Field getFiltersDisplay(Guild guild)
    {
        Filter badWordsFilter = getBadWordsFilter(guild.getIdLong());
        Filter veryBadWordsFilter = getVeryBadWordsFilter(guild.getIdLong());
        if (badWordsFilter == null && veryBadWordsFilter == null) {
            return null;
        }

        String embedContent = String.format("**Bad Words:** %s%n**Very Bad Words:**%s",
            badWordsFilter == null ? "_None_" : badWordsFilter.printContentEscaped(),
            veryBadWordsFilter == null ? "_None_" : veryBadWordsFilter.printContentEscaped()
        ).trim();

        return new Field(SETTINGS_TITLE, embedContent, true);
    }
    
    public JSONObject getFiltersJson(Guild guild) {
        Filter badWordsFilter = getBadWordsFilter(guild.getIdLong());
        Filter veryBadWordsFilter = getVeryBadWordsFilter(guild.getIdLong());
        return new JSONObject()
                .put("badWords", badWordsFilter == null ? JSONObject.NULL : badWordsFilter.printContent())
                .put("veryBadWords", badWordsFilter == null ? JSONObject.NULL : veryBadWordsFilter.printContent());
    }

    public void updateBadWordFilter(Guild guild, Filter filter) {
        long guildId = guild.getIdLong();
        readWrite(selectAll(GUILD_ID.is(guildId)), rs -> {
            if (rs.next()) {
                BAD_WORDS.updateValue(rs, filter.printContent());
            } else {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guildId);
                BAD_WORDS.updateValue(rs, filter.printContent());
                rs.insertRow();
            }
        });
        BAD_WORDS_CACHE.put(guildId, filter);
    }

    public void updateVeryBadWordsFilter(Guild guild, Filter filter) {
        long guildId = guild.getIdLong();
        readWrite(selectAll(GUILD_ID.is(guildId)), rs -> {
            if (rs.next()) {
                VERY_BAD_WORDS.updateValue(rs, filter.printContent());
            } else {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guildId);
                VERY_BAD_WORDS.updateValue(rs, filter.printContent());
                rs.insertRow();
            }
        });
        VERY_BAD_WORDS_CACHE.put(guildId, filter);
    }
}