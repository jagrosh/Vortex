package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.InstantColumn;
import com.jagrosh.easysql.columns.IntegerColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.easysql.columns.StringColumn;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.database.Database.Modlog;
import net.dv8tion.jda.api.entities.Guild;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class KickingManager extends DataManager implements ModlogManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<Long> MOD_ID = new LongColumn("MOD_ID", false, 0);
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);
    public static final SQLColumn<String> REASON = new StringColumn("REASON", false, "", 2000);
    public static final SQLColumn<Integer> CASE_ID = new IntegerColumn("CASE_ID", false, 0);
    public static final SQLColumn<Instant> TIME = new InstantColumn("TIME", false, Instant.EPOCH);

    public KickingManager(Database connector)
    {
        super(connector, "KICKS");
    }

    public void logCase(Vortex vortex, Guild guild, long modId, long userId, String reason)
    {
        readWrite(selectAll(), rs ->
        {
            Instant now = Instant.now();
            int id = Database.genNewId(guild.getIdLong());
            rs.moveToInsertRow();
            GUILD_ID.updateValue(rs, guild.getIdLong());
            MOD_ID.updateValue(rs, modId);
            USER_ID.updateValue(rs, userId);
            CASE_ID.updateValue(rs, id);
            REASON.updateValue(rs, Database.sanitise(reason));
            TIME.updateValue(rs, now);
            rs.insertRow();
            vortex.getBasicLogger().logModlog(guild, new Modlog(userId, modId, Action.KICK, id, reason, now));
        });
    }

    public int getMaxId(long guildId) {
        String query = selectAll(GUILD_ID.is(guildId)+" ORDER BY CASE_ID DESC NULLS LAST");
        return read(query, rs -> rs.next() ? rs.getInt("CASE_ID") : -1);
    }

    public String updateReason(long guildId, int id, String reason)
    {
        return readWrite(selectAll(CASE_ID.is(id)+" AND "+GUILD_ID.is(guildId)), rs ->
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
    protected final String primaryKey()
    {
        return GUILD_ID+", "+ CASE_ID;
    }

    @Override
    public final Modlog deleteCase(long guildId, int id)
    {
        return readWrite(selectAll(GUILD_ID.is(guildId)+" AND "+ CASE_ID.is(id)), rs -> {
            long modId = rs.getLong("MOD_ID");
            String reason = rs.getString("REASON");
            Modlog modlog = new Modlog(USER_ID.getValue(rs), modId, Action.KICK, id, reason, TIME.getValue(rs));
            rs.deleteRow();
            return modlog;
        });
    }

    public List<Modlog> getModlogs(long guildId, long userId)
    {
        String query = selectAll(GUILD_ID.is(guildId)+" AND "+USER_ID.is(userId));
        return read(query, rs ->
        {
            List<Modlog> modlogs = new ArrayList<>();
            while (rs.next()) {
                long modId = rs.getLong("MOD_ID");
                int id = rs.getInt("CASE_ID");
                String reason = rs.getString("REASON");
                modlogs.add(new Modlog(userId, modId, Action.KICK, id, reason, TIME.getValue(rs)));
            }
            return modlogs;
        });
    }
}