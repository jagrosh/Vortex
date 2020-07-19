/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
import com.jagrosh.vortex.utils.FixedCache;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.json.JSONArray;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class IgnoreManager extends DataManager
{
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L);
    public final static SQLColumn<Long> ENTITY_ID = new LongColumn("ENTITY_ID",false,0L,true);
    public final static SQLColumn<Integer> TYPE = new IntegerColumn("TYPE",false,0);
    
    private final FixedCache<Long, Set<Long>> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
    
    public IgnoreManager(DatabaseConnector connector)
    {
        super(connector, "IGNORED");
    }
    
    public JSONArray getIgnoresJson(Guild guild)
    {
        JSONArray array = new JSONArray();
        getIgnores(guild).forEach(id -> array.put(id));
        return array;
    }
    
    public boolean isIgnored(TextChannel tc)
    {
        return getIgnores(tc.getGuild()).contains(tc.getIdLong());
    }
    
    public boolean isIgnored(Member member)
    {
        Set<Long> ignored = getIgnores(member.getGuild());
        return member.getRoles().stream().anyMatch(r -> ignored.contains(r.getIdLong()));
    }
    
    public List<TextChannel> getIgnoredChannels(Guild guild)
    {
        return getIgnores(guild).stream().map(l -> guild.getTextChannelById(l)).filter(t -> t != null).collect(Collectors.toList());
    }
    
    public List<Role> getIgnoredRoles(Guild guild)
    {
        return getIgnores(guild).stream().map(l -> guild.getRoleById(l)).filter(r -> r != null).collect(Collectors.toList());
    }
    
    private Set<Long> getIgnores(Guild guild)
    {
        long gid = guild.getIdLong();
        if(cache.contains(gid))
            return cache.get(gid);
        Set<Long> ret = read(selectAll(GUILD_ID.is(gid)), rs -> 
        {
            Set<Long> set = new HashSet<>();
            while(rs.next())
                set.add(ENTITY_ID.getValue(rs));
            return set;
        });
        cache.put(gid, ret);
        return ret;
    }
    
    // set things in database
    public boolean ignore(TextChannel tc)
    {
        invalidateCache(tc.getGuild());
        return readWrite(selectAll(GUILD_ID.is(tc.getGuild().getIdLong())+" AND "+ENTITY_ID.is(tc.getIdLong())), rs ->
        {
            if(rs.next())
                return false;
            rs.moveToInsertRow();
            GUILD_ID.updateValue(rs, tc.getGuild().getIdLong());
            ENTITY_ID.updateValue(rs, tc.getIdLong());
            TYPE.updateValue(rs, Type.TEXT_CHANNEL.ordinal());
            rs.insertRow();
            return true;
        });
    }
    
    public boolean ignore(Role role)
    {
        invalidateCache(role.getGuild());
        return readWrite(selectAll(GUILD_ID.is(role.getGuild().getIdLong())+" AND "+ENTITY_ID.is(role.getIdLong())), rs ->
        {
            if(rs.next())
                return false;
            rs.moveToInsertRow();
            GUILD_ID.updateValue(rs, role.getGuild().getIdLong());
            ENTITY_ID.updateValue(rs, role.getIdLong());
            TYPE.updateValue(rs, Type.ROLE.ordinal());
            rs.insertRow();
            return true;
        });
    }
    
    public boolean unignore(TextChannel tc)
    {
        invalidateCache(tc.getGuild());
        return readWrite(selectAll(GUILD_ID.is(tc.getGuild().getIdLong())+" AND "+ENTITY_ID.is(tc.getIdLong())), rs ->
        {
            if(rs.next())
            {
                rs.deleteRow();
                return true;
            }
            return false;
        });
    }
    
    public boolean unignore(Role role)
    {
        invalidateCache(role.getGuild());
        return readWrite(selectAll(GUILD_ID.is(role.getGuild().getIdLong())+" AND "+ENTITY_ID.is(role.getIdLong())), rs ->
        {
            if(rs.next())
            {
                rs.deleteRow();
                return true;
            }
            return false;
        });
    }
    
    private void invalidateCache(Guild guild)
    {
        cache.pull(guild.getIdLong());
    }
    
    private enum Type
    {
        TEXT_CHANNEL, ROLE
    }
}
