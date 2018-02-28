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
import java.sql.ResultSet;
import java.time.Instant;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

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
    
    public void checkUnmutes(Guild guild)
    {
        if(!guild.isAvailable())
            return;
        if(!guild.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
            return;
        Role muted = guild.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("Muted")).findFirst().orElse(null);
        if(muted==null || !guild.getSelfMember().canInteract(muted))
            return;
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+FINISH.isLessThan(Instant.now().getEpochSecond())), rs -> 
        {
            while(rs.next())
            {
                Member m = guild.getMemberById(USER_ID.getValue(rs));
                if(m!=null && m.getRoles().contains(muted))
                    guild.getController().removeSingleRoleFromMember(m, muted).reason("Temporary Mute Completed").queue();
                rs.deleteRow();
            }
        });
    }
    
    public void checkUnmutes(JDA jda)
    {
        jda.getGuilds().forEach(g -> checkUnmutes(g));
    }
}
