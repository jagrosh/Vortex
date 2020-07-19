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
import java.util.HashMap;
import java.util.Map;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StrikeManager extends DataManager
{
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L);
    public final static SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0L);
    public final static SQLColumn<Integer> STRIKES = new IntegerColumn("STRIKES", false, 0);
    
    public StrikeManager(DatabaseConnector connector)
    {
        super(connector, "STRIKES");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+USER_ID;
    }
    
    public int[] addStrikes(Guild guild, long targetId, int strikes)
    {
        return readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(targetId)), rs -> 
        {
            if(rs.next())
            {
                int current = STRIKES.getValue(rs);
                STRIKES.updateValue(rs, current+strikes<0 ? 0 : current+strikes);
                rs.updateRow();
                return new int[]{current, current+strikes<0 ? 0 : current+strikes};
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, targetId);
                STRIKES.updateValue(rs, strikes<0 ? 0 : strikes);
                rs.insertRow();
                return new int[]{0, strikes<0 ? 0 : strikes};
            }
        });
    }
    
    public int[] removeStrikes(Member target, int strikes)
    {
        return removeStrikes(target.getGuild(), target.getUser().getIdLong(), strikes);
    }
    
    public int[] removeStrikes(Guild guild, long targetId, int strikes)
    {
        return addStrikes(guild, targetId, -1*strikes);
    }
    
    public int getStrikes(Member target)
    {
        return getStrikes(target.getGuild(), target.getUser().getIdLong());
    }
    
    public int getStrikes(Guild guild, long targetId)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(targetId)), rs -> 
        {
            if(rs.next())
                return STRIKES.getValue(rs);
            return 0;
        });
    }
    
    public Map<Long,Integer> getAllStrikes(Guild guild)
    {
        return getAllStrikes(guild.getIdLong());
    }
    
    public Map<Long,Integer> getAllStrikes(long guildId)
    {
        return read(selectAll(GUILD_ID.is(guildId)), rs -> 
        {
            HashMap<Long,Integer> map = new HashMap<>();
            while(rs.next())
                map.put(USER_ID.getValue(rs), STRIKES.getValue(rs));
            return map;
        });
    }
    
    public JSONObject getAllStrikesJson(Guild guild)
    {
        JSONObject obj = new JSONObject();
        getAllStrikes(guild).entrySet().forEach(e -> obj.put(Long.toString(e.getKey()), e.getValue()));
        return obj;
    }
}
