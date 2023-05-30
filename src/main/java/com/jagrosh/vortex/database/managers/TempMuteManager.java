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

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.database.Database.Modlog;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TempMuteManager extends DataManager implements ModlogManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<Instant> FINISH = new InstantColumn("FINISH", false, Instant.EPOCH);
    public static final SQLColumn<Instant> START = new InstantColumn("START", false, Instant.EPOCH);
    public static final SQLColumn<Boolean> IS_MUTED = new BooleanColumn("IS_MUTED", false, true);
    public static final SQLColumn<String> REASON = new StringColumn("REASON", false, "", 2000);
    public static final SQLColumn<Integer> CASE_ID = new IntegerColumn("CASE_ID", false, 0);
    public static final SQLColumn<Long> MOD_ID = new LongColumn("MOD_ID", false, 0);
    public static final SQLColumn<Long> SAVIOR_ID = new LongColumn("SAVIOR_ID", false, -1);

    public TempMuteManager(DatabaseConnector connector)
    {
        super(connector, "TEMP_MUTES");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+ CASE_ID;
    }
    
    public boolean isMuted(Member member)
    {
        return read(selectAll(GUILD_ID.is(member.getGuild().getId())+" AND "+USER_ID.is(member.getUser().getId())+" AND IS_MUTED=TRUE"),
                ResultSet::next);
    }

    public synchronized void setMute(Vortex vortex, Guild guild, long userId, Instant finish)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" AND IS_MUTED=TRUE"), rs ->
        {
            if(rs.next())
            {
                if(FINISH.getValue(rs).isBefore(finish))
                {
                    int caseId = CASE_ID.getValue(rs);
                    FINISH.updateValue(rs, finish);
                    vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, "", Instant.now()));
                    rs.updateRow();
                }
            }
            else
            {
                rs.moveToInsertRow();
                int caseId = Database.genNewId(guild.getIdLong());
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                MOD_ID.updateValue(rs, 0L);
                IS_MUTED.updateValue(rs, true);
                CASE_ID.updateValue(rs, caseId);
                FINISH.updateValue(rs, finish);
                START.updateValue(rs, Instant.now());
                vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, "", Instant.now()));
                rs.insertRow();
            }
        });
    }
    
    public synchronized void overrideMute(Guild guild, long userId, long modId, Instant finish, String reason)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" AND IS_MUTED=TRUE"), rs ->
        {
            if(rs.next())
            {
                int caseId = CASE_ID.getValue(rs);
                FINISH.updateValue(rs, finish);
                //TODO: vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, "", Instant.now()));
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                MOD_ID.updateValue(rs, modId);
                FINISH.updateValue(rs, finish);
                REASON.updateValue(rs, reason);
                CASE_ID.updateValue(rs, Database.genNewId(guild.getIdLong()));
                IS_MUTED.updateValue(rs, true);
                START.updateValue(rs, Instant.now());
                rs.insertRow();
            }
        });
    }

    public synchronized void removeMute(Guild guild, long userId, long saviorId)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" AND IS_MUTED=TRUE"), rs ->
        {
            if(rs.next())
            {
                SAVIOR_ID.updateValue(rs, saviorId);
                FINISH.updateValue(rs, Instant.now().minusSeconds(1));
                IS_MUTED.updateValue(rs, false);
                rs.updateRow();
                return;
            }

            if (saviorId == -2)
                return;

            String q2 = GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" ORDER BY CASE_ID DESC NULLS LAST";
            readWrite(selectAll(q2), rs2 -> {
                if (rs2.next() && SAVIOR_ID.getValue(rs2) == -2)
                {
                    SAVIOR_ID.updateValue(rs2, saviorId);
                    rs2.updateRow();
                }
            });
        });
    }
    
    public int timeUntilUnmute(Guild guild, long userId)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" AND IS_MUTED=TRUE"), rs ->
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
        readWrite(selectAll(FINISH.isLessThan(Instant.now().getEpochSecond())+" AND IS_MUTED=TRUE"), rs ->
        {
            while(rs.next())
            {
                // TODO: Do we really want g.getMemberCache().isEmpty()?
                Guild g = jda.getGuildById(GUILD_ID.getValue(rs));
                if(g==null || jda.isUnavailable(g.getIdLong()) || !g.getSelfMember().hasPermission(Permission.MANAGE_ROLES))
                    continue;
                Role mRole = data.getSettings(g).getMutedRole(g);
                if(mRole==null || !g.getSelfMember().canInteract(mRole))
                {
                    continue;
                }
                Member m = g.getMemberById(USER_ID.getValue(rs));
                if(m!=null && m.getRoles().contains(mRole))
                    g.removeRoleFromMember(m, mRole).reason("Temporary Mute Completed").queue();
                FINISH.updateValue(rs, Instant.now().minusSeconds(1));
                IS_MUTED.updateValue(rs, false);
                rs.updateRow();
            }
        });
    }

    @Override
    public int getMaxId(long guildId) {
        String query = selectAll(GUILD_ID.is(guildId)+" ORDER BY CASE_ID DESC NULLS LAST");
        return read(query, rs -> rs.next() ? rs.getInt("CASE_ID") : -1);
    }

    @Override
    public String updateReason(long guildId, int caseId, String reason) {
        return readWrite(selectAll(CASE_ID.is(caseId)+" AND "+GUILD_ID.is(guildId)), rs ->
        {
            if (rs.next())
            {
                String oldReason = REASON.getValue(rs);
                rs.updateString("REASON", Database.sanitise(reason));
                rs.updateRow();
                return oldReason == null ? "" : oldReason;
            }

            return null;
        });
    }

    @Override
    public Modlog deleteCase(long guildId, int caseId) {
        return readWrite(selectAll(GUILD_ID.is(guildId)+" AND "+CASE_ID.is(caseId)), rs ->
        {
            if (rs.next())
            {
                if (IS_MUTED.getValue(rs))
                    return null;

                long modId = rs.getLong("MOD_ID");
                String reason = rs.getString("REASON");
                Modlog modlog = new Modlog(USER_ID.getValue(rs), modId, Action.MUTE, caseId, reason, FINISH.getValue(rs), START.getValue(rs), SAVIOR_ID.getValue(rs));
                rs.deleteRow();
                return modlog;
            }

            return null;
        });
    }

    @Override
    public List<Database.Modlog> getModlogs(long guildId, long userId) {
        String query = selectAll(GUILD_ID.is(guildId)+" AND "+USER_ID.is(userId));
        return read(query, rs ->
        {
            List<Modlog> modlogs = new ArrayList<>();
            while (rs.next()) {
                long modId = rs.getLong("MOD_ID");
                int id = rs.getInt("CASE_ID");
                String reason = rs.getString("REASON");
                modlogs.add(new Modlog(userId, modId, Action.MUTE, id, reason, FINISH.getValue(rs), START.getValue(rs), SAVIOR_ID.getValue(rs)));
            }
            return modlogs;
        });
    }
}