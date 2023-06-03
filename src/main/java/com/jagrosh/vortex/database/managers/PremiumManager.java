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
import com.jagrosh.vortex.managers.MultiBotManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PremiumManager extends DataManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L, true);
    public static final SQLColumn<Instant> UNTIL = new InstantColumn("UNTIL", false, Instant.EPOCH);
    public static final SQLColumn<Integer> LEVEL = new IntegerColumn("LEVEL", false, 0);
    public static final SQLColumn<Long> USER_ID  = new LongColumn("USER_ID", false, 0L);
    
    private final long premiumGuildId = 147698382092238848L;
    private final long premiumRoleId = 867194076813852692L;
    private final PremiumInfo NO_PREMIUM = new PremiumInfo();
    private final Logger LOG = LoggerFactory.getLogger(PremiumManager.class);
    
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
    
    public Map<Long,PremiumInfo> getPremiumGuildsInfo()
    {
        return read(selectAll(LEVEL.isGreaterThan(-1)), rs -> 
        {
            Map<Long,PremiumInfo> map = new HashMap<>();
            while(rs.next())
                map.put(GUILD_ID.getValue(rs), new PremiumInfo(rs));
            return map;
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
    
    public boolean setUserForGuild(User user, Guild guild)
    {
        return readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> 
        {
            if(rs.next())
            {
                USER_ID.updateValue(rs, user.getIdLong());
                rs.updateRow();
                return true;
            }
            else
                return false;
        });
    }
    
    public void checkPremiumSubscriptions(MultiBotManager shards)
    {
        Guild guild = shards.getGuildById(premiumGuildId);
        if(guild == null)
            return;
        Role role = guild.getRoleById(premiumRoleId);
        if(role == null)
            return;
        // get all the user IDs with a specific role
        List<Long> users = guild.getMembersWithRoles(role).stream().map(m -> m.getIdLong()).collect(Collectors.toList());
        // apply a minimum time of 40 days
        List<Long> guilds = applyMinimumTimeBuffer(users, 40, ChronoUnit.DAYS);
        LOG.info("Updated premium subscriptions for " + guilds.toString());
    }
    
    /**
     * Updates the time remaining for all guilds with a premium subscription, based on the user who is associated with
     * the guild
     * 
     * @param users A list of users that have active premium subscriptions
     * @param time The value of the minimum time
     * @param unit The units of the minimum time
     * @return A list of the guild IDs that got updated
     */
    public List<Long> applyMinimumTimeBuffer(List<Long> users, int time, TemporalUnit unit)
    {
        Instant targetTime = Instant.now().plus(time, unit);
        List<Long> guilds = new ArrayList<>();
        readWrite(selectAll(UNTIL.isLessThan(targetTime.getEpochSecond())), rs -> 
        {
            while(rs.next())
            {
                if(LEVEL.getValue(rs) > Level.NONE.ordinal() && users.contains(USER_ID.getValue(rs)))
                {
                    UNTIL.updateValue(rs, targetTime);
                    rs.updateRow();
                    guilds.add(GUILD_ID.getValue(rs));
                }
            }
        });
        return guilds;
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
        PLUS("Vortex Pro Lite"),
        PRO("Vortex Pro"),
        ULTRA("Vortex Experimental");
        
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
            return Constants.WARNING + " Sorry, this feature requires " + name + ". See <" + Constants.Wiki.VORTEX_PRO + "> for more information.";
        }
    }
    
    public class PremiumInfo
    {
        public final Level level;
        public final Instant until;
        public final long user;
        
        private PremiumInfo(ResultSet rs) throws SQLException
        {
            this.level = Level.values()[LEVEL.getValue(rs)];
            this.until = UNTIL.getValue(rs);
            this.user = USER_ID.getValue(rs);
        }
        
        private PremiumInfo()
        {
            level = Level.NONE;
            until = null;
            user = 0L;
        }
        
        public String getFooterString()
        {
            if(level==Level.NONE)
                return "This server does not have Vortex Pro";
            if(isPermanent())
                return "This server has " + level.name + " permanently";
            return "This server has " + level.name + " until";
        }
        
        public Instant getTimestamp()
        {
            if(level==Level.NONE || until==null || until.getEpochSecond()==Instant.MAX.getEpochSecond())
                return null;
            return until;
        }
        
        public boolean isPermanent()
        {
            return until != null && until.getEpochSecond() == Instant.MAX.getEpochSecond();
        }

        @Override
        public String toString()
        {
            return level.name + " (Until " + (until == null ? "never" : isPermanent() ? "forever"
                    : until.atZone(ZoneId.of("GMT-4")).format(DateTimeFormatter.RFC_1123_DATE_TIME)) + ")";
        }
    }
}
