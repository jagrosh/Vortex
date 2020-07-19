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
import com.jagrosh.vortex.utils.FixedCache;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.LogUtil.ParsedAuditReason;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.audit.AuditLogChange;
import net.dv8tion.jda.core.audit.AuditLogEntry;
import net.dv8tion.jda.core.audit.AuditLogKey;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.PermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ModLogger
{
    private final static Logger LOG = LoggerFactory.getLogger(ModLogger.class);
    private final HashMap<Long,Integer> caseNum = new HashMap<>();
    private final HashSet<Long> needsUpdate = new HashSet<>();
    private final FixedCache<String, Message> banLogCache = new FixedCache<>(1000);
    private final Vortex vortex;
    private boolean isStarted = false;
    
    public ModLogger(Vortex vortex)
    {
        this.vortex = vortex;
    }
    
    public void start()
    {
        if(isStarted)
            return;
        isStarted=true;
        
        vortex.getThreadpool().scheduleWithFixedDelay(()->
        {
            Set<Long> toUpdate;
            synchronized(needsUpdate)
            {
                toUpdate = new HashSet<>(needsUpdate);
                needsUpdate.clear();
            }
            if(!toUpdate.isEmpty())
            {
                LOG.debug("DEBUG Modlog updating " + toUpdate.size() + " guilds: " + toUpdate.toString());
                try
                {
                    long time, diff;
                    for(long gid: toUpdate)
                    {
                        time = System.currentTimeMillis();
                        update(vortex.getShardManager().getGuildById(gid), 40);
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
    
    public void setNeedUpdate(Guild guild)
    {
        if(vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild)==null)
            return;
        vortex.getThreadpool().schedule(() -> 
        {
            synchronized(needsUpdate)
            {
                needsUpdate.add(guild.getIdLong());
            }
        }, 2, TimeUnit.SECONDS);
    }
    
    public int updateCase(Guild guild, int num, String reason)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if(modlog==null)
            return -1;
        else if(!modlog.canTalk() || !guild.getSelfMember().hasPermission(modlog, Permission.MESSAGE_HISTORY))
            return -2;
        List<Message> list = modlog.getHistory().retrievePast(100).complete();
        Message m = null;
        int thiscase = 0;
        for(Message msg: list)
        {
            thiscase = LogUtil.isCase(msg, num);
            if(thiscase!=0)
            {
                m = msg;
                break;
            }
        }
        if(m==null)
            return num==-1 ? -4 : -3;
        m.editMessage(m.getContentRaw().replaceAll("(?is)\n`\\[ Reason \\]` .+", "\n`[ Reason ]` "+FormatUtil.filterEveryone(reason))).queue();
        return thiscase;
    }
    
    public void postCleanCase(Member moderator, OffsetDateTime now, int numMessages, TextChannel target, String criteria, String reason, MessageEmbed embed)
    {
        TextChannel modlog = vortex.getDatabase().settings.getSettings(moderator.getGuild()).getModLogChannel(moderator.getGuild());
        if(modlog==null || !modlog.canTalk())
            return;
        getCaseNumberAsync(modlog, i ->
        {
            try
            {
                modlog.sendMessage(new MessageBuilder()
                        .setEmbed(embed)
                        .append(FormatUtil.filterEveryone(LogUtil.modlogCleanFormat(now, 
                                vortex.getDatabase().settings.getSettings(moderator.getGuild()).getTimezone(), 
                                i, moderator.getUser(), numMessages, target, criteria, reason)))
                        .build()).queue();
            }
            catch(PermissionException ignore) {}
        });
    }
    
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
    
    private void update(Guild guild, int limit) // not async
    {
        if(guild==null)
            return;
        GuildSettings gs = vortex.getDatabase().settings.getSettings(guild);
        TextChannel modlog = gs.getModLogChannel(guild);
        if(modlog==null || !modlog.canTalk() || !modlog.getGuild().getSelfMember().hasPermission(Permission.VIEW_AUDIT_LOGS))
            return;
        Role mRole = gs.getMutedRole(guild);
        try
        {
            List<AuditLogEntry> list = guild.getAuditLogs().cache(false).limit(limit).submit().get(30, TimeUnit.SECONDS);
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
                        if(mRole==null)
                            break;
                        AuditLogChange added = ale.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD);
                        if(added!=null)
                        {
                            if (((ArrayList<HashMap<String,String>>)added.getNewValue()).stream().anyMatch(hm -> hm.get("id").equals(mRole.getId())))
                            {
                                act = Action.MUTE;
                                break;
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
                        }
                        break;
                    default:
                }
                if(act!=null)
                {
                    User mod = ale.getUser();
                    if(ale.getJDA().getSelfUser().equals(mod) && act==Action.MUTE && AutoMod.RESTORE_MUTE_ROLE_AUDIT.equals(ale.getReason()))
                        continue; // restoring muted role shouldn't trigger a log entry
                    String reason = ale.getReason()==null ? "" : ale.getReason();
                    int minutes = 0;
                    User target = vortex.getShardManager().getUserById(ale.getTargetIdLong());
                    if(target==null)
                        target = modlog.getJDA().retrieveUserById(ale.getTargetIdLong()).complete();
                    ZoneId timezone = vortex.getDatabase().settings.getSettings(guild).getTimezone();
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
                            }
                        }
                    }
                    String banCacheKey = banCacheKey(ale, mod);
                    if(act==Action.UNBAN) // check for softban
                    {
                        Message banMsg = banLogCache.get(banCacheKey);
                        if(banMsg!=null && banMsg.getCreationTime().plusMinutes(2).isAfter(ale.getCreationTime()))
                        {
                            // This is a softban, because the user was banned by the same mod within the past 2 minutes
                            // We need to edit the existing modlog entry instead of making a new one
                            banMsg.editMessage(banMsg.getContentRaw()
                                    .replaceFirst(Action.BAN.getEmoji(), Action.SOFTBAN.getEmoji())
                                    .replaceFirst(Action.BAN.getVerb(), Action.SOFTBAN.getVerb())).queue();
                            continue;
                        }
                        vortex.getDatabase().tempbans.clearBan(guild, ale.getTargetIdLong());
                    }
                    if(act==Action.UNMUTE)
                    {
                        vortex.getDatabase().tempmutes.removeMute(guild, ale.getTargetIdLong());
                    }
                    String msg = FormatUtil.filterEveryone(minutes > 0 ? 
                            LogUtil.modlogTimeFormat(ale.getCreationTime(), timezone, getCaseNumber(modlog), mod, act, minutes, target, reason) :
                            LogUtil.modlogUserFormat(ale.getCreationTime(), timezone, getCaseNumber(modlog), mod, act, target, reason));
                    if(act==Action.BAN)
                        banLogCache.put(banCacheKey, modlog.sendMessage(msg).complete());
                    else
                        modlog.sendMessage(msg).queue();
                }
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
    
    private int getCaseNumber(TextChannel tc) // not async
    {
        if(caseNum.containsKey(tc.getGuild().getIdLong()))
        {
            int num = caseNum.get(tc.getGuild().getIdLong());
            caseNum.put(tc.getGuild().getIdLong(), num+1);
            return num;
        }
        else
        {
            int num;
            for(Message m: tc.getHistory().retrievePast(100).complete())
            {
                num = getCaseNumber(m);
                if(num!=-1)
                {
                    caseNum.put(tc.getGuild().getIdLong(), num+2);
                    return num+1;
                }
            }
            caseNum.put(tc.getGuild().getIdLong(), 2);
            return 1;
        }
    }
    
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
    
    private static String banCacheKey(AuditLogEntry ale, User mod)
    {
        return ale.getGuild().getId()+"|"+ale.getTargetId()+"|"+mod.getId();
    }
}
