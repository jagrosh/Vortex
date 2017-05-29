/*
 * Copyright 2016 John Grosh (jagrosh).
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
package vortex;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.Executors;
import javax.security.auth.login.LoginException;
import com.jagrosh.jdautilities.commandclient.CommandClientBuilder;
import com.jagrosh.jdautilities.commandclient.examples.*;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import java.sql.SQLException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import vortex.commands.*;
import vortex.data.DMSpamManager;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     * @throws javax.security.auth.login.LoginException
     * @throws net.dv8tion.jda.core.exceptions.RateLimitedException
     * @throws java.sql.SQLException
     */
    public static void main(String[] args) throws IOException, LoginException, IllegalArgumentException, RateLimitedException, SQLException {
        /**
         * Tokens:
         * 0  - bot token
         * 1  - bots.discord.pw token
         * 2  - database location
         * 3  - database username
         * 4  - database password
         * 5+ - dmspam block bots
         */
        List<String> tokens = Files.readAllLines(Paths.get("config.txt"));
        EventWaiter waiter = new EventWaiter();
        ScheduledExecutorService threadpool = Executors.newSingleThreadScheduledExecutor();
        DatabaseManager manager = new DatabaseManager(tokens.get(2), tokens.get(3), tokens.get(4));
        manager.startupCheck();
        ModLogger modlog = new ModLogger(manager);
        DMSpamManager dmspam = new DMSpamManager(tokens.subList(5, tokens.size()));
        AutoMod automod = new AutoMod(modlog, threadpool, manager, dmspam);
        new JDABuilder(AccountType.BOT)
                .setToken(tokens.get(0))
                .addEventListener(automod)
                //.useSharding(0, 1)
                .addEventListener(new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setOwnerId(Constants.OWNER_ID)
                        .setServerInvite(Constants.SERVER_INVITE)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .addCommands(
                                new AboutCommand(Color.CYAN, "and I'm here to keep your Discord server safe and make moderating easy!", 
                                        new String[]{"Moderation commands","Configurable automoderation","Very easy setup"},
                                        Permission.ADMINISTRATOR, Permission.BAN_MEMBERS, Permission.KICK_MEMBERS, Permission.MANAGE_ROLES,
                                        Permission.MANAGE_SERVER, Permission.MESSAGE_ADD_REACTION, Permission.MESSAGE_ATTACH_FILES, Permission.MESSAGE_READ,
                                        Permission.MESSAGE_WRITE,Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY, Permission.MESSAGE_EXT_EMOJI,
                                        Permission.MESSAGE_MANAGE, Permission.VOICE_CONNECT, Permission.VOICE_MOVE_OTHERS, Permission.VOICE_DEAF_OTHERS, 
                                        Permission.VOICE_MUTE_OTHERS, Permission.NICKNAME_CHANGE, Permission.NICKNAME_MANAGE),
                                new InviteCmd(),
                                new PingCommand(),
                                new UserinfoCmd(),
                                
                                // Settings
                                new ModlogCmd(manager, modlog),
                                new SettingsCmd(automod),
                                //new SetupCmd(waiter,manager),
                                
                                // Moderation
                                new KickCmd(modlog),
                                new BanCmd(modlog),
                                new SoftbanCmd(modlog, threadpool),
                                new HackbanCmd(modlog),
                                new CleanCmd(modlog,threadpool),
                                new MagnetCmd(waiter),
                                new MuteCmd(modlog),
                                new RaidCmd(automod),
                                new UnmuteCmd(modlog),
                                
                                // Automoderation
                                new AntiinviteCmd(manager, modlog),
                                new AntimentionCmd(manager, modlog),
                                new AntispamCmd(manager, modlog),
                                new AutoraidmodeCmd(manager),
                                new AntidmspamCmd(manager, dmspam),
                                new IgnoreCmd(manager, modlog),
                                new UnignoreCmd(manager, modlog),
                                
                                // Owner
                                new EvalCmd(),
                                new GuildlistCommand(waiter),
                                new StatsCmd(),
                                new ShutdownCmd(automod, dmspam)
                        )
                        //.setCarbonitexKey(tokens.get(1))
                        .setDiscordBotsKey(tokens.get(1))
                        .build())
                .addEventListener(waiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.of("loading..."))
                .buildAsync();
        Logger.getLogger("org.apache.http.client.protocol.ResponseProcessCookies").setLevel(Level.OFF);
    }
}
