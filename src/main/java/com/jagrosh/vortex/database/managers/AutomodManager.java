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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FixedCache;
import java.sql.ResultSet;
import java.sql.SQLException;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AutomodManager extends DataManager
{
    public final static int MENTION_MINIMUM = 4;
    public final static int ROLE_MENTION_MINIMUM = 2;
    private static final String SETTINGS_TITLE = "\uD83D\uDEE1 Automod Settings"; // 🛡
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L,true);
    
    public final static SQLColumn<Boolean> RESOLVE_URLS = new BooleanColumn("RESOLVE_URLS", false, false);
    
    public final static SQLColumn<Integer> MAX_MENTIONS = new IntegerColumn("MAX_MENTIONS", false, 0);
    public final static SQLColumn<Integer> MAX_ROLE_MENTIONS = new IntegerColumn("MAX_ROLE_MENTIONS", false, 0);
    public final static SQLColumn<Integer> MAX_LINES = new IntegerColumn("MAX_LINES", false, 0);
    
    public final static SQLColumn<Integer> RAIDMODE_NUMBER = new IntegerColumn("RAIDMODE_NUMBER", false, 0);
    public final static SQLColumn<Integer> RAIDMODE_TIME = new IntegerColumn("RAIDMODE_TIME", false, 0);
    
    public final static SQLColumn<Boolean> FILTER_INVITES = new BooleanColumn("FILTER_INVITES", false, true);
    public final static SQLColumn<Boolean> FILTER_REFS = new BooleanColumn("REF_STRIKES", false, true);
    public final static SQLColumn<Boolean> FILTER_COPYPASTAS = new BooleanColumn("FILTER_COPYPASTAS", false, true);
    public final static SQLColumn<Integer> DUPE_DELETE_THRESH = new IntegerColumn("DUPE_DELETE_THRESH", false, 0);
    public final static SQLColumn<Integer> DEHOIST_CHAR = new IntegerColumn("DEHOIST_CHAR", false, 0);
            
    // Cache
    private final FixedCache<Long, AutomodSettings> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
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
                  "__Anti-Advertisement__\n" + (!settings.filterInvites && !settings.filterRefs
                    ? "Disabled\n\n"
                    : "Invite Filter: `" + (settings.filterInvites ? "ON" : "OFF") + "`\n" +
                      "Referral Link Filter: `" + (settings.filterRefs ? "ON" : "OFF") + "`\n" +
                      "Resolve Links: `" + (settings.resolveUrls ? "ON" : "OFF") + "`\n\n")
                + "__Anti-Duplicate__\n" + (settings.useAntiDuplicate() ?
                     "Delete Threshold: `" + settings.dupeDeleteThresh + "`\n\n" : "Disabled\n\n")
                + "__Maximum Mentions__\n" + (settings.maxMentions==0 && settings.maxRoleMentions==0 
                    ? "Disabled\n\n" 
                    : "User Mentions: " + (settings.maxMentions==0 ? "None\n" : "`" + settings.maxMentions + "`\n") +
                      "Role Mentions: " + (settings.maxRoleMentions==0 ? "None\n\n" : "`" + settings.maxRoleMentions + "`\n\n"))
                + "__Misc Msg Settings__\n" + (settings.maxLines==0 && !settings.filterCopypastas
                    ? "Disabled\n\n"
                    : "Max Lines / Msg: " + (settings.maxLines==0 ? "Disabled\n" : "`"+settings.maxLines+"`\n") +
                      "Copypasta: `" + settings.filterCopypastas + "`\n")
                + "__Miscellaneous__\n"
                    + "Auto AntiRaid: " + (settings.useAutoRaidMode() 
                        ? "`" + settings.raidmodeNumber + "` joins/`" + settings.raidmodeTime + "`s\n" 
                        : "Disabled\n")
                    + "Auto Dehoist: " + (settings.dehoistChar==(char)0 
                        ? "Disabled" 
                        : "`"+settings.dehoistChar+"` and above")
                /*+ "\u200B"*/, true);
    }
    
    public JSONObject getSettingsJson(Guild guild)
    {
        AutomodSettings settings = getSettings(guild);
        return new JSONObject()
                .put("filterCopypastas", settings.filterCopypastas)
                .put("dehoistChar", ""+settings.dehoistChar)
                .put("dupeDeleteThresh", settings.dupeDeleteThresh)
                .put("filterInvites", settings.filterInvites)
                .put("maxLines", settings.maxLines)
                .put("maxMentions", settings.maxMentions)
                .put("maxRoleMentions", settings.maxRoleMentions)
                .put("raidmodeNumber", settings.raidmodeNumber)
                .put("raidmodeTime", settings.raidmodeTime)
                .put("filterRefs", settings.filterRefs)
                .put("resolveUrls", settings.resolveUrls);
    }
    
    public boolean hasSettings(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getIdLong())), ResultSet::next);
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
    
    public void setResolveUrls(Guild guild, boolean value)
    {
        setResolveUrls(guild.getIdLong(), value);
    }
    
    public void setResolveUrls(long guildId, boolean value)
    {
        invalidateCache(guildId);
        readWrite(selectAll(GUILD_ID.is(guildId)), rs ->
        {
            if(rs.next())
            {
                RESOLVE_URLS.updateValue(rs, value);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guildId);
                RESOLVE_URLS.updateValue(rs, value);
                rs.insertRow();
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
    
    public void setMaxLines(Guild guild, int max)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                MAX_LINES.updateValue(rs, max);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MAX_LINES.updateValue(rs, max);
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
    
    public void enableInviteFilter(Guild guild, boolean enabled)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                FILTER_INVITES.updateValue(rs, enabled);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                FILTER_INVITES.updateValue(rs, enabled);
                rs.insertRow();
            }
        });
    }
    
    public void enableReferalFilter(Guild guild, boolean enabled)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                FILTER_REFS.updateValue(rs, enabled);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                FILTER_REFS.updateValue(rs, enabled);
                rs.insertRow();
            }
        });
    }
    
    public void enableCopypastaFilter(Guild guild, boolean enabled)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                FILTER_COPYPASTAS.updateValue(rs, enabled);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                FILTER_COPYPASTAS.updateValue(rs, enabled);
                rs.insertRow();
            }
        });
    }

    
    public void setDupeThresh(Guild guild, int deleteThresh)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                DUPE_DELETE_THRESH.updateValue(rs, deleteThresh);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                DUPE_DELETE_THRESH.updateValue(rs, deleteThresh);
                rs.insertRow();
            }
        });
    }
    
    public void setDehoistChar(Guild guild, char dehoistChar)
    {
        invalidateCache(guild);
        readWrite(selectAll(GUILD_ID.is(guild.getIdLong())), rs ->
        {
            if(rs.next())
            {
                DEHOIST_CHAR.updateValue(rs, (int)dehoistChar);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                DEHOIST_CHAR.updateValue(rs, (int)dehoistChar);
                rs.insertRow();
            }
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
    
    public static class AutomodSettings
    {
        public final boolean resolveUrls, filterInvites, filterRefs, filterCopypastas;
        public final int maxMentions, maxRoleMentions;
        public final int maxLines;
        public final int raidmodeNumber, raidmodeTime;
        public final int dupeDeleteThresh;
        public final char dehoistChar;
        
        private AutomodSettings()
        {
            this.filterRefs = true;
            this.filterCopypastas = true;
            this.resolveUrls = false;
            this.filterInvites = true;
            this.maxMentions = 0;
            this.maxRoleMentions = 0;
            this.maxLines = 0;
            this.raidmodeNumber = 0;
            this.raidmodeTime = 0;
            this.dupeDeleteThresh = 0;
            this.dehoistChar = 0;
        }
        
        private AutomodSettings(ResultSet rs) throws SQLException
        {
            this.filterRefs = FILTER_REFS.getValue(rs);
            this.resolveUrls = RESOLVE_URLS.getValue(rs);
            this.maxMentions = MAX_MENTIONS.getValue(rs);
            this.maxRoleMentions = MAX_ROLE_MENTIONS.getValue(rs);
            this.maxLines = MAX_LINES.getValue(rs);
            this.raidmodeNumber = RAIDMODE_NUMBER.getValue(rs);
            this.raidmodeTime = RAIDMODE_TIME.getValue(rs);
            this.filterInvites = FILTER_INVITES.getValue(rs);
            this.dupeDeleteThresh = DUPE_DELETE_THRESH.getValue(rs);
            this.dehoistChar = (char)((int)DEHOIST_CHAR.getValue(rs));
            this.filterCopypastas = FILTER_COPYPASTAS.getValue(rs);
        }
        
        public boolean useAutoRaidMode()
        {
            return raidmodeNumber>1 && raidmodeTime>1;
        }
        
        public boolean useAntiDuplicate()
        {
            return dupeDeleteThresh != 0;
        }
    }
}