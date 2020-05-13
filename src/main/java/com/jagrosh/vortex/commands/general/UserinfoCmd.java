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
package com.jagrosh.vortex.commands.general;

import java.time.format.DateTimeFormatter;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.utils.FormatUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.utils.MiscUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class UserinfoCmd extends Command
{
    private final static String BOT_EMOJI = "<:botTag:230105988211015680>";
    private final static String USER_EMOJI = "\uD83D\uDC64"; // 👤
    private final static String LINESTART = "\u25AB"; // ▫
    
    public UserinfoCmd()
    {
        this.name = "userinfo";
        this.aliases = new String[]{"user","uinfo","memberinfo"};
        this.help = "shows info on a member";
        this.arguments = "[user]";
        this.guildOnly = true;
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS};
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        Member member;
        if(event.getArgs().isEmpty())
        {
            member = event.getMember();
        }
        else
        {
            List<Member> found = FinderUtil.findMembers(event.getArgs(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the member you were looking for!");
                return;
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.filterEveryone(FormatUtil.listOfMember(found, event.getArgs())));
                return;
            }
            else
            {
                member = found.get(0);
            }
        }
        User user = member.getUser();
        String title = (user.isBot() ? BOT_EMOJI : USER_EMOJI)+" Information about **"+user.getName()+"** #"+user.getDiscriminator()+":";
        String str = LINESTART+"Discord ID: **"+user.getId()+"**"+(user.getAvatarId()!=null && user.getAvatarId().startsWith("a_")?" <:nitro:314068430611415041>":"");
        if(member.getNickname()!=null)
            str+="\n"+LINESTART+"Nickname: **"+member.getNickname()+"**";
        String roles="";
        roles = member.getRoles().stream().map((rol) -> "`, `"+rol.getName()).reduce(roles, String::concat);
        if(roles.isEmpty())
            roles="None";
        else
            roles=roles.substring(3)+"`";
        str+="\n"+LINESTART+"Roles: "+roles;
        str+="\n"+LINESTART+"Status: "+statusToEmote(member.getOnlineStatus(), member.getGame())+"**"+member.getOnlineStatus().name()+"**";
        Game game = member.getGame();
        if(game!=null)
            str+=" ("+formatGame(game)+")";
        str+="\n"+LINESTART+"Account Creation: **"+MiscUtil.getDateTimeString(MiscUtil.getCreationTime(user))+"**";
        
        List<Member> joins = new ArrayList<>(event.getGuild().getMembers());
        Collections.sort(joins, (Member a, Member b) -> a.getJoinDate().compareTo(b.getJoinDate()));
        int index = joins.indexOf(member);
        str+="\n"+LINESTART+"Guild Join Date: **"+member.getJoinDate().format(DateTimeFormatter.RFC_1123_DATE_TIME) + "** `(#"+(index+1)+")`";
        index-=3;
        if(index<0)
            index=0;
        str+="\n"+LINESTART+"Join Order: ";
        if(joins.get(index).equals(member))
            str+="[**"+joins.get(index).getUser().getName()+"**]()";
        else
            str+=joins.get(index).getUser().getName();
        for(int i=index+1;i<index+7;i++)
        {
            if(i>=joins.size())
                break;
            Member m = joins.get(i);
            String uname = m.getUser().getName();
            if(m.equals(member))
                uname="[**"+uname+"**]()";
            str+=" > "+uname;
        }
        
        event.reply(new MessageBuilder()
                .append(FormatUtil.filterEveryone(title))
                .setEmbed(new EmbedBuilder()
                        .setDescription(str)
                        .setThumbnail(user.getEffectiveAvatarUrl())
                        .setColor(member.getColor()).build())
                .build());
    }
    
    private static String statusToEmote(OnlineStatus status, Game game)
    {
        if(game!=null && game.getType()==Game.GameType.STREAMING && game.getUrl()!=null && Game.isValidStreamingUrl(game.getUrl()))
            return "<:streaming:313956277132853248>";
        switch(status) {
            case ONLINE: return "<:online:313956277808005120>";
            case IDLE: return "<:away:313956277220802560>";
            case DO_NOT_DISTURB: return "<:dnd:313956276893646850>";
            case INVISIBLE: return "<:invisible:313956277107556352>";
            case OFFLINE: return "<:offline:313956277237710868>";
            default: return "";
        }
    }
    
    private static String formatGame(Game game)
    {
        String str;
        switch(game.getType())
        {
            case STREAMING: 
                return "Streaming [*"+game.getName()+"*]("+game.getUrl()+")";
            case LISTENING: 
                str="Listening to"; 
                break;
            case WATCHING: 
                str="Watching"; 
                break;
            case DEFAULT:
            default:
                str="Playing"; 
                break;
        }
        return str+" *"+game.getName()+"*";
    }
}
