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

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.WebhookClientBuilder;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.vortex.commands.tools.LookupCmd;
import com.jagrosh.vortex.commands.automod.*;
import com.jagrosh.vortex.commands.general.*;
import com.jagrosh.vortex.commands.moderation.*;
import com.jagrosh.vortex.commands.tools.*;
import com.jagrosh.vortex.commands.owner.*;
import com.jagrosh.vortex.commands.settings.*;

import java.util.Arrays;
import java.util.concurrent.Executors;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.automod.StrikeHandler;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import java.util.concurrent.ScheduledExecutorService;

import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.logging.BasicLogger;
import com.jagrosh.vortex.logging.MessageCache;
import com.jagrosh.vortex.logging.ModLogger;
import com.jagrosh.vortex.logging.TextUploader;
import com.jagrosh.vortex.utils.BlockingSessionController;
import com.jagrosh.vortex.utils.FormatUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.concurrent.TimeUnit;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex
{
    public  static final Config config;
    public  final boolean developerMode;
    private final EventWaiter waiter;
    private final ScheduledExecutorService threadpool;
    private final Database database;
    private final TextUploader uploader;
    private final ShardManager shards;
    private final ModLogger modlog;
    private final BasicLogger basiclog;
    private final MessageCache messages;
    private final WebhookClient logwebhook;
    private final AutoMod automod;
    private final StrikeHandler strikehandler;
    private final CommandExceptionListener listener;
    private final Command[] commands;
    private final SlashCommand[] slashCommands;


    static {
        System.setProperty("config.file", System.getProperty("config.file", "application.conf"));
        config = ConfigFactory.load();
    }


    public Vortex() throws Exception
    {
        commands = new Command[] {
                // General
                new AboutCmd(this),
                new PingCmd(this),
                new RoleinfoCmd(this),
                new ServerinfoCmd(this),
                new UserinfoCmd(this),

                // Moderation
                new KickCmd(this),
                new BanCmd(this),
                new SoftbanCmd(this),
                new UnbanCmd(this),
                new CleanCmd(this),
                new VoicemoveCmd(this),
                new VoicekickCmd(this),
                new MuteCmd(this),
                new GravelCmd(this),
                new UngravelCmd(this),
                new UnmuteCmd(this),
                new RaidCmd(this),
                // new StrikeCmd(this),
                // new PardonCmd(this),
                new CheckCmd(this),
                new ModlogsCmd(this),
                new WarnCmd(this),
                new DelModlogCmd(this),
                new UpdateCmd(this),

                // Settings
                new SetupCmd(this),
                new PunishmentCmd(this),
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
                new AnticopypastaCmd(this),
                new AntieveryoneCmd(this),
                new AntirefCmd(this),
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
                new InvitepruneCmd(this),
                new LookupCmd(this),
                new TagCmd(this),
                new TagsCmd(this),

                // Owner
                new EvalCmd(this),
                new DebugCmd(this),
                // new PremiumCmd(this),
                new ReloadCmd(this)
                //new TransferCmd(this)
        };

        JDA altBot = JDABuilder.createLight(config.getString("alt-token")).build();
        slashCommands = Arrays.stream(commands)
                            .filter(command -> command instanceof SlashCommand)
                            .toArray(SlashCommand[]::new);
        developerMode = config.getBoolean("developer-mode");
        waiter = new EventWaiter(Executors.newSingleThreadScheduledExecutor(), false);
        threadpool = Executors.newScheduledThreadPool(100);
        database = new Database(config.getString("database.host"), 
                                       config.getString("database.username"), 
                                       config.getString("database.password"));
        uploader = new TextUploader(altBot, config.getLong("uploader.guild"), config.getLong("uploader.category"));
        modlog = new ModLogger(this);
        basiclog = new BasicLogger(this, config);
        messages = new MessageCache();
        logwebhook = new WebhookClientBuilder(config.getString("webhook-url")).build();
        automod = new AutoMod(this, altBot, config);
        strikehandler = new StrikeHandler(this);
        listener = new CommandExceptionListener();
        CommandClient client = new CommandClientBuilder()
                        .setPrefix(Constants.PREFIX)
                        .setActivity(Activity.watching("Toycat"))
                        .setOwnerId(Constants.OWNER_ID)
                        // .setServerInvite(Constants.SERVER_INVITE)
                        .setEmojis(Constants.SUCCESS, Constants.WARNING, Constants.ERROR)
                        .setLinkedCacheSize(0)
                        .setGuildSettingsManager(database.settings)
                        .setListener(listener)
                        .setScheduleExecutor(threadpool)
                        .setShutdownAutomatically(false)
                        .addCommands(commands)
                        .addSlashCommands(slashCommands)
                        .forceGuildOnly(developerMode ? config.getString("uploader.guild") : null)
                        .setHelpConsumer(e -> OtherUtil.commandEventReplyDm(e, FormatUtil.formatHelp(e, this), m ->
                                {
                                    if(e.isFromType(ChannelType.TEXT))
                                        try
                                        {
                                            e.getMessage().addReaction(Emoji.fromFormatted(Constants.HELP_REACTION)).queue(s->{}, f->{});
                                        } catch(PermissionException ignore) {}
                                }, t -> e.replyWarning("Help cannot be sent because you are blocking Direct Messages.")))
                                //.setDiscordBotsKey(config.getString("listing.discord-bots"))
                                //.setCarbonitexKey(config.getString("listing.carbon"))
                                .build();
        // TODO: Support custom amount of shards via shard-total in config
        shards = DefaultShardManagerBuilder.create(config.getString("bot-token"), GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MODERATION, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_PRESENCES)
                .addEventListeners(new Listener(this), client, waiter)
                .setStatus(OnlineStatus.ONLINE)
                .setActivity(Activity.playing("loading..."))
                .setBulkDeleteSplittingEnabled(false)
                .setRequestTimeoutRetry(true)
                .disableCache(CacheFlag.EMOJI, CacheFlag.ACTIVITY, CacheFlag.STICKER, CacheFlag.SCHEDULED_EVENTS, CacheFlag.FORUM_TAGS) //TODO: figure out why it said dont disable game and see if disabling it (as done) will break anything internally
                .setSessionController(new BlockingSessionController())
                .setCompression(Compression.NONE)
                .build();
        
        modlog.start();
        threadpool.scheduleWithFixedDelay(System::gc, 12, 6, TimeUnit.HOURS);
    }
    
    
    // Getters
    public EventWaiter getEventWaiter()
    {
        return waiter;
    }
    
    public Database getDatabase()
    {
        return database;
    }
    
    public ScheduledExecutorService getThreadpool()
    {
        return threadpool;
    }
    
    public TextUploader getTextUploader()
    {
        return uploader;
    }
    
    public ShardManager getShardManager()
    {
        return shards;
    }
    
    public ModLogger getModLogger()
    {
        return modlog;
    }
    
    public BasicLogger getBasicLogger()
    {
        return basiclog;
    }
    
    public MessageCache getMessageCache()
    {
        return messages;
    }
    
    public WebhookClient getLogWebhook()
    {
        return logwebhook;
    }
    
    public AutoMod getAutoMod()
    {
        return automod;
    }

    @Deprecated
    public StrikeHandler getStrikeHandler()
    {
        return strikehandler;
    }
    
    public CommandExceptionListener getListener()
    {
        return listener;
    }

    // Global methods
    @Deprecated
    public void cleanPremium() {}

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}
