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
import java.util.concurrent.ScheduledExecutorService;
import javax.security.auth.login.LoginException;
import me.jagrosh.jdacommands.CommandClientBuilder;
import me.jagrosh.jdacommands.examples.*;
import me.jagrosh.jdacommands.utils.EventWaiter;
import net.dv8tion.jda.core.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.commands.*;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        List<String> tokens;
        /**
         * Tokens:
         * 0 - bot token
         * 1 - carbonitex key
         * 2 - bots.discord.pw token
         */
        try {
            tokens = Files.readAllLines(Paths.get("config.txt"));
            EventWaiter waiter = new EventWaiter();
            ScheduledExecutorService threadpool = Executors.newSingleThreadScheduledExecutor();
            new JDABuilder(AccountType.BOT)
                    .setToken(tokens.get(0))
                    .addListener(new AutoMod(threadpool))
                    .addListener(new CommandClientBuilder()
                            .setPrefix(Constants.PREFIX)
                            .setOwnerId(Constants.OWNER_ID)
                            .setServerInvite(Constants.SERVER_INVITE)
                            .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                            .addCommands(
                                    new AutomodCmd(),
                                    new KickCmd(),
                                    new BanCmd(),
                                    new SoftbanCmd(threadpool),
                                    new HackbanCmd(),
                                    new CleanCmd(waiter),
                                    new MagnetCmd(waiter),
                                    new AboutCommand(Color.CYAN, "and I'm here to keep your Discord server safe and make moderating easy!", 
                                            Constants.BOT_INVITE, new String[]{"Moderation commands","Configurable automoderation","Very easy setup [coming soon]"}),
                                    new PingCommand(),
                                    new InviteCmd(),
                                    
                                    new StatsCmd(),
                                    new EvalCmd(),
                                    new ShutdownCmd()
                            )
                            .setCarbonitexKey(tokens.get(1))
                            .setDiscordBotsKey(tokens.get(2))
                            .setGame(null) //remove this for actual code
                            .build())
                    .addListener(waiter)
                    //.setStatus(OnlineStatus.DO_NOT_DISTURB)
                    //.setGame(Game.of("loading..."))
                    .buildAsync();
            //new JDABuilder(AccountType.CLIENT).setToken(tokens.get(1)).addListener(new AutoMod(commands)).buildAsync();
        } catch (IOException | ArrayIndexOutOfBoundsException | LoginException | RateLimitedException ex) {
            SimpleLog.getLog("Vortex").fatal(ex);
        }
    }
    
}
