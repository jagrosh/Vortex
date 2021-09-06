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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.jagrosh.vortex.commands.tools.LookupCmd;
import com.jagrosh.vortex.commands.automod.*;
import com.jagrosh.vortex.commands.general.*;
import com.jagrosh.vortex.commands.moderation.*;
import com.jagrosh.vortex.commands.tools.*;
import com.jagrosh.vortex.commands.owner.*;
import com.jagrosh.vortex.commands.settings.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.OnlineStatus;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.logging.BasicLogger;
import com.jagrosh.vortex.logging.MessageCache;
import com.jagrosh.vortex.logging.ModLogger;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.logging.TextUploader;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.MultiBotManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 * Main class for Vortex
 * @author John Grosh (jagrosh)
 */
@Slf4j
public class Vortex
{
    public static final Config config;
    private final @Getter EventWaiter eventWaiter;
    private final @Getter ScheduledExecutorService threadpool;
    private final @Getter Database database;
    private final @Getter TextUploader textUploader;
    private final @Getter MultiBotManager multiBotManager;
    private final @Getter ModLogger modLogger;
    private final @Getter BasicLogger basicLogger;
    private final @Getter MessageCache messageCache;
    private final @Getter WebhookClient logWebhook;
    private final @Getter AutoMod autoMod;
    private final @Getter CommandExceptionListener listener;

    static {
        System.setProperty("config.file", System.getProperty("config.file", "application.conf"));
        File configFile = new File(System.getProperty("config.file"));
        try {
            if (configFile.createNewFile()) {
                InputStream inputStream = Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("reference.conf");

                if (inputStream == null) {
                    log.error("Unable to load reference.conf in resources");
                    System.exit(1);
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new FileWriter(configFile));

                String line;
                while ((line = reader.readLine()) != null)
                {
                    writer.write(line + "\n");
                }

                reader.close();
                writer.close();
                log.info("A configuration file named " + System.getProperty("config.file") + " was created. Please fill it out and rerun the bot");
                System.exit(0);
            }
        } catch (IOException e) {
            log.error("Could not create a configuration file", e);
            throw new IOError(e);
        }

        config = ConfigFactory.load();
    }


    public Vortex() throws Exception
    {
        eventWaiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "eventwaiter")), false);
        threadpool = Executors.newScheduledThreadPool(100, r -> new Thread(r, "vortex"));
        database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        textUploader = new TextUploader(config.getStringList("upload-webhooks"));
        modLogger = new ModLogger(this);
        basicLogger = new BasicLogger(this, config);
        messageCache = new MessageCache();
        logWebhook = new WebhookClientBuilder(config.getString("webhook-url")).build();
        autoMod = new AutoMod(this, config);
        listener = new CommandExceptionListener();
        CommandClient client = new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setActivity(Activity.playing(Constants.Wiki.PRIMARY_LINK))
                        .setOwnerId(Constants.OWNER_ID)
                        .setServerInvite(Constants.SERVER_INVITE)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .setLinkedCacheSize(0)
                        .setGuildSettingsManager(database.settings)
                        .setListener(listener)
                        .setScheduleExecutor(threadpool)
                        .setShutdownAutomatically(false)
                        .addCommands(
                            // General
                            new AboutCmd(this),
                            new PingCmd(this),
                            new RoleinfoCmd(this),
                            new ServerinfoCmd(this),
                            new UserinfoCmd(this),
                            new RatCmd(this),

                            // Moderation
                            new KickCmd(this),
                            new BanCmd(this),
                            new SoftbanCmd(this),
                            new SilentbanCmd(this),
                            new UnbanCmd(this),
                            new CleanCmd(this),
                            new VoicemoveCmd(this),
                            new VoicekickCmd(this),
                            new MuteCmd(this),
                            new GravelCmd(this),
                            new UngravelCmd(this),
                            new UnmuteCmd(this),
                            new RaidCmd(this),
                            new ReasonCmd(this),
                            new SlowmodeCmd(this),


                            // Settings
                            new SetupCmd(this),
                            new MessagelogCmd(this),
                            new ModlogCmd(this),
                            new ServerlogCmd(this),
                            new VoicelogCmd(this),
                            new AvatarlogCmd(this),
                            new TimezoneCmd(this),
                            new ModroleCmd(this),
                            new PrefixCmd(this),
                            new SettingsCmd(this),
                            new AddTagCmd(this),
                            new DelTagCmd(this),

                            // Automoderation
                            new AntiinviteCmd(this),
                            new MaxlinesCmd(this),
                            new MaxmentionsCmd(this),
                            new AntiduplicateCmd(this),
                            new AutodehoistCmd(this),
                            new FilterCmd(this),
                            new ResolvelinksCmd(this),
                            new AutoraidmodeCmd(this),
                            new IgnoreCmd(this),
                            new UnignoreCmd(this),
                            
                            // Tools
                            new AnnounceCmd(),
                            new AuditCmd(),
                            new DehoistCmd(),
                            new ExportCmd(this),
                            new InvitepruneCmd(this),
                            new LookupCmd(this),
                            new TagCmd(this),
                            new TagsCmd(this),

                            // Owner
                            new EvalCmd(this),
                            new DebugCmd(this),
                            new ReloadCmd(this)
                            //new TransferCmd(this)
                        )
                        .setHelpConsumer(event -> event.replyInDm(FormatUtil.formatHelp(event, this), m -> 
                        {
                            if(event.isFromType(ChannelType.TEXT))
                                try
                                {
                                    event.getMessage().addReaction(Constants.HELP_REACTION).queue(s->{}, f->{});
                                } catch(PermissionException ignore) {}
                        }, t -> event.replyWarning("Help cannot be sent because you are blocking Direct Messages.")))
                        .setDiscordBotsKey(config.getString("listing.discord-bots"))
                        //.setCarbonitexKey(config.getString("listing.carbon"))
                        .build();
        MessageAction.setDefaultMentions(Arrays.asList(Message.MentionType.EMOTE, Message.MentionType.CHANNEL));
        multiBotManager = new MultiBotManager.MultiBotManagerBuilder()
                .addBot(config.getString("bot-token"), Constants.INTENTS)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableCache(CacheFlag.MEMBER_OVERRIDES, CacheFlag.VOICE_STATE)
                .disableCache(CacheFlag.EMOTE, CacheFlag.ACTIVITY, CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .addEventListeners(new Listener(this), client, eventWaiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setActivity(Activity.playing("loading..."))
                .build();
        
        modLogger.start();

        threadpool.scheduleWithFixedDelay(() -> database.tempbans.checkUnbans(multiBotManager), 0, 2, TimeUnit.MINUTES);
        threadpool.scheduleWithFixedDelay(() -> database.tempmutes.checkUnmutes(multiBotManager, database.settings), 0, 45, TimeUnit.SECONDS);
        threadpool.scheduleWithFixedDelay(() -> database.tempslowmodes.checkSlowmode(multiBotManager), 0, 45, TimeUnit.SECONDS);
    }

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception Any uncaught exception in the bot that may occur
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}