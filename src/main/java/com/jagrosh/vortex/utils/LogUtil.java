/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.utils;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LogUtil
{
    // Constants
    private final static String NO_REASON = "[no reason specified]";
    
    // Channel logging formats
    private final static String LOG_TIME = "`[%s]`";
    private final static String MODLOG_CASE = " `[%d]`";
    private final static String EMOJI = " %s";
    private final static String ACTION = " %s";
    private final static String MODERATOR = " **%s**#%s";
    private final static String TARGET_USER = " **%s**#%s (ID:%s)";
    private final static String REASON = "\n`[ Reason ]` %s";
    
    private final static String MODLOG_USER_FORMAT   = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + ACTION + TARGET_USER + REASON;
    private final static String MODLOG_TIME_FORMAT   = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + ACTION + TARGET_USER + " for %s" + REASON;
    private final static String MODLOG_CLEAN_FORMAT  = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + ACTION + " `%d` messages in %s\n`[Criteria]` %s" + REASON;
    private final static String MODLOG_STRIKE_FORMAT = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + " gave `%d` strikes `[%d → %d]` to" + TARGET_USER + REASON;
    private final static String MODLOG_PARDON_FORMAT = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + " pardoned `%d` strikes `[%d → %d]` from" + TARGET_USER + REASON;
    private final static String MODLOG_RAID_FORMAT   = LOG_TIME + MODLOG_CASE + EMOJI + MODERATOR + " `%s` anti-raid mode" + REASON;
    
    private final static String BASICLOG_FORMAT = LOG_TIME + EMOJI + " %s";
    
    // Modlog methods
    public static String modlogUserFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, Action action, User target, String reason)
    {
        return String.format(MODLOG_USER_FORMAT, timeF(time, zone), caseNum, action.getEmoji(), moderator.getName(), 
                moderator.getDiscriminator(), action.getVerb(), target.getName(), target.getDiscriminator(), target.getId(), 
                reasonF(reason));
    }
    
    public static String modlogTimeFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, Action action, int minutes, User target, String reason)
    {
        return String.format(MODLOG_TIME_FORMAT, timeF(time, zone), caseNum, action.getEmoji(), moderator.getName(), 
                moderator.getDiscriminator(), action.getVerb(), target.getName(), target.getDiscriminator(), target.getId(), 
                FormatUtil.secondsToTime(minutes*60), reasonF(reason));
    }
    
    public static String modlogCleanFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, int numMessages, TextChannel channel, String criteria, String reason)
    {
        return String.format(MODLOG_CLEAN_FORMAT, timeF(time, zone), caseNum, Action.CLEAN.getEmoji(), moderator.getName(), 
                moderator.getDiscriminator(), Action.CLEAN.getVerb(), numMessages, channel.getAsMention(), criteria, reasonF(reason));
    }
    
    public static String modlogStrikeFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, int givenStrikes, int oldStrikes, int newStrikes, User target, String reason)
    {
        return String.format(MODLOG_STRIKE_FORMAT, timeF(time, zone), caseNum, Action.STRIKE.getEmoji(), moderator.getName(),
                moderator.getDiscriminator(), givenStrikes, oldStrikes, newStrikes, target.getName(), target.getDiscriminator(), target.getId(), 
                reasonF(reason));
    }
    
    public static String modlogPardonFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, int pardonedStrikes, int oldStrikes, int newStrikes, User target, String reason)
    {
        return String.format(MODLOG_PARDON_FORMAT, timeF(time, zone), caseNum, Action.PARDON.getEmoji(), moderator.getName(),
                moderator.getDiscriminator(), pardonedStrikes, oldStrikes, newStrikes, target.getName(), target.getDiscriminator(), target.getId(), 
                reasonF(reason));
    }
    
    public static String modlogRaidFormat(OffsetDateTime time, ZoneId zone, int caseNum, User moderator, boolean enabled, String reason)
    {
        return String.format(MODLOG_RAID_FORMAT, timeF(time, zone), caseNum, enabled ? Action.RAIDMODE.getEmoji() : Action.NORAIDMODE.getEmoji(), moderator.getName(),
                moderator.getDiscriminator(), enabled ? "ENABLED" : "DISABLED", reasonF(reason));
    }
    
    public static int isCase(Message m, int caseNum)
    {
        if(m.getAuthor().getIdLong()!=m.getJDA().getSelfUser().getIdLong())
            return 0;
        String match = "(?is)`\\[.{8}\\]` `\\["+(caseNum==-1 ? "(\\d+)" : caseNum)+"\\]` .+";
        if(m.getContentRaw().matches(match) && (caseNum!=-1 || m.getContentRaw().endsWith(NO_REASON)))
            return caseNum==-1 ? Integer.parseInt(m.getContentRaw().replaceAll(match, "$1")) : caseNum;
        return 0;
    }
    
    // Basiclog methods
    public static String basiclogFormat(OffsetDateTime time, ZoneId zone, String emoji, String content)
    {
        return String.format(BASICLOG_FORMAT, timeF(time, zone), emoji, content);
    }
    
    public static String logMessagesForwards(String title, List<Message> messages)
    {
        TextChannel deltc = messages.get(0).getTextChannel();
        Guild delg = messages.get(0).getGuild();
        StringBuilder sb = new StringBuilder("-- "+title+" -- #"+deltc.getName()+" ("+deltc.getId()+") -- "+delg.getName()+" ("+delg.getId()+") --");
        Message m;
        for(int i=0; i<messages.size(); i++)
        {
            m = messages.get(i);
            sb.append("\r\n\r\n[")
                .append(m.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .append("] ").append(m.getAuthor().getName()).append("#").append(m.getAuthor().getDiscriminator())
                .append(" (").append(m.getAuthor().getId()).append(") : ").append(m.getContentRaw());
            m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        }
        return sb.toString().trim();
    }
    
    public static String logCachedMessagesForwards(String title, List<CachedMessage> messages, ShardManager shardManager)
    {
        TextChannel deltc = messages.get(0).getTextChannel(shardManager);
        Guild delg = deltc.getGuild();
        StringBuilder sb = new StringBuilder("-- "+title+" -- #"+deltc.getName()+" ("+deltc.getId()+") -- "+delg.getName()+" ("+delg.getId()+") --");
        CachedMessage m;
        for(int i=0; i<messages.size(); i++)
        {
            m = messages.get(i);
            User author = m.getAuthor(shardManager);
            sb.append("\r\n\r\n[")
                .append(m.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .append("] ");
            if(author==null)
                sb.append(m.getUsername()).append("#").append(m.getDiscriminator()).append(" (").append(m.getAuthorId());
            else
                sb.append(author.getName()).append("#").append(author.getDiscriminator()).append(" (").append(author.getId());
            sb.append(") : ").append(m.getContentRaw());
            m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        }
        return sb.toString().trim();
    }
    
    public static String logMessagesBackwards(String title, List<Message> messages)
    {
        TextChannel deltc = messages.get(0).getTextChannel();
        Guild delg = messages.get(0).getGuild();
        StringBuilder sb = new StringBuilder("-- "+title+" -- #"+deltc.getName()+" ("+deltc.getId()+") -- "+delg.getName()+" ("+delg.getId()+") --");
        Message m;
        for(int i=messages.size()-1; i>=0; i--)
        {
            m = messages.get(i);
            sb.append("\r\n\r\n[")
                .append(m.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME))
                .append("] ").append(m.getAuthor().getName()).append("#").append(m.getAuthor().getDiscriminator())
                .append(" (").append(m.getAuthor().getId()).append(") : ").append(m.getContentRaw());
            m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        }
        return sb.toString().trim();
    }
    
    // Audit logging formats
    private final static String A_MOD = "%s#%s";
    private final static String A_TIME = " (%dm)";
    private final static String A_REASON = ": %s";
    
    private final static String AUDIT_BASIC_FORMAT = A_MOD + A_REASON;
    private final static String AUDIT_TIMED_FORMAT = A_MOD + A_TIME + A_REASON;
    private final static Pattern AUDIT_BASIC_PATTERN = Pattern.compile("^(\\S.{0,32}\\S)#(\\d{4}): (.*)$", Pattern.DOTALL);
    private final static Pattern AUDIT_TIMED_PATTERN = Pattern.compile("^(\\S.{0,32}\\S)#(\\d{4}) \\((\\d{1,9})m\\): (.*)$", Pattern.DOTALL);
    
    // Auditlog methods
    public static String auditStrikeReasonFormat(Member moderator, int minutes, int oldstrikes, int newstrikes, String reason)
    {
        return auditReasonFormat(moderator, minutes, "["+oldstrikes+" → "+newstrikes+" strikes] "+reason);
    }
    
    public static String auditReasonFormat(Member moderator, String reason)
    {
        return limit512(String.format(AUDIT_BASIC_FORMAT, moderator.getUser().getName(), moderator.getUser().getDiscriminator(), reasonF(reason)));
    }
    
    public static String auditReasonFormat(Member moderator, int minutes, String reason)
    {
        if(minutes<=0)
            return auditReasonFormat(moderator, reason);
        return limit512(String.format(AUDIT_TIMED_FORMAT, moderator.getUser().getName(), moderator.getUser().getDiscriminator(), minutes, reasonF(reason)));
    }
    
    public static ParsedAuditReason parse(Guild guild, String reason)
    {
        if(reason==null || reason.isEmpty())
            return null;
        try
        {
            // first try the timed pattern
            Matcher m = AUDIT_TIMED_PATTERN.matcher(reason);
            if(m.find())
            {
                Member mem = OtherUtil.findMember(m.group(1), m.group(2), guild);
                if(mem==null)
                    return null;
                Integer minutes = Integer.parseInt(m.group(3));
                return new ParsedAuditReason(mem, minutes, reasonF(m.group(4)));
            }
            
            // next try the basic pattern
            m = AUDIT_BASIC_PATTERN.matcher(reason);
            if(m.find())
            {
                Member mem = OtherUtil.findMember(m.group(1), m.group(2), guild);
                if(mem==null)
                    return null;
                return new ParsedAuditReason(mem, 0, reasonF(m.group(3)));
            }
            
            // we got nothin
            return null;
        }
        catch(Exception e)
        {
            return null;
        }
    }
    
    public static class ParsedAuditReason
    {
        public final Member moderator;
        public final int minutes;
        public final String reason;
        
        private ParsedAuditReason(Member moderator, int minutes, String reason)
        {
            this.moderator = moderator;
            this.minutes = minutes;
            this.reason = reason;
        }
    }
    
    
    // Private methods
    private static String reasonF(String reason)
    {
        return reason==null || reason.isEmpty() ? NO_REASON : reason;
    }
    
    private static String timeF(OffsetDateTime time, ZoneId zone)
    {
        return time.atZoneSameInstant(zone).format(DateTimeFormatter.ISO_LOCAL_TIME).substring(0,8);
    }
    
    private static String limit512(String input)
    {
        if(input.length()<512)
            return input;
        return input.substring(0, 512);
    }
}
