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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumManager extends DataManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L, true);
    public static final SQLColumn<Instant> UNTIL = new InstantColumn("UNTIL", false, Instant.EPOCH);
    public static final SQLColumn<Integer> LEVEL = new IntegerColumn("LEVEL", false, 0);
    
    private final PremiumInfo NO_PREMIUM = new PremiumInfo();
    
    public PremiumManager(DatabaseConnector connector)
    {
        super(connector, "PREMIUM");
    }
    
    public List<Long> getPremiumGuilds()
    {
        return read(selectAll(LEVEL.isGreaterThan(-1)), rs -> 
        {
            List<Long> list = new ArrayList<>();
            while(rs.next())
                list.add(GUILD_ID.getValue(rs));
            return list;
        });
    }
    
    public PremiumInfo getPremiumInfo(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> 
        {
            if(rs.next())
            {
                if(UNTIL.getValue(rs).isAfter(Instant.now()))
                    return new PremiumInfo(rs);
            }
            return NO_PREMIUM;
        });
    }
    
    public JSONObject getPremiumInfoJson(Guild guild)
    {
        PremiumInfo info = getPremiumInfo(guild);
        return new JSONObject()
                .put("level", info.level.name())
                .put("until", info.until == null ? 0 : info.until.getEpochSecond());
    }
    
    public void addPremiumForever(Guild guild, Level level)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> 
        {
            if(rs.next())
            {
                LEVEL.updateValue(rs, level.ordinal());
                UNTIL.updateValue(rs, Instant.MAX);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                LEVEL.updateValue(rs, level.ordinal());
                UNTIL.updateValue(rs, Instant.MAX);
                rs.insertRow();
            }
        });
    }
    
    public void addPremium(Guild guild, Level level, int time, TemporalUnit unit)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> 
        {
            if(rs.next())
            {
                LEVEL.updateValue(rs, level.ordinal());
                Instant current = UNTIL.getValue(rs);
                if(current.getEpochSecond() != Instant.MAX.getEpochSecond())
                {
                    Instant now = Instant.now();
                    UNTIL.updateValue(rs, now.isBefore(current) ? current.plus(time, unit) : now.plus(time, unit));
                }
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                LEVEL.updateValue(rs, level.ordinal());
                UNTIL.updateValue(rs, Instant.now().plus(time, unit));
                rs.insertRow();
            }
        });
    }
    
    public void cancelPremium(Guild guild)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> 
        {
            if(rs.next())
                rs.deleteRow();
        });
    }
    
    public List<Long> cleanPremiumList()
    {
        return readWrite(selectAll(UNTIL.isLessThan(Instant.now().getEpochSecond())), rs -> 
        {
            List<Long> list = new LinkedList<>();
            while(rs.next())
            {
                list.add(GUILD_ID.getValue(rs));
                rs.deleteRow();
            }
            return list;
        });
    }
    
    public static enum Level
    {
        NONE("No Premium"),
        PLUS("Vortex Plus"),
        PRO("Vortex Pro"),
        ULTRA("Vortex Ultra");
        
        public final String name;
        
        private Level(String name)
        {
            this.name = name;
        }
        
        public boolean isAtLeast(Level other)
        {
            return ordinal() >= other.ordinal();
        }
        
        public String getRequirementMessage()
        {
            return Constants.WARNING + " Sorry, this feature requires " + name + ". " + name + " is not available yet.";
        }
    }
    
    public class PremiumInfo
    {
        public final Level level;
        public final Instant until;
        
        private PremiumInfo(ResultSet rs) throws SQLException
        {
            this.level = Level.values()[LEVEL.getValue(rs)];
            this.until = UNTIL.getValue(rs);
        }
        
        private PremiumInfo()
        {
            level = Level.NONE;
            until = null;
        }
        
        public String getFooterString()
        {
            if(level==Level.NONE)
                return "This server does does not have Vortex Plus or Vortex Pro";
            if(until.getEpochSecond()==Instant.MAX.getEpochSecond())
                return "This server has " + level.name + " permanently";
            return "This server has " + level.name + " until";
        }
        
        public Instant getTimestamp()
        {
            if(level==Level.NONE || until==null || until.getEpochSecond()==Instant.MAX.getEpochSecond())
                return null;
            return until;
        }

        @Override
        public String toString()
        {
            return level.name + " (Until " + (until == null ? "never" 
                    : until.atZone(ZoneId.of("GMT-4")).format(DateTimeFormatter.RFC_1123_DATE_TIME)) + ")";
        }
    }
}
