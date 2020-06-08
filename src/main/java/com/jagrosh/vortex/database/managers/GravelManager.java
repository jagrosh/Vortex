package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.InstantColumn;
import com.jagrosh.easysql.columns.LongColumn;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;

public class GravelManager extends DataManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<Long> USER_ID = new LongColumn("USER_ID", false, 0);

    public GravelManager(DatabaseConnector connector)
    {
        super(connector, "GRAVELS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+USER_ID;
    }

    public boolean isGraveled(Member member)
    {
        return read(selectAll(GUILD_ID.is(member.getGuild().getId())+" AND "+USER_ID.is(member.getUser().getId())),
                ResultSet::next);
    }

    public void gravel(Guild guild, long userId)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs ->
        {
            if(rs.next())
            {
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                USER_ID.updateValue(rs, userId);
                rs.insertRow();
            }
        });
    }

    public void ungravel(Guild guild, long userId)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+USER_ID.is(userId)), rs ->
        {
            if(rs.next())
                rs.deleteRow();
        });
    }
}
