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

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Constants
{
    public final static OffsetDateTime STARTUP  = OffsetDateTime.now();
    public final static String PREFIX              = "?";
    public final static String SUCCESS             = ":white_check_mark:";
    public final static String WARNING             = ":warning:";
    public final static String ERROR               = ":x:";
    public final static String LOADING             = ":alarm_clock:";
    public final static String HELP_REACTION       = SUCCESS.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String ERROR_REACTION      = ERROR.replaceAll("<a?:(.+):(\\d+)>", "$1:$2");
    public final static String VORTEX_EMOJI        = "<:Vortex:386971287282515970>";
    public final static String ACTIVE_DEVELOPER    = "<:active_developer:1109963459929587713>";
    public final static String BOT                 = "<:bot:1109962368999497748>";
    public final static String VERIFIED_BOT        = "<:verified_bot:1109962361911124002>";
    public final static String VERIFIED_EARLY_DEV  = "<:verified_developer:1109962363316215860>";
    public final static String BUG_HUNTER_LEVEL_1  = "<:bug_hunter1:1109963458356727851>";
    public final static String BUG_HUNTER_LEVEL_2  = "<:bug_hunter2:1109963456414756915>";
    public final static String DISCORD_STAFF       = "<:discord_staff:1109962360887722066>";
    public final static String EARLY_NITRO_SUB     = "<:early_nitro_supporter:1109965926826266694>";
    public final static String HYPESQUAD_BALANCE   = "<:hypesquad_balance:1109965531542470756>";
    public final static String HYPESQUAD_BRAVERY   = "<:hypesquad_balance:1109965531542470756>";
    public final static String HYPESQUAD_BRILIANCE = "<:hypesquad_brilliance:1109962357788115008>";
    public final static String HYPESQUAD_EVENTS    = "<:hypesquad_events:1112537171090489388>";
    public final static String MODERATOR_ALUMNI    = "<:moderator_alumni:1109966127842480172>";
    public final static String NITRO               = "<:nitro:1109965928113897583>";
    public final static String PARTNERED_USER      = "<:partner:1109962367795732480>";
    public final static String PARTNERED_SERVER    = "<:partnered_server:1109962365610512476>";
    public final static String SERVER_BOOSTER      = "<:server_booster:1109965535019540582>";
    public final static String SERVER_OWNER        = "<:server_owner:1109965533698347099>";
    public final static String NEW_MEMBER          = "<:new_user:1112503015199473746>";
    public final static String DESKTOP_ONLINE      = "<:desktop_online:1112512720051380244>";
    public final static String DESKTOP_IDLE        = "<:desktop_idle:1112512761549819915>";
    public final static String DESKTOP_DND         = "<:desktop_dnd:1112512797587279922>";
    public final static String DESKTOP_OFFLINE     = "<:desktop_offline:1112512846966820925>";
    public final static String MOBILE_ONLINE       = "<:mobile_online:1112513069705334836>";
    public final static String MOBILE_IDLE         = "<:mobile_idle:1112513110356529242>";
    public final static String MOBILE_DND          = "<:mobile_dnd:1112513142367473754>";
    public final static String MOBILE_OFFLINE      = "<:mobile_offline:1112513178945986631>";
    public final static String BROWSER_ONLINE      = "<:browser_online:1112513381736398889>";
    public final static String BROWSER_IDLE        = "<:browser_idle:1112513425197772881>";
    public final static String BROWSER_DND         = "<:browser_dnd:1112513456109793331>";
    public final static String BROWSER_OFFLINE     = "<:browser_offline:1112513493262925935>";

    public final static int DEFAULT_CACHE_SIZE = 8000;

    // public final static String SERVER_INVITE = "https://discord.gg/0p9LSGoRLu6Pet0k";
    //public final static String BOT_INVITE  = "https://discordapp.com/oauth2/authorize?client_id=240254129333731328&scope=bot&permissions="+Permission.getRaw(PERMISSIONS);
    // public final static String BOT_INVITE    = "https://discordapp.com/oauth2/authorize?client_id=169463754382114816&scope=bot&permissions="+Permission.getRaw(PERMISSIONS);
    public final static String OWNER_ID = Vortex.config.getString("owner-id");
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

    private Constants() {}

    /*public static String getEmojiFromFlag(UserFlag flag) {
        switch (flag) {
            case UserFlag.ACTIVE_DEVELOPER:
                return ACTIVE_DEVELOPER;
            case UserFlag.BO
        }
    }*/
}
