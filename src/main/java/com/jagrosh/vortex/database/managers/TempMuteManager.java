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
import com.jagrosh.easysql.columns.InstantColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.vortex.utils.Pair;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TempMuteManager extends DataManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<Instant> FINISH = new InstantColumn("FINISH", false, Instant.EPOCH);
    
    public TempMuteManager(DatabaseConnector connector)
    {
        super(connector, "TEMP_MUTES");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+USER_ID;
    }
    
    public JSONObject getAllMutesJson(Guild guild)
    {
        List<Pair<Long,Instant>> list = read(selectAll(GUILD_ID.is(guild.getId())), rs -> 
        {
            List<Pair<Long,Instant>> arr = new ArrayList<>();
            while(rs.next())
                arr.add(new Pair<>(USER_ID.getValue(rs), FINISH.getValue(rs)));
            return arr;
        });
        JSONObject json = new JSONObject();
        list.forEach(p -> json.put(Long.toString(p.getKey()), p.getValue().getEpochSecond()));
        return json;
    }
    
    public boolean isMuted(Member member)
    {
        return read(selectAll(GUILD_ID.is(member.getGuild().getId())+" AND "+USER_ID.is(member.getUser().getId())+" AND "+FINISH.isGreaterThan(Instant.now().getEpochSecond())), 
                rs -> {return rs.next();});
    }
    
    public void setMute(Guild guild, long userId, Instant finish)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs -> 
        {
            if(rs.next())
            {
                if(FINISH.getValue(rs).isBefore(finish))
                {
                    FINISH.updateValue(rs, finish);
                    rs.updateRow();
                }
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                FINISH.updateValue(rs, finish);
                rs.insertRow();
            }
        });
    }
    
    public void overrideMute(Guild guild, long userId, Instant finish)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs -> 
        {
            if(rs.next())
            {
                FINISH.updateValue(rs, finish);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                FINISH.updateValue(rs, finish);
                rs.insertRow();
            }
        });
    }
    
    public void removeMute(Guild guild, long userId)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs -> 
        {
            if(rs.next())
                rs.deleteRow();
        });
    }
    
    public int timeUntilUnmute(Guild guild, long userId)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs -> 
        {
            if(rs.next())
            {
                Instant end = FINISH.getValue(rs);
                if(end.getEpochSecond() == Instant.MAX.getEpochSecond())
                    return Integer.MAX_VALUE;
                else
                    return (int)(Instant.now().until(end, ChronoUnit.MINUTES));
            }
            return 0;
        });
    }
    
    public void checkUnmutes(JDA jda, GuildSettingsDataManager data)
    {
        readWrite(selectAll(FINISH.isLessThan(Instant.now().getEpochSecond())), rs -> 
        {
            while(rs.next())
            {
                Guild g = jda.getGuildById(GUILD_ID.getValue(rs));
                if(g==null || !g.isAvailable() || !g.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
                    continue;
                Role mRole = data.getSettings(g).getMutedRole(g);
                if(mRole==null || !g.getSelfMember().canInteract(mRole))
                {
                    rs.deleteRow();
                    continue;
                }
                Member m = g.getMemberById(USER_ID.getValue(rs));
                if(m!=null && m.getRoles().contains(mRole))
                    g.getController().removeSingleRoleFromMember(m, mRole).reason("Temporary Mute Completed").queue();
                rs.deleteRow();
            }
        });
    }
}
