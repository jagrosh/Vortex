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
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.automod.AutoMod;
import com.jagrosh.vortex.database.managers.GuildSettingsDataManager.GuildSettings;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.LogUtil.ParsedAuditReason;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.AuditLogChange;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The ModLogger's primary use is to log stuff from the Audit Log to a users or to a servers modlogs
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ModLogger
{
    private final static int LIMIT = 40;
    private final static Logger LOG = LoggerFactory.getLogger(ModLogger.class);
    private final HashMap<Long,Integer> caseNum = new HashMap<>();
    private final HashSet<Long> needsUpdate = new HashSet<>();
    private final Vortex vortex;
    private volatile AtomicBoolean isStarted = new AtomicBoolean();
    private final Map<Long, List<Long>> beginningCache = new HashMap<>(LIMIT);

    public ModLogger(Vortex vortex)
    {
        this.vortex = vortex;
    }

    /**
     * Method for interacting with the beginningCache map in a thread-safe and synchronous manner
     * @param method The name of the method to invoke.
     *               Currently only supports containsKey(Long), get(Long), put(Long, List<​Long>), and putIfAbsent(Long, List<​Long>)
     * @param args The arguments of the specific method.
     * @see HashMap
     * @return
     */
    private synchronized Object beginningCache(String method, Object... args) {
        switch (method) {
            case "containsKey":
                // Checks if arguments can be beginningCache(String, Object (preferably of type Long), Long), will ignore extra arguments
                if (args.length == 0 || !(args[0] instanceof Long))
                    return false;
                return beginningCache.containsKey(args[0]);
            case "get":
                // Checks if arguments can be beginningCache(String, Object, Long)
                if (args.length == 0 || !(args[0] instanceof Long))
                    return false;
                return beginningCache.get(args[0]);
            case "put":
                // Checks if arguments can be beginningCache(String, Long, List (preferably of type List<Long>)), will ignore extra arguments
                if (args.length < 2 || !(args[0] instanceof Long) || !(args[1] instanceof List))
                    return null;

                try {
                    return beginningCache.put((Long) args[0], (List<Long>) args[1]);
                } catch (Exception e) {
                    LOG.error("Exception while updating auditlog cache: " + e.toString());
                    return null;
                }
            case "putIfAbsent":
                // Checks if arguments can be beginningCache(String, Long, List)
                if (args.length < 2 || !(args[0] instanceof Long) || !(args[1] == null || args[1] instanceof List))
                    return null;

                try {
                    return beginningCache.putIfAbsent((Long) args[0], (List<Long>) args[1]);
                } catch (Exception e) {
                    LOG.error("Exception while updating auditlog cache: " + e.toString());
                    return null;
                }
        }

        return null;
    }

    /**
     * Starts the thread that executes every 3 seconds to check for new audit log entries
     */
    public synchronized void start()
    {
        if(isStarted.getAndSet(true))
            return;

        vortex.getThreadpool().scheduleWithFixedDelay(()->
        {
            Set<Long> toUpdate;
            synchronized(needsUpdate)
            {
                toUpdate = new HashSet<>(needsUpdate);
                modifyUpdate();
            }
            if(!toUpdate.isEmpty())
            {
                LOG.info("DEBUG Modlog updating " + toUpdate.size() + " guilds: " + toUpdate.toString());
                try
                {
                    long time, diff;
                    for(long gid: toUpdate)
                    {
                        time = System.currentTimeMillis();
                        update(vortex.getShardManager().getGuildById(gid));
                        diff = System.currentTimeMillis() - time;
                        if(diff > 10000)
                            LOG.warn("Took " + diff + "ms to update " + gid);
                    }
                } catch(Exception ex)
                {
                    LOG.error("Exception thrown during modlog update loop: "+ex);
                    ex.printStackTrace();
                }
            }
        }, 0, 3, TimeUnit.SECONDS);
    }

    /**
     * Method for interacting with the needsUpdate map in a thread-safe manner
     * @param guild If none are passed in, the clear() method will be invoked. If a (or multiple) guild's IDs are passed in,
     *              it will add the first one to the needsUpdate list.
     */
    private synchronized void modifyUpdate(long... guild) {
        if (guild.length > 0)
            needsUpdate.add(guild[0]);
        else
            needsUpdate.clear();
    }

    public void addNewGuild(Guild guild)
    {
        if (guild == null)
            return;

        beginningCache("putIfAbsent", guild.getIdLong(), null);
        setNeedUpdate(guild);
    }

    /**
     * Filters the audit log entries to get rid of the ones from before the bot has joined the server, or if in
     * first-run mode, will also filter logs from before the bot has turned on.
     * @param entries The list of entries to filter
     */
    @SuppressWarnings("unchecked")
    private synchronized void filterEntries(List<AuditLogEntry> entries)
    {
        if (entries == null || entries.isEmpty())
            return;

        try
        { //TODO: Figure out why I even put first run and why is it tied to developer mode
            if (!vortex.developerMode && !((boolean) beginningCache("containsKey", entries.get(0).getGuild().getIdLong())))
                return;
        }
        catch (NullPointerException e)
        {
            return;
        }

        List<Long> aleIds = (List<Long>) beginningCache("get", entries.get(0).getGuild().getIdLong());
        if (aleIds == null)
        {
            List<Long> newAleIds = new ArrayList<>(entries.size());
            for (AuditLogEntry ale : entries)
                newAleIds.add(ale.getIdLong());
            beginningCache.put(entries.get(0).getGuild().getIdLong(), newAleIds);
            entries.clear();
        }
        else
        {
            if (aleIds.isEmpty())
                return;

            boolean shouldClearCache = true;
            for (int i = 0; i < entries.size(); i++)
            {
                if (aleIds.contains(entries.get(i).getIdLong()))
                {
                    entries.remove(i--);
                    shouldClearCache = false;
                }
            }

            if (shouldClearCache)
                aleIds.clear();
        }
    }



    /**
     * Sets that a guild might have new audit logs that would interest the bot, and that it should retrive and work with them.
     * @param guild The guild to update
     */
    public void setNeedUpdate(Guild guild)
    {
        vortex.getThreadpool().schedule(() -> modifyUpdate(guild.getIdLong()), 2, TimeUnit.SECONDS);
    }

    @Deprecated
    public void postStrikeCase(Member moderator, OffsetDateTime now, int givenStrikes, int oldStrikes, int newStrikes, User target, String reason)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getModLogChannel(moderator.getGuild());
        if(modlog==null || !modlog.canTalk())
            return;
        getCaseNumberAsync(modlog, i -> 
        {
            modlog.sendMessage(FormatUtil.filterEveryone(LogUtil.modlogStrikeFormat(now, 
                    vortex.getDatabase().settings.getSettings(moderator.getGuild()).getTimezone(), i, 
                    moderator.getUser(), givenStrikes, oldStrikes, newStrikes, target, reason))).queue();
        });
    }

    @Deprecated
    public void postPardonCase(Member moderator, OffsetDateTime now, int pardonedStrikes, int oldStrikes, int newStrikes, User target, String reason)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getModLogChannel(moderator.getGuild());
        if(modlog==null || !modlog.canTalk())
            return;
        getCaseNumberAsync(modlog, i -> 
        {
            modlog.sendMessage(FormatUtil.filterEveryone(LogUtil.modlogPardonFormat(now, 
                    vortex.getDatabase().settings.getSettings(moderator.getGuild()).getTimezone(), i, 
                    moderator.getUser(), pardonedStrikes, oldStrikes, newStrikes, target, reason))).queue();
        });
    }

    @Deprecated
    public void postRaidmodeCase(Member moderator, OffsetDateTime now, boolean enabled, String reason)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getModLogChannel(moderator.getGuild());
        if(modlog==null || !modlog.canTalk())
            return;
        getCaseNumberAsync(modlog, i -> 
        {
            
            modlog.sendMessage(FormatUtil.filterEveryone(LogUtil.modlogRaidFormat(now, 
                    vortex.getDatabase().settings.getSettings(moderator.getGuild()).getTimezone(), i, 
                    moderator.getUser(), enabled, reason))).queue();
        });
    }

    @Deprecated
    public void postPseudoCase(Member moderator, OffsetDateTime now, Action act, User target, int minutes, String reason)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getModLogChannel(moderator.getGuild());
        if(modlog==null || !modlog.canTalk())
            return;
        ZoneId timezone = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getTimezone();
        getCaseNumberAsync(modlog, i ->
        {
            modlog.sendMessage(FormatUtil.filterEveryone(minutes > 0 ?
                            LogUtil.modlogTimeFormat(now, timezone, i, moderator.getUser(), act, minutes, target, reason) :
                            LogUtil.modlogUserFormat(now, timezone, i, moderator.getUser(), act, target, reason))).queue();
        });
    }

    // private methods

    /**
     * Updates a specific guild's modlogs
     * @param guild The guild to update
     */
    private void update(Guild guild) // not async
    {
        if(guild==null)
            return;
        GuildSettings gs = vortex.getDatabase().settings.getSettings(guild);
        TextChannel modlog = gs.getModLogChannel(guild);
        if(!guild.getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS))
            return;
        Role mRole = gs.getMutedRole(guild);
        Role gRole = gs.getGravelRole(guild);
        try
        {
            List<AuditLogEntry> list = guild.retrieveAuditLogs().cache(false).limit(LIMIT).submit().get(30, TimeUnit.SECONDS);
            filterEntries(list);
            for(AuditLogEntry ale: vortex.getDatabase().auditcache.filterUncheckedEntries(list))
            {
                Action act = null;
                switch(ale.getType())
                {
                    case BAN:
                        act = Action.BAN;
                        break;
                    case KICK:
                        act = Action.KICK;
                        break;
                    case UNBAN:
                        act = Action.UNBAN;
                        break;
                    case MEMBER_ROLE_UPDATE:
                        AuditLogChange added = ale.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD);
                        if(added!=null)
                        {
                            if (mRole !=null && ((ArrayList<HashMap<String,String>>)added.getNewValue()).stream().anyMatch(hm -> hm.get("id").equals(mRole.getId())))
                            {
                                act = Action.MUTE;
                                break;
                            }
                            else if (gRole !=null && ((ArrayList<HashMap<String,String>>)added.getNewValue()).stream().anyMatch(hm -> hm.get("id").equals(gRole.getId())))
                            {
                                act = Action.GRAVEL;
                                break;
                            }
                            else
                            {
                                vortex.getBasicLogger().logAuditLogEntry(ale);
                                continue;
                            }
                        }
                        AuditLogChange removed = ale.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE);
                        if(removed!=null)
                        {
                            if (((ArrayList<HashMap<String,String>>)removed.getNewValue()).stream().anyMatch(hm -> hm.get("id").equals(mRole.getId())))
                            {
                                act = Action.UNMUTE;
                                break;
                            }
                            else if (((ArrayList<HashMap<String,String>>)removed.getNewValue()).stream().anyMatch(hm -> hm.get("id").equals(gRole.getId())))
                            {
                                act = Action.UNGRAVEL;
                                break;
                            }
                            else
                            {
                                vortex.getBasicLogger().logAuditLogEntry(ale);
                                continue;
                            }
                        }
                        break;
                    default:
                }
                if(act!=null)
                {
                    User mod = ale.getUser();
                    if(ale.getJDA().getSelfUser().equals(mod) && (act==Action.MUTE || act==Action.GRAVEL) && (AutoMod.RESTORE_MUTE_ROLE_AUDIT.equals(ale.getReason()) || AutoMod.RESTORE_GRAVEL_ROLE_AUDIT.equals(ale.getReason())))
                        continue; // restoring muted or gravel role (aka role persist) shouldn't trigger a log entry
                    String reason = ale.getReason()==null ? "" : ale.getReason();
                    int minutes;
                    if(mod.isBot())
                    {
                        ParsedAuditReason parsed = LogUtil.parse(guild, reason);
                        if(parsed!=null)
                        {
                            mod = parsed.moderator.getUser();
                            reason = parsed.reason;
                            minutes = parsed.minutes;
                            if(minutes>0)
                            {
                                if(act==Action.BAN)
                                    act = Action.TEMPBAN;
                                else if(act==Action.MUTE)
                                    act = Action.TEMPMUTE;
                                else if(act==Action.GRAVEL)
                                    act = Action.TEMPGRAVEL;
                            }
                        }
                    }
                    if(act==Action.UNBAN)
                        // vortex.getDatabase().tempbans.clearBan(guild, ale.getTargetIdLong(), mod.getIdLong());
                    if(act==Action.UNMUTE)
                        vortex.getDatabase().tempmutes.removeMute(guild, ale.getTargetIdLong(), ale.getUser().getIdLong());
                    if(act==Action.UNGRAVEL)
                        vortex.getDatabase().gravels.removeGravel(guild, ale.getTargetIdLong(), ale.getUser().getIdLong());
                    if (guild.getJDA().getSelfUser().getIdLong()!=mod.getIdLong())
                    {
                        if(act==Action.MUTE)
                            vortex.getDatabase().tempmutes.overrideMute(guild, ale.getTargetIdLong(), mod.getIdLong(), Instant.MAX, "");
                        else if(act==Action.GRAVEL)
                            vortex.getDatabase().gravels.overrideGravel(guild, ale.getTargetIdLong(), mod.getIdLong(), Instant.MAX, "");
                        else if(act==Action.BAN)
                        ;// vortex.getDatabase().tempbans.setBan(guild, ale.getTargetIdLong(), mod.getIdLong(), Instant.MAX, reason);
                        else if(act==Action.KICK)
                            vortex.getDatabase().kicks.logCase(vortex, guild, mod.getIdLong(), ale.getTargetIdLong(), reason);
                    }
                    else if(act==Action.KICK)
                    {
                        vortex.getDatabase().kicks.logCase(vortex, guild, getActualModId(ale), ale.getTargetIdLong(), getActualReason(ale));
                    }
                }
                else
                    vortex.getBasicLogger().logAuditLogEntry(ale);
            }
        }
        catch (TimeoutException ex)
        {
            LOG.warn("Retreiving audit logs for "+guild+" took longer than 30 seconds!");
        }
        catch(Exception ex)
        {
            LOG.error("Exception thrown during modlog update: "+ex);
            ex.printStackTrace();
        }
    }

    /**
     * Gets the ID of the actual responsible moderator from an Audit Log entry. If this method is not then it would show
     * that the bot is responsible for all kicks, manual bans, etc.
     * @param ale The audit log entry
     * @return -2 if this action has no moderator or the moderator's IDs
     */
    private static long getActualModId(AuditLogEntry ale)
    {
        try
        {
            if (ale.getReason() == null || !ale.getReason().contains(" "))
                return -2;
            return Long.parseLong(ale.getReason().substring(0, ale.getReason().indexOf(' ')));
        }
        catch (NumberFormatException e)
        {
            return -2;
        }
    }

    /**
     * Gets the actual reason of an action, as this filters out any data that the bot would have put in a reason
     * @param ale The audit log entry
     * @return The reason
     */
    private static String getActualReason(AuditLogEntry ale)
    {
        if (ale.getReason() == null || ale.getReason().trim().isEmpty())
            return "";
        if (!ale.getReason().trim().contains(" "))
            return ale.getReason();
        return ale.getReason().trim().substring(ale.getReason().indexOf(' ') + 1);
    }

    @Deprecated
    private static int getCaseNumber(Message m)
    {
        if(m.getAuthor().getIdLong()!=m.getJDA().getSelfUser().getIdLong())
            return -1;
        if(!m.getContentRaw().startsWith("`["))
            return -1;
        try
        {
            return Integer.parseInt(m.getContentRaw().substring(15, m.getContentRaw().indexOf("]` ",15)));
        }
        catch(Exception e)
        {
            return -1;
        }
    }

    @Deprecated
    private void getCaseNumberAsync(TextChannel tc, Consumer<Integer> result)
    {
        if(caseNum.containsKey(tc.getGuild().getIdLong()))
        {
            int num = caseNum.get(tc.getGuild().getIdLong());
            caseNum.put(tc.getGuild().getIdLong(), num+1);
            result.accept(num);
        }
        else
        {
            tc.getHistory().retrievePast(100).queue(list ->
            {
                int num;
                for(Message m: list)
                {
                    num = getCaseNumber(m);
                    if(num!=-1)
                    {
                        caseNum.put(tc.getGuild().getIdLong(), num+2);
                        result.accept(num+1);
                        return;
                    }
                }
                caseNum.put(tc.getGuild().getIdLong(), 2);
                result.accept(1);
            });
        }
    }
}