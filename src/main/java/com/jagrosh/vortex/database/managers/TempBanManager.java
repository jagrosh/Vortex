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
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.User;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class TempBanManager extends DataManager implements ModlogManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<Instant> FINISH = new InstantColumn("FINISH", false, Instant.EPOCH);
    public static final SQLColumn<Instant> START = new InstantColumn("START", false, Instant.EPOCH);
    public static final SQLColumn<Boolean> IS_BANNED = new BooleanColumn("IS_BANNED", false, true);
    public static final SQLColumn<String> REASON = new StringColumn("REASON", false, "", 2000);
    public static final SQLColumn<Integer> CASE_ID = new IntegerColumn("CASE_ID", false, 0);
    public static final SQLColumn<Long> MOD_ID = new LongColumn("MOD_ID", false, 0);
    public static final SQLColumn<Long> SAVIOR_ID = new LongColumn("SAVIOR_ID", false, -1);

    public TempBanManager(DatabaseConnector connector)
    {
        super(connector, "TEMP_BANS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+CASE_ID;
    }

    public synchronized void setBan(Vortex vortex, Guild guild, long userId, long modId, Instant finish, String reason)
    {
        Instant now = Instant.now();
        if (finish.getEpochSecond() == now.getEpochSecond()) {
            setSoftBan(vortex, guild, userId, modId, reason);
            return;
        }

        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs ->
        {
            if(rs.next())
            {
                FINISH.updateValue(rs, finish);
                int caseId = CASE_ID.getValue(rs);
                rs.updateRow();
                vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, reason, Instant.now()));
            }
            else
            {
                rs.moveToInsertRow();
                int caseId = Database.genNewId(guild.getIdLong());
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                MOD_ID.updateValue(rs, modId);
                IS_BANNED.updateValue(rs, true);
                CASE_ID.updateValue(rs, caseId);
                REASON.updateValue(rs, Database.sanitise(reason));
                START.updateValue(rs, now);
                FINISH.updateValue(rs, finish);
                vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, null, Instant.now()));
                rs.insertRow();
            }
        });
    }

    public synchronized void setSoftBan(Vortex vortex, Guild guild, long userId, long modId, String reason)
    {
        readWrite(selectAll(), rs -> {
            Instant now = Instant.now();

            rs.moveToInsertRow();
            int caseId = Database.genNewId(guild.getIdLong());
            GUILD_ID.updateValue(rs, guild.getIdLong());
            USER_ID.updateValue(rs, userId);
            MOD_ID.updateValue(rs, modId);
            IS_BANNED.updateValue(rs, false);
            CASE_ID.updateValue(rs, caseId);
            FINISH.updateValue(rs, now);
            REASON.updateValue(rs, Database.sanitise(reason));
            START.updateValue(rs, now);
            vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, 0, Action.BAN, caseId, null, Instant.now()));
            rs.insertRow();
        });
    }

    public synchronized void clearBan(Vortex vortex, Guild guild, long userId, long saviorId)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs ->
        {
            if(rs.next())
            {
                Instant now = Instant.now();
                Instant start = START.getValue(rs);
                int caseId = CASE_ID.getValue(rs);
                long modId = MOD_ID.getValue(rs);
                String reason = REASON.getValue(rs);
                SAVIOR_ID.updateValue(rs, saviorId);
                FINISH.updateValue(rs, now);
                IS_BANNED.updateValue(rs, false);
                rs.updateRow();
                vortex.getBasicLogger().logModlog(guild, new Database.Modlog(userId, modId, Action.BAN, caseId, reason, now, start, saviorId));
            }
        });
    }

    public int timeUntilUnban(Guild guild, long userId)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)+" AND IS_BANNED=TRUE"), rs ->
        {
            if(rs.next())
            {
                Instant end = FINISH.getValue(rs);
                if(end==Instant.MAX)
                    return Integer.MAX_VALUE;
                else
                    return (int)(Instant.now().until(end, ChronoUnit.MINUTES));
            }
            return 0;
        });
    }

    public void checkUnbans(Vortex vortex, JDA jda)
    {
        readWrite(selectAll(FINISH.isLessThan(Instant.now().getEpochSecond())+" AND IS_BANNED=TRUE"), rs ->
        {
            while(rs.next())
            {
                Guild g = jda.getGuildById(GUILD_ID.getValue(rs));
                if(g==null || jda.isUnavailable(g.getIdLong()) || !g.getSelfMember().hasPermission(Permission.BAN_MEMBERS))
                    continue;
                g.unban(User.fromId(USER_ID.getValue(rs))).reason("Temporary Ban Completed").queue(s->{}, f->{});
                Instant now = Instant.now();
                long saviorId = SAVIOR_ID.getValue(rs);
                String reason = REASON.getValue(rs);
                Instant start = START.getValue(rs);
                long userId = USER_ID.getValue(rs);
                int caseId = CASE_ID.getValue(rs);
                long modId = MOD_ID.getValue(rs);
                rs.deleteRow();
                vortex.getBasicLogger().logModlog(g, new Database.Modlog(userId, modId, Action.BAN, caseId, reason, now, start, saviorId));
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
    public Database.Modlog deleteCase(long guildId, int caseId) {
        return readWrite(selectAll(GUILD_ID.is(guildId)+" AND "+CASE_ID.is(caseId)), rs ->
        {
            if (rs.next())
            {
                if (IS_BANNED.getValue(rs))
                    return null;
                Database.Modlog modlog;
                long modId = rs.getLong("MOD_ID");
                String reason = rs.getString("REASON");
                Instant finish = FINISH.getValue(rs), start = START.getValue(rs);
                if (start.getEpochSecond() == finish.getEpochSecond())
                    modlog = new Database.Modlog(USER_ID.getValue(rs), modId, Action.SOFTBAN, caseId, reason, start);
                else
                    modlog = new Database.Modlog(USER_ID.getValue(rs), modId, Action.BAN, caseId, reason, finish, start, SAVIOR_ID.getValue(rs));
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
            List<Database.Modlog> modlogs = new ArrayList<>();
            while (rs.next()) {
                long modId = rs.getLong("MOD_ID");
                int id = rs.getInt("CASE_ID");
                String reason = rs.getString("REASON");
                Instant finish = FINISH.getValue(rs), start = START.getValue(rs);
                if (start.getEpochSecond() == finish.getEpochSecond())
                    modlogs.add(new Database.Modlog(userId, modId, Action.SOFTBAN, id, reason, start));
                else
                    modlogs.add(new Database.Modlog(userId, modId, Action.BAN, id, reason, finish, start, SAVIOR_ID.getValue(rs)));
            }
            return modlogs;
        });
    }
}
