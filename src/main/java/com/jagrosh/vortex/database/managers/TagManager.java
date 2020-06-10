package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.LongColumn;
import com.jagrosh.easysql.columns.StringColumn;
import com.jagrosh.vortex.database.Database;
import net.dv8tion.jda.core.entities.Guild;

import java.util.ArrayList;
import java.util.List;

public class TagManager extends DataManager
{
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0);
    public static final SQLColumn<String> TAG_NAME = new StringColumn("TAG_NAME", false, "", 64);
    public static final SQLColumn<String> TAG_VALUE = new StringColumn("TAG_VALUE", false, "", 1999);

    public TagManager(DatabaseConnector connector)
    {
        super(connector, "TAGS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+TAG_NAME;
    }

    /**
     * Gets a tag
     * @param guild Guild of the tag
     * @param tagName Name of the tag
     * @return The tag value, or null if the tag doesn't exist or if the user isn't the proper level
     */
    public String getTagValue(Guild guild, String tagName)
    {
        tagName = Database.sanitise(tagName.toLowerCase());
        return read(selectAll(GUILD_ID.is(guild.getId())+" AND TAG_NAME = '"+tagName+"'"), rs ->
        {
            if (rs.next())
                return rs.getString("TAG_VALUE");
            return null;
        });
    }

    public void addTagValue(Guild guild, String tagName, final String TAG_VALUE)
    {
        final String TAG_NAME = Database.sanitise(tagName.toLowerCase());
        final long GUILD_ID = guild.getIdLong();
        readWrite(selectAll(TagManager.GUILD_ID.is(GUILD_ID)+" AND TAG_NAME = '"+TAG_NAME +"'"), rs ->
        {
            if(rs.next())
            {
                rs.updateString("TAG_VALUE", TAG_VALUE);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                TagManager.GUILD_ID.updateValue(rs, GUILD_ID);
                TagManager.TAG_NAME.updateValue(rs, TAG_NAME);
                TagManager.TAG_VALUE.updateValue(rs, TAG_VALUE);
                rs.insertRow();
            }
        });
    }

    /**
     * Deletes a tag
     * @param guild Guild of the tag
     * @param tagName Name of the tag
     * @return Returns true if the tag was deleted, false if no tag existed with that name
     */
    public boolean deleteTag(Guild guild, String tagName)
    {
        tagName = Database.sanitise(tagName.trim().toLowerCase());
        return readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND TAG_NAME = '"+tagName + "'"), rs ->
        {
            if(rs.next()) {
                rs.deleteRow();
                return true;
            }

            return false;
        });
    }

    public List<String> getTagNames(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())), rs ->
        {
            List<String> tagNames = new ArrayList<>();

            while (rs.next())
                tagNames.add(rs.getString("TAG_NAME"));

            return tagNames;
        });
    }
}
