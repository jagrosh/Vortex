package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FixedCache;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.json.JSONArray;

public class InviteWhitelistManager extends DataManager
{
    public static final int MAX_WHITELISTED_GUILDS = 80;
    
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L);
    public static final SQLColumn<Long> WHITELIST_ID = new LongColumn("WL_ID", false, 0L);

    private final FixedCache<Long, List<Long>> cache = new FixedCache<>(Constants.DEFAULT_CACHE_SIZE);

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
        if(readWhitelist(guild).size() + 1 > MAX_WHITELISTED_GUILDS)
            return false;
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

    public void addAllToWhitelist(Guild guild, Collection<Long> whitelistIds)
    {
        if(readWhitelist(guild).size() + whitelistIds.size() > MAX_WHITELISTED_GUILDS)
            return;
        invalidateCache(guild);
        Set<Long> ids = new HashSet<>(whitelistIds);
        readWrite(selectAll(String.format("%s AND %s IN (%s)",
                GUILD_ID.is(guild.getId()), WHITELIST_ID.name,
                ids.stream().map(String::valueOf).collect(Collectors.joining(",")))),rs ->
        {
            while(rs.next())
            {
                ids.remove(WHITELIST_ID.getValue(rs));
            }
            for(long id : ids)
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                WHITELIST_ID.updateValue(rs, id);
                rs.insertRow();
            }
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

    public void removeAllFromWhitelist(Guild guild, Collection<Long> whitelistIds)
    {
        invalidateCache(guild);
        try
        {
            PreparedStatement stmt = getConnection().prepareStatement("DELETE FROM " + getTableName() + " WHERE " +
                    GUILD_ID.name + " = ? AND " + WHITELIST_ID.name + " IN (" +
                    IntStream.range(0, whitelistIds.size()).mapToObj(i -> "?").collect(Collectors.joining(",")) + ')');
            int paramIndex = 0;
            stmt.setLong(++paramIndex, guild.getIdLong());
            for(Long whitelistId : whitelistIds)
            {
                stmt.setLong(++paramIndex, whitelistId);
            }
            stmt.executeUpdate();
        }
        catch(SQLException e)
        {
            LoggerFactory.getLogger(DatabaseConnector.class).error("Exception in SQL: "+e);
        }
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
    
    public JSONArray getWhitelistJson(Guild guild)
    {
        List<Long> list = readWhitelist(guild);
        JSONArray arr = new JSONArray();
        list.forEach(id -> arr.put(id));
        return arr;
    }

    private void invalidateCache(Guild guild)
    {
        cache.pull(guild.getIdLong());
    }
}
