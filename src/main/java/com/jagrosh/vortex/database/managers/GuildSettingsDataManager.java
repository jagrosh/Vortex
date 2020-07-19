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
import com.jagrosh.jdautilities.command.GuildSettingsManager;
import com.jagrosh.jdautilities.command.GuildSettingsProvider;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FixedCache;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZoneId;
import java.time.zone.ZoneRulesException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Guild.VerificationLevel;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class GuildSettingsDataManager extends DataManager implements GuildSettingsManager
{
    public final static int PREFIX_MAX_LENGTH = 40;
    private static final String SETTINGS_TITLE = "\uD83D\uDCCA Server Settings"; // 📊
    private static final ZoneId DEFAULT_TIMEZONE = ZoneId.of("GMT-4");
    
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L,true);
    public final static SQLColumn<Long> MOD_ROLE_ID = new LongColumn("MOD_ROLE_ID",false,0L);
    
    public final static SQLColumn<Long> MODLOG_ID = new LongColumn("MODLOG_ID",false,0L);
    public final static SQLColumn<Long> SERVERLOG_ID = new LongColumn("SERVERLOG_ID",false,0L);
    public final static SQLColumn<Long> MESSAGELOG_ID = new LongColumn("MESSAGELOG_ID",false,0L);
    public final static SQLColumn<Long> VOICELOG_ID = new LongColumn("VOICELOG_ID",false,0L);
    public final static SQLColumn<Long> AVATARLOG_ID = new LongColumn("AVATARLOG_ID",false,0L);
    
    public final static SQLColumn<String> PREFIX = new StringColumn("PREFIX", true, null, PREFIX_MAX_LENGTH);
    public final static SQLColumn<String> TIMEZONE = new StringColumn("TIMEZONE",true,null,32);

    public final static SQLColumn<Integer> RAIDMODE = new IntegerColumn("RAIDMODE",false,-2); 
    // -2 = Raid Mode not activated
    // -1+ = Raid Mode active
    // level to set permission when finished
    
    // Cache
    private final FixedCache<Long, GuildSettings> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);
    private final GuildSettings blankSettings = new GuildSettings();
    
    public GuildSettingsDataManager(DatabaseConnector connector)
    {
        super(connector, "GUILD_SETTINGS");
    }
    
    // Getters
    @Override
    public GuildSettings getSettings(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
            return cache.get(guild.getIdLong());
        GuildSettings settings = read(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> rs.next() ? new GuildSettings(rs) : blankSettings);
        cache.put(guild.getIdLong(), settings);
        return settings;
    }
    
    public Field getSettingsDisplay(Guild guild)
    {
        GuildSettings settings = getSettings(guild);
        TextChannel modlog = settings.getModLogChannel(guild);
        TextChannel serverlog = settings.getServerLogChannel(guild);
        TextChannel messagelog = settings.getMessageLogChannel(guild);
        TextChannel voicelog = settings.getVoiceLogChannel(guild);
        TextChannel avylog = settings.getAvatarLogChannel(guild);
        Role modrole = settings.getModeratorRole(guild);
        Role muterole = settings.getMutedRole(guild);
        return new Field(SETTINGS_TITLE, "Prefix: `"+(settings.prefix==null ? Constants.PREFIX : settings.prefix)+"`"
                + "\nMod Role: "+(modrole==null ? "None" : modrole.getAsMention())
                + "\nMuted Role: "+(muterole==null ? "None" : muterole.getAsMention())
                + "\nMod Log: "+(modlog==null ? "None" : modlog.getAsMention())
                + "\nMessage Log: "+(messagelog==null ? "None" : messagelog.getAsMention())
                + "\nVoice Log: "+(voicelog==null ? "None" : voicelog.getAsMention())
                + "\nAvatar Log: "+(avylog==null ? "None" : avylog.getAsMention())
                + "\nServer Log: "+(serverlog==null ? "None" : serverlog.getAsMention())
                + "\nTimezone: **"+settings.timezone+"**\n\u200B", true);
    }
    
    public JSONObject getSettingsJson(Guild guild)
    {
        GuildSettings settings = getSettings(guild);
        return new JSONObject()
                .put("avatarlog", settings.avatarlog)
                .put("messagelog", settings.messagelog)
                .put("modRole", settings.modRole)
                .put("modlog", settings.modlog)
                .put("muteRole", settings.muteRole)
                .put("prefix", settings.prefix)
                .put("raidMode", settings.raidMode)
                .put("serverlog", settings.serverlog)
                .put("timezone", settings.timezone)
                .put("voicelog", settings.voicelog);
    }
    
    public boolean hasSettings(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getIdLong())), rs -> {return rs.next();});
    }
    
    // Setters
    public void setModLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MODLOG_ID), rs -> 
        {
            if(rs.next())
            {
                MODLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MODLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setServerLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, SERVERLOG_ID), rs -> 
        {
            if(rs.next())
            {
                SERVERLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                SERVERLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setMessageLogChannel(Guild guild, TextChannel tc)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MESSAGELOG_ID), rs -> 
        {
            if(rs.next())
            {
                MESSAGELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MESSAGELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setVoiceLogChannel(Guild guild, TextChannel tc)
    {
        setVoiceLogChannel(guild.getIdLong(), tc);
    }
    
    public void setVoiceLogChannel(long guildId, TextChannel tc)
    {
        invalidateCache(guildId);
        readWrite(select(GUILD_ID.is(guildId), GUILD_ID, VOICELOG_ID), rs -> 
        {
            if(rs.next())
            {
                VOICELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guildId);
                VOICELOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setAvatarLogChannel(Guild guild, TextChannel tc)
    {
        setAvatarLogChannel(guild.getIdLong(), tc);
    }
    
    public void setAvatarLogChannel(long guildId, TextChannel tc)
    {
        invalidateCache(guildId);
        readWrite(select(GUILD_ID.is(guildId), GUILD_ID, AVATARLOG_ID), rs -> 
        {
            if(rs.next())
            {
                AVATARLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guildId);
                AVATARLOG_ID.updateValue(rs, tc==null ? 0L : tc.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setModeratorRole(Guild guild, Role role)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, MOD_ROLE_ID), rs -> 
        {
            if(rs.next())
            {
                MOD_ROLE_ID.updateValue(rs, role==null ? 0L : role.getIdLong());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                MOD_ROLE_ID.updateValue(rs, role==null ? 0L : role.getIdLong());
                rs.insertRow();
            }
        });
    }
    
    public void setPrefix(Guild guild, String prefix)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, PREFIX), rs -> 
        {
            if(rs.next())
            {
                PREFIX.updateValue(rs, prefix);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                PREFIX.updateValue(rs, prefix);
                rs.insertRow();
            }
        });
    }
    
    public void setTimezone(Guild guild, ZoneId zone)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, TIMEZONE), rs -> 
        {
            if(rs.next())
            {
                TIMEZONE.updateValue(rs, zone.getId());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                TIMEZONE.updateValue(rs, zone.getId());
                rs.insertRow();
            }
        });
    }
    
    public void enableRaidMode(Guild guild)
    {
        invalidateCache(guild);
        readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, RAIDMODE), rs -> 
        {
            if(rs.next())
            {
                RAIDMODE.updateValue(rs, guild.getVerificationLevel().getKey());
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                RAIDMODE.updateValue(rs, guild.getVerificationLevel().getKey());
                rs.insertRow();
            }
        });
    }
    
    public VerificationLevel disableRaidMode(Guild guild)
    {
        invalidateCache(guild);
        return readWrite(select(GUILD_ID.is(guild.getIdLong()), GUILD_ID, RAIDMODE), rs -> 
        {
            VerificationLevel old = null;
            if(rs.next())
            {
                old = VerificationLevel.fromKey(RAIDMODE.getValue(rs));
                RAIDMODE.updateValue(rs, -2);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                RAIDMODE.updateValue(rs, -2);
                rs.insertRow();
            }
            return old;
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
    
    public class GuildSettings implements GuildSettingsProvider
    {
        private final long modRole, muteRole, modlog, serverlog, messagelog, voicelog, avatarlog;
        private final String prefix;
        private final ZoneId timezone;
        private final int raidMode;
        
        private GuildSettings()
        {
            this.modRole = 0;
            this.modlog = 0;
            this.muteRole = 0;
            this.serverlog = 0;
            this.messagelog = 0;
            this.voicelog = 0;
            this.avatarlog = 0;
            this.prefix = null;
            this.timezone = DEFAULT_TIMEZONE;
            this.raidMode = -2;
        }
        
        private GuildSettings(ResultSet rs) throws SQLException
        {
            this.modRole = MOD_ROLE_ID.getValue(rs);
            this.muteRole = 0;
            this.modlog = MODLOG_ID.getValue(rs);
            this.serverlog = SERVERLOG_ID.getValue(rs);
            this.messagelog = MESSAGELOG_ID.getValue(rs);
            this.voicelog = VOICELOG_ID.getValue(rs);
            this.avatarlog = AVATARLOG_ID.getValue(rs);
            this.prefix = PREFIX.getValue(rs);
            String str = TIMEZONE.getValue(rs);
            ZoneId zid;
            if(str == null)
                zid = DEFAULT_TIMEZONE;
            else try
            {
                zid = ZoneId.of(str);
            }
            catch(ZoneRulesException ex)
            {
                zid = DEFAULT_TIMEZONE;
            }
            this.timezone = zid;
            this.raidMode = RAIDMODE.getValue(rs);
        }
        
        public Role getModeratorRole(Guild guild)
        {
            return guild.getRoleById(modRole);
        }
        
        public Role getMutedRole(Guild guild)
        {
            Role rid = guild.getRoleById(muteRole);
            if(rid!=null)
                return rid;
            return guild.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("Muted")).findFirst().orElse(null);
        }
        
        public TextChannel getModLogChannel(Guild guild)
        {
            return guild.getTextChannelById(modlog);
        }
        
        public TextChannel getServerLogChannel(Guild guild)
        {
            return guild.getTextChannelById(serverlog);
        }
        
        public TextChannel getMessageLogChannel(Guild guild)
        {
            return guild.getTextChannelById(messagelog);
        }
        
        public TextChannel getVoiceLogChannel(Guild guild)
        {
            return guild.getTextChannelById(voicelog);
        }
        
        public TextChannel getAvatarLogChannel(Guild guild)
        {
            return guild.getTextChannelById(avatarlog);
        }
        
        public ZoneId getTimezone()
        {
            return timezone;
        }

        @Override
        public Collection<String> getPrefixes()
        {
            if(prefix==null || prefix.isEmpty())
                return null;
            return Collections.singleton(prefix);
        }
        
        public boolean isInRaidMode()
        {
            return raidMode != -2;
        }
    }
}
