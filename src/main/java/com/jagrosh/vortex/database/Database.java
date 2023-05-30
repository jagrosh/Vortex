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
package com.jagrosh.vortex.database;

import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.database.managers.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Database extends DatabaseConnector
{
    public final AutomodManager automod; // automod settings
    public final GuildSettingsDataManager settings; // logs and other settings
    public final IgnoreManager ignores; // ignored roles and channels
    public final AuditCacheManager auditcache; // cache of latest audit logs
    public final TempMuteManager tempmutes;
    public final GravelManager gravels;
    public final TempBanManager tempbans;
    public final TempSlowmodeManager tempslowmodes;
    public final InviteWhitelistManager inviteWhitelist;
    public final FilterManager filters;
    public final TagManager tags;
    public final WarningManager warnings;
    public final KickingManager kicks;
    private static final List<CurrentId> idCache = new ArrayList<>(1);
    private static ModlogManager[] managers = null;

    private static class CurrentId
    {
        private final long guildId;
        private int id;

        private CurrentId(long guildId, int id)
        {
            this.id = id;
            this.guildId = guildId;
        }

        @Override
        public boolean equals(Object obj)
        {
            return obj instanceof CurrentId && guildId == ((CurrentId) obj).guildId && id == ((CurrentId) obj).id;
        }
    }

    public static class Modlog
    {
        private final long modId, saviorId, userId;
        private final int id;
        private final Action type;
        private final String reason;
        private final Instant finnish, start;

        public Modlog(long userId, long modId, Action type, int id, String reason, Instant start)
        {
            this.userId = userId;
            this.modId = modId;
            this.type = type;
            this.id = id;
            this.reason = reason;
            this.finnish = null;
            this.start = start;
            this.saviorId = -1;
        }

        public Modlog(long userId, long modId, Action type, int id, String reason, Instant finnish, Instant start, long saviorId)
        {
            this.userId = userId;
            this.modId = modId;
            this.type = type;
            this.id = id;
            this.reason = reason;
            this.finnish = finnish;
            this.start = start;
            this.saviorId = saviorId;
        }

        public long getUserId()
        {
            return userId;
        }

        public long getModId()
        {
            return modId;
        }

        public long getSaviorId() { return saviorId; }

        public Action getType()
        {
            return type;
        }

        public int getId()
        {
            return id;
        }

        public String getReason()
        {
            return reason;
        }

        public Instant getFinnish() { return finnish;}

        public Instant getStart() { return start; }
    }

    public Database(String host, String user, String pass) throws Exception
    {
        super(host, user, pass);
        
        automod = new AutomodManager(this);
        settings = new GuildSettingsDataManager(this);
        ignores = new IgnoreManager(this);
        auditcache = new AuditCacheManager(this);
        tempmutes = new TempMuteManager(this);
        gravels = new GravelManager(this);
        tempbans = new TempBanManager(this);
        tempslowmodes = new TempSlowmodeManager(this);
        inviteWhitelist = new InviteWhitelistManager(this);
        filters = new FilterManager(this);
        tags = new TagManager(this);
        warnings = new WarningManager(this);
        kicks = new KickingManager(this);

        managers = new ModlogManager[] {tempmutes, gravels, warnings, tempbans, kicks};
        init();
    }

    public static synchronized int genNewId(long guildId)
    {
        for (CurrentId currentId: idCache)
            if (currentId.guildId == guildId)
                return ++currentId.id;

        int id = -1;
        for (ModlogManager manager : managers)
            id = Math.max(id, manager.getMaxId(guildId));

        if (id == -1) {
            CurrentId toBeAdded = new CurrentId(guildId, id);
            for (int i = 0; i < idCache.size(); i++)
                if (idCache.get(i).equals(toBeAdded)) {
                    idCache.remove(i);
                    break;
                }
        }

        idCache.add(new CurrentId(guildId, ++id));
        return id;
    }

    public static String sanitise(String param) {
        param = param.replaceAll("\"", "\"\"");
        param = param.replaceAll("\\\\", "\\\\");
        return param;
    }

    public static List<Modlog> getAllModlogs(long guildId, long userId)
    {
        List<Modlog> modlogs = new ArrayList<>();
        for (ModlogManager manager: managers)
            modlogs.addAll(manager.getModlogs(guildId, userId));
        return modlogs;
    }

    /**
     * Updates a reason
     * @param guildId Guild Id
     * @param caseId Case Id
     * @param reason New Reason
     * @return The old reason, null if the case could not be found
     */
    public static String updateReason(long guildId, int caseId, String reason) {
        for (ModlogManager manager : managers) {
            String oldReason = manager.updateReason(guildId, caseId, reason);
            if (oldReason != null)
                return oldReason;
        }
        return null;
    }

    public static Modlog deleteModlog(long guildId, int caseId)
    {
        for (ModlogManager manager: managers)
        {
            Modlog modlog = manager.deleteCase(guildId, caseId);
            if (modlog != null)
                return modlog;
        }

        return null;
    }

    public static boolean deleteRow(ResultSet rs) throws SQLException {
        if (rs.next())
        {
            rs.deleteRow();
            return true;
        }

        return false;
    }
}
