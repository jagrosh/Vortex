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
import com.jagrosh.vortex.utils.FixedCache;
import java.sql.ResultSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class IgnoreManager extends DataManager
{
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L);
    public final static SQLColumn<Long> ENTITY_ID = new LongColumn("ENTITY_ID",false,0L,true);
    public final static SQLColumn<Integer> TYPE = new IntegerColumn("TYPE",false,0);
    
    private final FixedCache<Long, Set<Long>> cache = new FixedCache<>(1000);
    
    public IgnoreManager(DatabaseConnector connector)
    {
        super(connector, "IGNORED");
    }
    
    public boolean isIgnored(TextChannel tc)
    {
        return read(select(ENTITY_ID.is(tc.getIdLong()), ENTITY_ID), ResultSet::next);
    }
    
    public boolean isIgnored(Member member)
    {
        if(cache.contains(member.getGuild().getIdLong()))
        {
            for(long rid: cache.get(member.getGuild().getIdLong()))
                if(member.getRoles().stream().anyMatch(r -> r.getIdLong()==rid))
                    return true;
            return false;
        }
        return read(select(GUILD_ID.is(member.getGuild().getId())+" AND "+TYPE.is(Type.ROLE.ordinal()), ENTITY_ID), rs ->
        {
            while(rs.next())
            {
                long id = ENTITY_ID.getValue(rs);
                if(member.getRoles().stream().anyMatch(r -> r.getIdLong()==id))
                    return true;
            }
            return false;
        });
    }
    
    public boolean ignore(TextChannel tc)
    {
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
    
    public List<TextChannel> getIgnoredChannels(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getIdLong())+" AND "+TYPE.is(Type.TEXT_CHANNEL.ordinal())), rs ->
        {
            List<TextChannel> list = new LinkedList<>();
            while(rs.next())
            {
                TextChannel tc = guild.getTextChannelById(ENTITY_ID.getValue(rs));
                if(tc!=null)
                    list.add(tc);
            }
            return list;
        });
    }
    
    public List<Role> getIgnoredRoles(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
        {
            List<Role> list = new LinkedList<>();
            for(long rid: cache.get(guild.getIdLong()))
            {
                Role role = guild.getRoleById(rid);
                if(role!=null)
                    list.add(role);
            }
            return list;
        }
        return read(selectAll(GUILD_ID.is(guild.getIdLong())+" AND "+TYPE.is(Type.ROLE.ordinal())), rs ->
        {
            List<Role> list = new LinkedList<>();
            while(rs.next())
            {
                Role role = guild.getRoleById(ENTITY_ID.getValue(rs));
                if(role!=null)
                    list.add(role);
            }
            return list;
        });
    }
    
    public boolean unignore(TextChannel tc)
    {
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
