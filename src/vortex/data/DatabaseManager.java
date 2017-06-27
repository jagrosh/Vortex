/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vortex.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.Action;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class DatabaseManager {
    
    private final Connection connection;
    private final SimpleLog LOG = SimpleLog.getLog("SQL");
    private final GuildSettings DEFAULT = new GuildSettings((short)0, (short)0, Action.NONE.getLetter(), Action.NONE.getLetter(), "0", "0", false, false);
    
    public DatabaseManager (String host, String user, String pass) throws SQLException
    {
        connection = DriverManager.getConnection(host, user, pass);
    }
    
    public void startupCheck()
    {
        try {
            if(!connection.getMetaData().getColumns(null, null, "GUILD_SETTINGS", "AUTO_RAID_MODE").next())
            {
                LOG.info("Creating column 'auto_raid_mode`");
                Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                statement.closeOnCompletion();
                statement.execute( "ALTER TABLE GUILD_SETTINGS\n" +
                                    "ADD COLUMN auto_raid_mode BOOLEAN NOT NULL WITH DEFAULT FALSE");
            }
            if(!connection.getMetaData().getColumns(null, null, "GUILD_SETTINGS", "BLOCK_DM_SPAM").next())
            {
                LOG.info("Creating column 'block_dm_spam`");
                Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
                statement.closeOnCompletion();
                statement.execute( "ALTER TABLE GUILD_SETTINGS\n" +
                                    "ADD COLUMN block_dm_spam BOOLEAN NOT NULL WITH DEFAULT FALSE");
            }
        } catch( SQLException e) {
            LOG.fatal(e);
        }
    }
    
    public GuildSettings getSettings(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            GuildSettings gs;
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM GUILD_SETTINGS WHERE GUILD_ID = %s", guild.getId())))
            {
                if(results.next())
                {
                    gs = new GuildSettings(results.getShort("max_mentions"),
                            results.getShort("spam_limit"),
                            results.getString("spam_action"),
                            results.getString("invite_action"),
                            Long.toString(results.getLong("mod_role_id")),
                            Long.toString(results.getLong("modlog_channel_id")),
                            results.getBoolean("auto_raid_mode"),
                            results.getBoolean("block_dm_spam"));
                }
                else gs = DEFAULT;
            }
            return gs;
        } catch( SQLException e) {
            LOG.warn(e);
            return DEFAULT;
        }
    }
    
    public boolean hasSettings(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            GuildSettings gs;
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM GUILD_SETTINGS WHERE GUILD_ID = %s", guild.getId())))
            {
                return results.next();
            }
        } catch( SQLException e) {
            LOG.warn(e);
            return false;
        }
    }
    
    public TextChannel getModlogChannel(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            TextChannel tc;
            try (ResultSet results = statement.executeQuery(String.format("SELECT modlog_channel_id FROM GUILD_SETTINGS WHERE GUILD_ID = %s", guild.getId())))
            {
                if(results.next())
                {
                    tc = guild.getTextChannelById(Long.toString(results.getLong("modlog_channel_id")));
                }
                else tc=null;
            }
            return tc;
        } catch( SQLException e) {
            LOG.warn(e);
            return null;
        }
    }
    
    public Set<Role> getIgnoredRoles(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            Set<Role> roles;
            try (ResultSet results = statement.executeQuery(String.format("SELECT entity_id FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'R'", guild.getId())))
            {
                roles = new HashSet<>();
                while(results.next())
                {
                    Role role = guild.getRoleById(results.getLong("entity_id"));
                    if(role!=null)
                        roles.add(role);
                }
            }
            return roles;
        } catch( SQLException e) {
            LOG.warn(e);
            return null;
        }
    }
    
    public Set<TextChannel> getIgnoredChannels(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            Set<TextChannel> channels;
            try (ResultSet results = statement.executeQuery(String.format("SELECT entity_id FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'C'", guild.getId())))
            {
                channels = new HashSet<>();
                while(results.next())
                {
                    TextChannel channel = guild.getTextChannelById(results.getLong("entity_id"));
                    if(channel!=null)
                        channels.add(channel);
                }
            }
            return channels;
        } catch( SQLException e) {
            LOG.warn(e);
            return null;
        }
    }
    
    public boolean isAutoRaidMode(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT auto_raid_mode FROM GUILD_SETTINGS WHERE GUILD_ID = %s", guild.getId())))
            {
                if(results.next())
                {
                    return results.getBoolean("auto_raid_mode");
                }
            }
            return false;
        } catch( SQLException e) {
            LOG.warn(e);
            return false;
        }
    }
    
    public boolean isDMSpamPrevention(Guild guild)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT block_dm_spam FROM GUILD_SETTINGS WHERE GUILD_ID = %s", guild.getId())))
            {
                if(results.next())
                {
                    return results.getBoolean("block_dm_spam");
                }
            }
            return false;
        } catch( SQLException e) {
            LOG.warn(e);
            return false;
        }
    }
    
    public List<Guild> getDMSpamPreventionGuilds(JDA jda)
    {
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery("SELECT GUILD_ID FROM GUILD_SETTINGS WHERE block_dm_spam = TRUE"))
            {
                List<Guild> guilds = new LinkedList<>();
                while(results.next())
                {
                    Guild g = jda.getGuildById(results.getLong("guild_id"));
                    if(g!=null)
                        guilds.add(g);
                }
                return guilds;
            }
        } catch( SQLException e) {
            LOG.warn(e);
            return Collections.EMPTY_LIST;
        }
    }
    
    public boolean isIgnored(Member member)
    {
        Set<Role> ignored = getIgnoredRoles(member.getGuild());
        if(ignored==null || ignored.isEmpty())
            return false;
        return member.getRoles().stream().anyMatch(role -> ignored.contains(role));
    }
    
    public boolean isIgnored(TextChannel channel)
    {
        if(channel==null)
            return false;
        try {
            Statement statement = connection.createStatement();
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT * FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'C' AND ENTITY_ID = %s", channel.getGuild().getId(), channel.getId())))
            {
                return results.next();
            }
        } catch( SQLException e) {
            LOG.warn(e);
            return false;
        }
    }
    
    public void addIgnore(TextChannel tc)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'C' AND entity_id = %s", tc.getGuild().getId(), tc.getId())))
            {
                if(!results.next())
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", tc.getGuild().getIdLong());
                    results.updateString("type", "C");
                    results.updateLong("entity_id", tc.getIdLong());
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void addIgnore(Role role)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'R' AND entity_id = %s", role.getGuild().getId(), role.getId())))
            {
                if(!results.next())
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", Long.parseLong(role.getGuild().getId()));
                    results.updateString("type", "R");
                    results.updateLong("entity_id", Long.parseLong(role.getId()));
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public boolean removeIgnore(TextChannel tc)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'C' AND entity_id = %s", tc.getGuild().getId(), tc.getId())))
            {
                if(results.next())
                {
                    results.deleteRow();
                    return true;
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
        return false;
    }
    
    public boolean removeIgnore(Role role)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try (ResultSet results = statement.executeQuery(String.format("SELECT * FROM IGNORED_ENTITIES WHERE GUILD_ID = %s AND TYPE = 'R' AND entity_id = %s", role.getGuild().getId(), role.getId())))
            {
                if(results.next())
                {
                    results.deleteRow();
                    return true;
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
        return false;
    }
    
    public void setMaxMentions(Guild guild, short maxMentions)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, max_mentions FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateShort("max_mentions", maxMentions);
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateShort("max_mentions", maxMentions);
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setSpam(Guild guild, Action spamAction, short spamLimit)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, spam_limit, spam_action FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateShort("spam_limit", spamLimit);
                    results.updateString("spam_action", spamAction.getLetter());
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateShort("spam_limit", spamLimit);
                    results.updateString("spam_action", spamAction.getLetter());
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setInviteAction(Guild guild, Action inviteAction)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, invite_action FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateString("invite_action", inviteAction.getLetter());
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateString("invite_action", inviteAction.getLetter());
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setAutoRaidmode(Guild guild, boolean auto)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, auto_raid_mode FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateBoolean("auto_raid_mode", auto);
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateBoolean("auto_raid_mode", auto);
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setBlockDmSpam(Guild guild, boolean block)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, block_dm_spam FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateBoolean("block_dm_spam", block);
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateBoolean("block_dm_spam", block);
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setModlogChannel(Guild guild, TextChannel tc)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, modlog_channel_id FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateLong("modlog_channel_id", tc==null ? 0l : tc.getIdLong());
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateLong("modlog_channel_id", tc==null ? 0l : tc.getIdLong());
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void setModRole(Guild guild, Role role)
    {
        try {
            Statement statement = connection.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE);
            statement.closeOnCompletion();
            try(ResultSet results = statement.executeQuery(String.format("SELECT guild_id, mod_role_id FROM GUILD_SETTINGS WHERE guild_id = %s", guild.getId())))
            {
                if(results.next())
                {
                    results.updateLong("mod_role_id", Long.parseLong(role.getId()));
                    results.updateRow();
                }
                else
                {
                    results.moveToInsertRow();
                    results.updateLong("guild_id", guild.getIdLong());
                    results.updateLong("mod_role_id", Long.parseLong(role.getId()));
                    results.insertRow();
                }
            }
        } catch( SQLException e) {
            LOG.warn(e);
        }
    }
    
    public void shutdown()
    {
        try {
            connection.close();
        } catch (SQLException ex) {
            LOG.warn(ex);
        }
    }
    
    public class GuildSettings {
        public final short maxMentions;
        public final short spamLimit;
        public final Action spamAction;
        public final Action inviteAction;
        public final String modRoleId;
        public final String modlogChannelId;
        public final boolean autoRaidMode;
        public final boolean blockDmSpam;
        private GuildSettings(short maxMentions, short spamLimit, String spamAction, String inviteAction, String modRoleId, String modlogChannelId, boolean autoRaidMode, boolean blockDmSpam)
        {
            this.maxMentions = maxMentions;
            this.spamLimit = spamLimit;
            this.spamAction = Action.of(spamAction);
            this.inviteAction = Action.of(inviteAction);
            this.modRoleId = modRoleId;
            this.modlogChannelId = modlogChannelId;
            this.autoRaidMode = autoRaidMode;
            this.blockDmSpam = blockDmSpam;
        }
    }
}
