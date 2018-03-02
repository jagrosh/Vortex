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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.List;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CheckCmd extends ModCommand
{
    public CheckCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "check";
        this.arguments = "<user>";
        this.help = "checks a user";
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty() || event.getArgs().equalsIgnoreCase("help"))
        {
            event.replySuccess("This command is used to see a user's strikes and mute/ban status for the current server. Please include a user or user ID to check.");
            return;
        }
        List<Member> members = FinderUtil.findMembers(event.getArgs(), event.getGuild());
        if(!members.isEmpty())
        {
            check(event, members.get(0).getUser());
            return;
        }
        List<User> users = FinderUtil.findUsers(event.getArgs(), event.getJDA());
        if(!users.isEmpty())
        {
            check(event, users.get(0));
            return;
        }
        try
        {
            Long uid = Long.parseLong(event.getArgs());
            User u = vortex.getShardManager().getUserById(uid);
            if(u!=null)
                check(event, u);
            else
                event.getJDA().retrieveUserById(uid).queue(
                        user -> check(event, user), 
                        v -> event.replyError("`"+uid+"` is not a valid user ID"));
        }
        catch(Exception ex)
        {
            event.replyError("Could not find a user `"+event.getArgs()+"`");
        }
    }
    
    private void check(CommandEvent event, User user)
    {
        int strikes = vortex.getDatabase().strikes.getStrikes(event.getGuild(), user.getIdLong());
        int minutesMuted = vortex.getDatabase().tempmutes.timeUntilUnmute(event.getGuild(), user.getIdLong());
        boolean actuallyMuted = event.getGuild().isMember(user) && event.getGuild().getMember(user).getRoles().stream().anyMatch(r -> r.getName().equalsIgnoreCase("Muted"));
        int minutesBanned = vortex.getDatabase().tempbans.timeUntilUnban(event.getGuild(), user.getIdLong());
        String str = "Moderation Information for "+FormatUtil.formatFullUser(user)+":\n"
                + Action.STRIKE.getEmoji() + " Strikes: **"+strikes+"**\n";
        if(minutesMuted<=0)
            str += actuallyMuted ? Action.MUTE.getEmoji() + " Muted: **Manually**" : Action.UNMUTE.getEmoji() + " Not Muted";
        else
            str += (minutesMuted==Integer.MAX_VALUE 
                    ? Action.MUTE.getEmoji() + " Muted: **Forever**" 
                    : Action.TEMPMUTE.getEmoji() + " Muted: " + FormatUtil.secondsToTime(minutesMuted*60))
                    + (actuallyMuted ? "" : " " + Constants.WARNING + " No 'Muted' role!");
        str += minutesBanned<=0 ? "" : "\n" + Action.TEMPBAN.getEmoji() + " Banned: " + FormatUtil.secondsToTime(minutesBanned*60);
        event.replySuccess(str);
    }
}
