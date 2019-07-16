package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.vortex.utils.FixedCache;
import net.dv8tion.jda.core.entities.Guild;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class InviteWhitelistManager extends DataManager
{

    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L);
    public static final SQLColumn<Long> WHITELIST_ID = new LongColumn("WL_ID", false, 0L);

    private final FixedCache<Long, List<Long>> cache = new FixedCache<>(1000);

    public InviteWhitelistManager(DatabaseConnector connector)
    {
        super(connector, "INVITE_WL");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+WHITELIST_ID;
    }

    public boolean addToWhitelist(Guild guild, long whitelistId)
    {
        invalidateCache(guild);
        return readWrite(selectAll(GUILD_ID.is(guild.getId()) + " AND " + WHITELIST_ID.is(whitelistId)), rs ->
        {
            if(rs.next())
                return false;
            rs.moveToInsertRow();
            GUILD_ID.updateValue(rs, guild.getIdLong());
            WHITELIST_ID.updateValue(rs, whitelistId);
            rs.insertRow();
            return true;
        });
    }

    public boolean removeFromWhitelist(Guild guild, long whitelistId)
    {
        invalidateCache(guild);
        return readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+WHITELIST_ID.is(whitelistId)), rs ->
        {
            if(rs.next())
            {
                rs.deleteRow();
                return true;
            }
            return false;
        });
    }

    public List<Long> readWhitelist(Guild guild)
    {
        if(cache.contains(guild.getIdLong()))
            return cache.get(guild.getIdLong());
        List<Long> whitelist = read(selectAll(GUILD_ID.is(guild.getId())), rs ->
        {
            List<Long> list = new LinkedList<>();
            while(rs.next())
                list.add(WHITELIST_ID.getValue(rs));
            return Collections.unmodifiableList(list);
        });
        cache.put(guild.getIdLong(), whitelist);
        return whitelist;
    }

    private void invalidateCache(Guild guild)
    {
        cache.pull(guild.getIdLong());
    }
}
