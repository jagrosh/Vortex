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

import com.jagrosh.vortex.commands.tools.LookupCmd;
import com.jagrosh.vortex.commands.automod.*;
import com.jagrosh.vortex.commands.general.*;
import com.jagrosh.vortex.commands.moderation.*;
import com.jagrosh.vortex.commands.tools.*;
import com.jagrosh.vortex.commands.owner.*;
import com.jagrosh.vortex.commands.settings.*;
import java.util.concurrent.Executors;
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandClientBuilder;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import com.jagrosh.jdautilities.examples.command.*;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.automod.StrikeHandler;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.Game;
import com.jagrosh.vortex.database.Database;
import com.jagrosh.vortex.logging.BasicLogger;
import com.jagrosh.vortex.logging.MessageCache;
import com.jagrosh.vortex.logging.ModLogger;
import com.jagrosh.vortex.logging.TextUploader;
import com.jagrosh.vortex.utils.BlockingSessionController;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.util.EnumSet;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Vortex
{
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
    private final JDA altBot;
    
    public Vortex() throws Exception
    {
        System.setProperty("config.file", System.getProperty("config.file", "application.conf"));
        Config config = ConfigFactory.load();
        altBot = new JDABuilder(config.getString("alt-token")).build();
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
                        .setGame(Game.playing(Constants.Wiki.PRIMARY_LINK))
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
                            new AboutCmd(),
                            new InviteCmd(),
                            new PingCommand(),
                            new RoleinfoCmd(),
                            new ServerinfoCmd(),
                            new UserinfoCmd(),

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
                            new UnmuteCmd(this),
                            new RaidCmd(this),
                            new StrikeCmd(this),
                            new PardonCmd(this),
                            new CheckCmd(this),
                            new ReasonCmd(this),

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
                            new ExportCmd(this),
                            new InvitepruneCmd(this),
                            new LookupCmd(this),

                            // Owner
                            new EvalCmd(this),
                            new DebugCmd(this),
                            new PremiumCmd(this),
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
        shards = new DefaultShardManagerBuilder()
                .setShardsTotal(config.getInt("shards-total"))
                .setToken(config.getString("bot-token"))
                .addEventListeners(new Listener(this), client, waiter)
                .setStatus(OnlineStatus.DO_NOT_DISTURB)
                .setGame(Game.playing("loading..."))
                .setBulkDeleteSplittingEnabled(false)
                .setRequestTimeoutRetry(true)
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.EMOTE, CacheFlag.GAME)) //TODO: dont disable GAME
                .setSessionController(new BlockingSessionController())
                .build();
        
        modlog.start();
        
        threadpool.scheduleWithFixedDelay(() -> cleanPremium(), 0, 2, TimeUnit.HOURS);
        threadpool.scheduleWithFixedDelay(() -> leavePointlessGuilds(), 5, 30, TimeUnit.MINUTES);
        threadpool.scheduleWithFixedDelay(() -> System.gc(), 12, 6, TimeUnit.HOURS);
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
    
    public StrikeHandler getStrikeHandler()
    {
        return strikehandler;
    }
    
    public CommandExceptionListener getListener()
    {
        return listener;
    }
    
    
    // Global methods
    public void cleanPremium()
    {
        database.premium.cleanPremiumList().forEach((gid) ->
        {
            database.automod.setResolveUrls(gid, false);
            database.settings.setAvatarLogChannel(gid, null);
            database.settings.setVoiceLogChannel(gid, null);
            database.filters.deleteAllFilters(gid);
        });
    }
    
    public void leavePointlessGuilds()
    {
        shards.getGuilds().stream().filter(g -> 
        {
            if(!g.isAvailable())
                return false;
            if(Constants.OWNER_ID.equals(g.getOwnerId()))
                return false;
            int botcount = (int) g.getMemberCache().stream().filter(m -> m.getUser().isBot()).count();
            int totalcount = (int) g.getMemberCache().size();
            int humancount = totalcount - botcount;
            if(humancount < 30 || botcount > humancount)
            {
                if(database.settings.hasSettings(g))
                    return false;
                if(database.automod.hasSettings(g))
                    return false;
                return true;
            }
            return false;
        }).forEach(g -> 
        {
            OtherUtil.safeDM(g.getOwner()==null ? null : g.getOwner().getUser(), Constants.ERROR + " Sorry, your server **" 
                    + g.getName() + "** does not meet the minimum requirements for using Vortex. You can find the requirements "
                    + "here: <" + Constants.Wiki.START + ">. \n\n" + Constants.WARNING + "You may want to consider using a "
                    + "different bot that is designed for servers like yours. You can find a public list of bots here: "
                    + "<https://discord.bots.gg>.", true, () -> g.leave().queue());
        });
    }
    
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception
    {
        new Vortex();
    }
}
