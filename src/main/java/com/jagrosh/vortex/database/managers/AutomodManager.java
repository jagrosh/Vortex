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
import java.sql.SQLException;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AutomodManager extends DataManager
{
    public final static int MAX_STRIKES = 100;
    public final static int MENTION_MINIMUM = 7;
    public final static int ROLE_MENTION_MINIMUM = 2;
    private static final String SETTINGS_TITLE = "\uD83D\uDEE1 Automod Settings";
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L,true);
    
    public final static SQLColumn<Integer> MAX_MENTIONS = new IntegerColumn("MAX_MENTIONS", false, 0);
    public final static SQLColumn<Integer> MAX_ROLE_MENTIONS = new IntegerColumn("MAX_ROLE_MENTIONS", false, 0);
    
    public final static SQLColumn<Integer> RAIDMODE_NUMBER = new IntegerColumn("RAIDMODE_NUMBER", false, 0);
    public final static SQLColumn<Integer> RAIDMODE_TIME = new IntegerColumn("RAIDMODE_TIME", false, 0);
    
    public final static SQLColumn<Integer> INVITE_STRIKES = new IntegerColumn("INVITE_STRIKES", false, 0);
    public final static SQLColumn<Integer> REF_STRIKES = new IntegerColumn("REF_STRIKES", false, 0);
    
    public final static SQLColumn<Integer> DUPE_STRIKES = new IntegerColumn("DUPE_STRIKES", false, 0);
    public final static SQLColumn<Integer> DUPE_DELETE_THRESH = new IntegerColumn("DUPE_DELETE_THRESH", false, 0);
    public final static SQLColumn<Integer> DUPE_STRIKE_THRESH = new IntegerColumn("DUPE_STRIKES_THRESH", false, 0);
            
    // Cache
    private final FixedCache<Long, AutomodSettings> cache = new FixedCache<>(1000);
    private final AutomodSettings blankSettings = new AutomodSettings();
    
    public AutomodManager(DatabaseConnector connector)
    {
        super(connector, "AUTOMOD");
    }
    
    // Getters
    public AutomodSettings getSettings(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
            return cache.get(guild.getIdLong());
        AutomodSettings settings = read(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> rs.next() ? new AutomodSettings(rs) : blankSettings);
        cache.put(guild.getIdLong(), settings);
        return settings;
    }
    
    public Field getSettingsDisplay(Guild guild)
    {
        AutomodSettings settings = getSettings(guild);
        return new Field(SETTINGS_TITLE, 
                  "__Anti-Invite__\n" + (settings.inviteStrikes==0 
                    ? "Disabled\n\n"
                    : "Strikes: **" + settings.inviteStrikes + "**\n\n")
                + "__Anti-Duplicate__\n" + (settings.useAntiDuplicate() 
                    ? "Delete Threshold: **" + settings.dupeDeleteThresh + "**\n" +
                      "Strike Threshold: **" + settings.dupeStrikeThresh + "**\n" +
                      "Strikes: **" + settings.dupeStrikes + "**\n\n" 
                    : "Disabled\n\n")
                + "__Anti-Mass-Mention__\n" + (settings.maxMentions==0 && settings.maxRoleMentions==0 
                    ? "Disabled\n\n" 
                    : "Max User Mentions: " + (settings.maxMentions==0 ? "None\n" : "**" + settings.maxMentions + "**\n") +
                      "Max Role Mentions: " + (settings.maxRoleMentions==0 ? "None\n\n" : "**" + settings.maxRoleMentions + "**\n\n"))
                + "__Auto Anti-Raid Mode__\n" + (settings.useAutoRaidMode() 
                    ? "**" + settings.raidmodeNumber + "** joins / **" + settings.raidmodeTime + "** seconds\n" 
                    : "Disabled\n")
                + "\u200B", true);
    }
    
    // Setters
    public void disableMaxMentions(Guild guild)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                MAX_MENTIONS.updateValue(rs, 0);
                MAX_ROLE_MENTIONS.updateValue(rs, 0);
                rs.updateRow();
            }
        });
    }
    
    public void setMaxMentions(Guild guild, int max)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                MAX_MENTIONS.updateValue(rs, max);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MAX_MENTIONS.updateValue(rs, max);
                rs.insertRow();
            }
        });
    }
    
    public void setMaxRoleMentions(Guild guild, int max)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                MAX_ROLE_MENTIONS.updateValue(rs, max);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MAX_ROLE_MENTIONS.updateValue(rs, max);
                rs.insertRow();
            }
        });
    }
    
    public void setAutoRaidMode(Guild guild, int number, int time)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                RAIDMODE_NUMBER.updateValue(rs, number);
                RAIDMODE_TIME.updateValue(rs, time);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                RAIDMODE_NUMBER.updateValue(rs, number);
                RAIDMODE_TIME.updateValue(rs, time);
                rs.insertRow();
            }
        });
    }
    
    public void setInviteStrikes(Guild guild, int strikes)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                INVITE_STRIKES.updateValue(rs, strikes);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                INVITE_STRIKES.updateValue(rs, strikes);
                rs.insertRow();
            }
        });
    }
    
    public void setRefStrikes(Guild guild, int strikes)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                REF_STRIKES.updateValue(rs, strikes);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                REF_STRIKES.updateValue(rs, strikes);
                rs.insertRow();
            }
        });
    }
    
    public void setDupeSettings(Guild guild, int strikes, int deleteThresh, int strikeThresh)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                DUPE_STRIKES.updateValue(rs, strikes);
                DUPE_DELETE_THRESH.updateValue(rs, deleteThresh);
                DUPE_STRIKE_THRESH.updateValue(rs, strikeThresh);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                DUPE_STRIKES.updateValue(rs, strikes);
                DUPE_DELETE_THRESH.updateValue(rs, deleteThresh);
                DUPE_STRIKE_THRESH.updateValue(rs, strikeThresh);
                rs.insertRow();
            }
        });
    }
    
    private void invalidateCache(Guild guild)
    {
        cache.pull(guild.getIdLong());
    }
    
    public class AutomodSettings
    {
        public final int maxMentions, maxRoleMentions;
        public final int raidmodeNumber, raidmodeTime;
        public final int inviteStrikes;
        public final int refStrikes;
        public final int dupeStrikes, dupeDeleteThresh, dupeStrikeThresh;
        
        private AutomodSettings()
        {
            this.maxMentions = 0;
            this.maxRoleMentions = 0;
            this.raidmodeNumber = 0;
            this.raidmodeTime = 0;
            this.inviteStrikes = 0;
            this.refStrikes = 0;
            this.dupeStrikes = 0;
            this.dupeDeleteThresh = 0;
            this.dupeStrikeThresh = 0;
        }
        
        private AutomodSettings(int maxMentions, int maxRoleMentions, int raidmodeNumber, int raidmodeTime, 
                int inviteStrikes, int refStrikes, int dupeStrikes, int dupeDeleteThresh, int dupeStrikeThresh)
        {
            this.maxMentions = maxMentions;
            this.maxRoleMentions = maxRoleMentions;
            this.raidmodeNumber = raidmodeNumber;
            this.raidmodeTime = raidmodeTime;
            this.inviteStrikes = inviteStrikes;
            this.refStrikes = refStrikes;
            this.dupeStrikes = dupeStrikes;
            this.dupeDeleteThresh = dupeDeleteThresh;
            this.dupeStrikeThresh = dupeStrikeThresh;
        }
        
        private AutomodSettings(ResultSet rs) throws SQLException
        {
            this(MAX_MENTIONS.getValue(rs), MAX_ROLE_MENTIONS.getValue(rs), RAIDMODE_NUMBER.getValue(rs), RAIDMODE_TIME.getValue(rs), 
                    INVITE_STRIKES.getValue(rs), REF_STRIKES.getValue(rs), DUPE_STRIKES.getValue(rs), DUPE_DELETE_THRESH.getValue(rs), 
                    DUPE_STRIKE_THRESH.getValue(rs));
        }
        
        public boolean useAutoRaidMode()
        {
            return raidmodeNumber>1 && raidmodeTime>1;
        }
        
        public boolean useAntiDuplicate()
        {
            return dupeStrikes>0 && dupeDeleteThresh>0 && dupeStrikeThresh>0;
        }
    }
}
