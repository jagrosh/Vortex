/*
 * Copyright 2018 John Grosh (jagrosh).
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
package com.jagrosh.vortex;

import java.time.OffsetDateTime;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Constants
{
    public final static OffsetDateTime STARTUP = OffsetDateTime.now();
    public final static String PREFIX          = ">>";
    public final static String SUCCESS         = "<:vSuccess:390202497827864597>";
    public final static String WARNING         = "<:vWarning:390208158699618306>";
    public final static String ERROR           = "<:vError:390229421228949504>";
    public final static String LOADING         = "<a:typing:393848431413559296>";
    public final static String HELP_REACTION   = SUCCESS.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String ERROR_REACTION  = ERROR.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String VORTEX_EMOJI    = "<:Vortex:386971287282515970>";
    public final static int DEFAULT_CACHE_SIZE = 8000;
    public final static Permission[] PERMISSIONS = {Permission.ADMINISTRATOR, Permission.BAN_MEMBERS, Permission.KICK_MEMBERS, Permission.MANAGE_ROLES,
                                        Permission.MANAGE_SERVER, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_READ,
                                        Permission.MESSAGE_WRITE,Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI,
                                        Permission.MESSAGE_MANAGE, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_DEAF_OTHERS, 
                                        Permission.VOICE_MUTE_OTHERS, Permission.NICKNAME_CHANGE, Permission.NICKNAME_MANAGE, Permission.VIEW_AUDIT_LOGS};
    public final static String SERVER_INVITE = "https://discord.gg/0p9LSGoRLu6Pet0k";
    //public final static String BOT_INVITE  = "https://discordapp.com/oauth2/authorize?client_id=240254129333731328&scope=bot&permissions="+Permission.getRaw(PERMISSIONS);
    // public final static String BOT_INVITE    = "https://discordapp.com/oauth2/authorize?client_id=169463754382114816&scope=bot&permissions="+Permission.getRaw(PERMISSIONS);
    public final static String OWNER_ID      = "113156185389092864";
    public final static String DONATION_LINK = "https://patreon.com/jagrosh";
    
    public final static class Wiki
    {
        public final static String PRIMARY_LINK = "https://jagrosh.com/vortex";
        
        public final static String SHORT_WIKI     = "https://git.io/fxHam";
        public final static String SHORT_COMMANDS = "https://git.io/vAr0G";
        
        public final static String WIKI_BASE    = "https://github.com/jagrosh/Vortex/wiki";
        public final static String START        = WIKI_BASE + "/Getting-Started";
        public final static String LOG_TIMEZONE = WIKI_BASE + "/Log-Timezone";
        public final static String RAID_MODE    = WIKI_BASE + "/Raid-Mode";
        public final static String COMMANDS     = WIKI_BASE + "/Commands";
        public final static String AUTOMOD      = WIKI_BASE + "/Auto-Moderation";
    }
}
