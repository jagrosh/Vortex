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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.database;

import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.vortex.database.managers.*;

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
        
        init();
    }

    public static String sanitise(String param) {
        param = param.replaceAll("'", "''");
        param = param.replaceAll("\"", "\"\"");
        param = param.replaceAll("\\\\", "\\\\");
        return param;
    }
}
