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
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.automod.Filter;
import com.jagrosh.vortex.utils.FixedCache;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FilterManager extends DataManager
{
    private final static int MAX_FILTERS = 15;
    private final static String SETTINGS_TITLE = "\uD83D\uDEAF Filters"; // 🚯
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L);
    public final static SQLColumn<String> SHORTNAME = new StringColumn("SHORTNAME",false,"",Filter.MAX_NAME_LENGTH);
    public final static SQLColumn<String> NAME = new StringColumn("NAME",false,"",Filter.MAX_NAME_LENGTH);
    public final static SQLColumn<Integer> STRIKES = new IntegerColumn("STRIKES", false, 0);
    public final static SQLColumn<String> CONTENT = new StringColumn("CONTENT", false, "", Filter.MAX_CONTENT_LENGTH);
    
    private final FixedCache<Long, List<Filter>> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
    
    public FilterManager(DatabaseConnector connector)
    {
        super(connector, "FILTERS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+SHORTNAME;
    }
    
    public List<Filter> getFilters(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
            return cache.get(guild.getIdLong());
        List<Filter> filters = read(selectAll(GUILD_ID.is(guild.getId())), rs -> 
        {
            List<Filter> list = new ArrayList<>();
            while(rs.next())
            {
                try
                {
                    Filter filter = Filter.parseFilter(NAME.getValue(rs), STRIKES.getValue(rs), CONTENT.getValue(rs));
                    if(filter != null)
                        list.add(filter);
                }
                catch(IllegalArgumentException ignore) {}
            }
            return list;
        });
        cache.put(guild.getIdLong(), filters);
        return filters;
    }
    
    public Field getFiltersDisplay(Guild guild)
    {
        List<Filter> filters = getFilters(guild);
        if(filters.isEmpty())
            return null;
        StringBuilder sb = new StringBuilder();
        filters.forEach(f -> sb.append("\n**").append(f.name).append("** (`").append(f.strikes).append(" ")
                .append(Action.STRIKE.getEmoji()).append("`): ").append(f.printContentEscaped()));
        return new Field(SETTINGS_TITLE, sb.toString().trim(), true);
    }
    
    public JSONObject getFiltersJson(Guild guild)
    {
        List<Filter> filters = getFilters(guild);
        JSONObject obj = new JSONObject();
        filters.forEach(f -> obj.put(f.name, new JSONObject().put("strikes",f.strikes).put("content", f.printContent())));
        return obj;
    }
    
    public boolean addFilter(Guild guild, Filter filter)
    {
        String shortname = shortnameOf(filter.name);
        if(shortname.isEmpty())
            return false;
        if(getFilters(guild).size() >= MAX_FILTERS)
            return false;
        invalidateCache(guild);
        return readWrite(selectAll(GUILD_ID.is(guild.getId()) + " AND " + SHORTNAME.is("'"+shortname+"'")), rs -> 
        {
            if(rs.next())
                return false;
            rs.moveToInsertRow();
            GUILD_ID.updateValue(rs, guild.getIdLong());
            SHORTNAME.updateValue(rs, shortname);
            NAME.updateValue(rs, filter.name);
            STRIKES.updateValue(rs, filter.strikes);
            CONTENT.updateValue(rs, filter.printContent());
            rs.insertRow();
            return true;
        });
    }
    
    public Filter deleteFilter(Guild guild, String name)
    {
        String shortname = shortnameOf(name);
        if(shortname.isEmpty())
            return null;
        invalidateCache(guild);
        return readWrite(selectAll(GUILD_ID.is(guild.getId()) + " AND " + SHORTNAME.is("'"+shortname+"'")), rs -> 
        {
            if(rs.next())
            {
                Filter filter = null;
                try
                {
                    filter = Filter.parseFilter(NAME.getValue(rs), STRIKES.getValue(rs), CONTENT.getValue(rs));
                }
                catch(IllegalArgumentException ignore) {}
                rs.deleteRow();
                return filter;
            }
            return null;
        });
    }
    
    public int deleteAllFilters(long guildId)
    {
        invalidateCache(guildId);
        return readWrite(selectAll(GUILD_ID.is(guildId)), rs -> 
        {
            int count = 0;
            while(rs.next())
            {
                rs.deleteRow();
                count++;
            }
            return count;
        });
    }
    
    private void invalidateCache(Guild guild)
    {
        invalidateCache(guild.getIdLong());
    }
    
    private void invalidateCache(long guildId)
    {
        cache.pull(guildId);
    }
    
    private String shortnameOf(String name)
    {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }
}
