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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.PunishmentManager.Punishment;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class StrikeHandler
{
    private final static String STRIKE_FORMAT = Constants.WARNING+" You have received `%d` strikes in **%s** for: `%s`";
    private final static String PARDON_FORMAT = Constants.SUCCESS+" You have been pardoned `%d` strikes in **%s** for: `%s`";
    private final static String PUNISH_FORMAT = "\n%s You have been **%s** from **%s**";
    private final static String PUNISH_FORMAT_TIME = "\n%s You have been **%s** for %s from **%s**";
    
    private final Vortex vortex;
    
    public StrikeHandler(Vortex vortex)
    {
        this.vortex = vortex;
    }
    
    public void pardonStrikes(Member moderator, OffsetDateTime nowo, long targetId, int number, String reason)
    {
        int[] counts = vortex.getDatabase().strikes.removeStrikes(moderator.getGuild(), targetId, number);
        User user = vortex.getShardManager().getUserById(targetId);
        if(user==null)
        {
            moderator.getJDA().retrieveUserById(targetId).queue(u -> vortex.getModLogger().postPardonCase(moderator, nowo, number, counts[0], counts[1], u, reason));
        }
        else
        {
            String dmmsg = String.format(PARDON_FORMAT, number, moderator.getGuild().getName(), reason);
            vortex.getModLogger().postPardonCase(moderator, nowo, number, counts[0], counts[1], user, reason);
            OtherUtil.safeDM(user, dmmsg, ()->{});
        }
    }
    
    public void applyStrikes(Member moderator, OffsetDateTime nowo, User target, int number, String reason)
    {
        applyStrikes(moderator, nowo, target.getIdLong(), number, reason);
    }
    
    public void applyStrikes(Member moderator, OffsetDateTime nowo, long targetId, int number, String reason)
    {
        //reason = reason.length()>400 ? reason.substring(0, 400) : reason;
        Instant now = nowo.toInstant();
        int[] counts = vortex.getDatabase().strikes.addStrikes(moderator.getGuild(), targetId, number);
        List<Punishment> punishments = vortex.getDatabase().actions.getPunishments(moderator.getGuild(), counts[0], counts[1]);
        User user = vortex.getShardManager().getUserById(targetId);
        String dmmsg = String.format(STRIKE_FORMAT, number, moderator.getGuild().getName(), reason);
        if(punishments.isEmpty())
        {
            if(user==null)
            {
                moderator.getJDA().retrieveUserById(targetId).queue(u -> vortex.getModLogger().postStrikeCase(moderator, nowo, number, counts[0], counts[1], u, reason));
            }
            else
            {
                vortex.getModLogger().postStrikeCase(moderator, nowo, number, counts[0], counts[1], user, reason);
                OtherUtil.safeDM(user, dmmsg, ()->{});
            }
        }
        else
        {
            String notimeaudit = LogUtil.auditStrikeReasonFormat(moderator, 0, counts[1], reason);
            if(punishments.stream().anyMatch(p -> p.action==Action.BAN))
            {
                OtherUtil.safeDM(user, dmmsg + punish(Action.BAN, moderator.getGuild()), 
                        () -> moderator.getGuild().getController().ban(Long.toString(targetId), 7, notimeaudit).queue());
                vortex.getDatabase().tempbans.clearBan(moderator.getGuild(), targetId);
                return;
            }
            int muteDuration = 0;
            int banDuration = 0;
            for(Punishment p: punishments)
            {
                if(p.action==Action.MUTE)
                    muteDuration = Integer.MAX_VALUE;
                else if(p.action==Action.TEMPMUTE && p.time>muteDuration)
                    muteDuration = p.time;
                else if(p.action==Action.TEMPBAN && p.time>banDuration)
                    banDuration = p.time;
            }
            if(banDuration>0)
            {
                int finalBanDuration = banDuration;
                OtherUtil.safeDM(user, dmmsg + punishTime(Action.TEMPBAN, moderator.getGuild(), banDuration), 
                        () -> moderator.getGuild().getController().ban(Long.toString(targetId), 7, LogUtil.auditStrikeReasonFormat(moderator, finalBanDuration, counts[1], reason)).queue());
                vortex.getDatabase().tempbans.setBan(moderator.getGuild(), targetId, now.plus(banDuration, ChronoUnit.MINUTES));
                if(muteDuration>0)
                    vortex.getDatabase().tempmutes.setMute(moderator.getGuild(), targetId, muteTime(now, muteDuration));
                return;
            }
            if(punishments.stream().anyMatch(p -> p.action==Action.SOFTBAN))
            {
                OtherUtil.safeDM(user, dmmsg + punish(Action.SOFTBAN, moderator.getGuild()), 
                        () -> moderator.getGuild().getController().ban(Long.toString(targetId), 7, notimeaudit).queue(
                                s -> moderator.getGuild().getController().unban(Long.toString(targetId)).reason(notimeaudit).queueAfter(5, TimeUnit.SECONDS)));
                if(muteDuration>0)
                    vortex.getDatabase().tempmutes.setMute(moderator.getGuild(), targetId, muteTime(now, muteDuration));
                return;
            }
            if(punishments.stream().anyMatch(p -> p.action==Action.KICK))
            {
                if(user!=null && moderator.getGuild().isMember(user))
                {
                    OtherUtil.safeDM(user, dmmsg + punish(Action.KICK, moderator.getGuild()), 
                        () -> moderator.getGuild().getController().kick(Long.toString(targetId), notimeaudit).queue());
                }
                else
                {
                    vortex.getModLogger().postStrikeCase(moderator, nowo, number, counts[0], counts[1], user, reason);
                }
                if(muteDuration>0)
                    vortex.getDatabase().tempmutes.setMute(moderator.getGuild(), targetId, muteTime(now, muteDuration));
                return;
            }
            if(muteDuration>0)
            {
                vortex.getDatabase().tempmutes.setMute(moderator.getGuild(), targetId, muteTime(now, muteDuration));
                Role muted = moderator.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("Muted")).findFirst().orElse(null);
                Member mem = moderator.getGuild().getMemberById(targetId);
                if(muted==null || mem==null)
                {
                    vortex.getModLogger().postStrikeCase(moderator, nowo, number, counts[0], counts[1], user, reason);
                    OtherUtil.safeDM(user, dmmsg, ()->{});
                    return;
                }
                if(mem.getRoles().contains(muted))
                {
                    vortex.getModLogger().postPseudoCase(moderator, nowo, 
                            muteDuration==Integer.MAX_VALUE ? Action.MUTE : Action.TEMPMUTE, user, 
                            muteDuration==Integer.MAX_VALUE ? 0 : muteDuration, "["+counts[1]+" strikes] "+reason);
                }
                else
                {
                    moderator.getGuild().getController().addSingleRoleToMember(mem, muted)
                        .reason(muteDuration==Integer.MAX_VALUE ? notimeaudit : LogUtil.auditStrikeReasonFormat(moderator, muteDuration, counts[1], reason))
                        .queue();
                }
                OtherUtil.safeDM(user, dmmsg + (muteDuration==Integer.MAX_VALUE ? punish(Action.MUTE, moderator.getGuild()) 
                        : punishTime(Action.TEMPMUTE, moderator.getGuild(), muteDuration)), ()->{});
            }
        }
    }
    
    private static String punish(Action action, Guild guild)
    {
        return String.format(PUNISH_FORMAT, action.getEmoji(), action.getVerb(), guild.getName());
    }
    
    private static String punishTime(Action action, Guild guild, int minutes)
    {
        return String.format(PUNISH_FORMAT_TIME, action.getEmoji(), action.getVerb(), FormatUtil.secondsToTime(minutes*60), guild.getName());
    }
    
    private static Instant muteTime(Instant now, int minutes)
    {
        return minutes==Integer.MAX_VALUE ? Instant.MAX : now.plus(minutes, ChronoUnit.MINUTES);
    }
}
