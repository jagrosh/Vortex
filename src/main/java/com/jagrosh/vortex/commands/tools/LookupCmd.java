/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://wwwidget.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Emoji;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.automod.InviteResolver;
import com.jagrosh.vortex.database.managers.PremiumManager;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.WidgetUtil;
import net.dv8tion.jda.api.utils.WidgetUtil.Widget;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LookupCmd extends Command
{
    private final static String USER_EMOJI = "\uD83D\uDC64"; // 👤
    private final static String GUILD_EMOJI = "\uD83D\uDDA5"; // 🖥
    private final static String UNKNOWN_ID = "\uD83C\uDD94"; // 🆔
    private final static String LINESTART = "\u25AB"; // ▫
    
    private final InviteResolver ir = new InviteResolver();
    private final Vortex vortex;
    
    public LookupCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "lookup";
        this.arguments = "<ID | invite>";
        this.help = "finds information about a user or server";
        this.cooldown = 5;
        this.category = new Category("Tools");
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.isFromType(ChannelType.TEXT) && event.getMember().getRoles().isEmpty())
        {
            event.reactError();
            return;
        }
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please provide a User ID, Server ID, or Invite Code\n"
                    + "This command provides information about a user or server. "
                    + "All of the information provided is information that Discord makes publically-available.");
            return;
        }
        event.getChannel().sendTyping().queue();
        event.async(() -> 
        {
            // determine if this is a valid ID
            long id = -1;
            try
            {
                id = Long.parseLong(event.getArgs());
            }
            catch(NumberFormatException ignore) {}
            
            // if it's valid and we find a user, we're done
            if(id > 0 && lookupUser(id, event))
                return;
            
            // require Vortex Plus for looking up guilds
            if(!vortex.getDatabase().premium.getPremiumInfo(event.getGuild()).level.isAtLeast(PremiumManager.Level.PLUS))
            {
                event.reply("No users found. Searching for guilds is not available here.\n" + PremiumManager.Level.PLUS.getRequirementMessage());
                return;
            }
            
            // if valid id, use widget, otherwise try invite code
            if(id > 0)
                lookupGuild(id, event);
            else
                lookupGuild(extractCode(event.getArgs()), event);
        });
    }
    
    private String extractCode(String args)
    {
        return args.substring(args.lastIndexOf("/")+1);
    }
    
    private boolean lookupUser(long userId, CommandEvent event)
    {
        User u = vortex.getShardManager().getUserById(userId);
        if(u==null) try
        {
            u = vortex.getShardManager().retrieveUserById(userId).complete(false);
        }
        catch(RateLimitedException ratelimited)
        {
            event.reactWarning();
            return true;
        }
        catch(Exception ignore) {}
        if(u == null)
            return false;
        String text = (u.isBot() ? Emoji.BOT : USER_EMOJI) + " Information about **" + u.getName() + "**:";
        EmbedBuilder eb = new EmbedBuilder();
        eb.setThumbnail(u.getEffectiveAvatarUrl());
        StringBuilder str = new StringBuilder(LINESTART + "Discord ID: **" + u.getId() + "** ");
        u.getFlags().forEach(flag -> str.append(OtherUtil.getEmoji(flag)));
        if(u.getAvatarId() != null && u.getAvatarId().startsWith("a_"))
            str.append(Emoji.BADGE_NITRO);
        str.append("\n" + LINESTART + "Account Creation: **").append(TimeUtil.getDateTimeString(u.getTimeCreated())).append("**");
        eb.setDescription(str.toString());
        event.reply(new MessageBuilder().append(FormatUtil.filterEveryone(text)).setEmbeds(eb.build()).build());
        return true;
    }
    
    private void lookupGuild(long guildId, CommandEvent event)
    {
        Invite invite = null;
        Widget widget = null;
        try
        {
            widget = WidgetUtil.getWidget(guildId);
        }
        catch(RateLimitedException ratelimited)
        {
            event.reactWarning();
            return;
        }
        catch(Exception ignore) {}
        if(widget != null && widget.isAvailable())
        {
            String invCode = widget.getInviteCode();
            if(invCode != null)
            {
                try
                {
                    invite = ir.resolveFull(invCode, event.getJDA());
                }
                catch(Exception ignore) {}
            }
        }
        event.reply(constructMessage(invite, widget, guildId));
    }
    
    private void lookupGuild(String inviteCode, CommandEvent event)
    {
        Invite invite = null;
        Widget widget = null;
        try
        {
            invite = ir.resolveFull(inviteCode, event.getJDA());
        }
        catch(Exception ex)
        {
            event.reactWarning();
            return;
        }
        if(invite != null)
        {
            Invite.Guild g = invite.getGuild();
            if(g != null)
            {
                try
                {
                    widget = WidgetUtil.getWidget(g.getIdLong());
                }
                catch(Exception ignore) {}
            }
        }
        event.reply(constructMessage(invite, widget, 0));
    }
    
    private Message constructMessage(Invite invite, Widget widget, long input)
    {
        String gname = null;
        long gid = 0L;
        int users = -1;
        if(invite == null)
        {
            if(widget == null)
            {
                if(input == 0)
                    return new MessageBuilder().append(Constants.ERROR + " No users, guilds, or invites found.").build();

                return new MessageBuilder()
                        .append(UNKNOWN_ID + " Information about an unknown ID:")
                        .setEmbeds(new EmbedBuilder()
                                .appendDescription(LINESTART + "Creation: " + "**"+TimeUtil.getTimeCreated(input).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**").build())
                        .build();
            }


            gid = widget.getIdLong();
            if(widget.isAvailable())
            {
                gname = widget.getName();
                users = widget.getMembers().size();
            }
        }
        else
        {
            Invite.Guild g = invite.getGuild();
            if(g != null)
            {
                gid = g.getIdLong();
                gname = g.getName();
                users = g.getOnlineCount();
            }
        }
        
        String text = GUILD_EMOJI + " Information about " + (gname == null ? "an unknown guild" : "**"+gname+"**") + ":";
        EmbedBuilder eb = new EmbedBuilder();
        eb.appendDescription(LINESTART + "ID: " + (gid == 0 ? "N/A" : "**"+gid+"**")
                + "\n" + LINESTART + "Creation: " + (gid == 0 ? "N/A" : "**"+TimeUtil.getTimeCreated(gid).format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**")
                + "\n" + LINESTART + "Users: " + (users == -1 ? "N/A" : "**" + users + "** online")
                + (widget != null && widget.isAvailable() ? "\n" + LINESTART + "Channels: **" + widget.getVoiceChannels().size() + "** voice" : ""));
        if(invite != null)
        {
            Invite.Guild g = invite.getGuild();
            eb.setThumbnail(g.getIconUrl());
            eb.setImage(g.getSplashId() == null ? null : g.getSplashUrl() + "?size=1024");
            eb.addField("Invite Info", LINESTART + "Invite: **" + invite.getCode() + "**"
                    + "\n" + LINESTART + "Channel: **" + (invite.getChannel().getType() == ChannelType.TEXT ? "#" : "") + invite.getChannel().getName() + "** (ID:" +invite.getChannel().getId() + ")"
                    + "\n" + LINESTART + "Inviter: " + (invite.getInviter() == null ? "N/A" : FormatUtil.formatFullUser(invite.getInviter()))
                    + (g.getSplashId() == null ? "" : "\n" + LINESTART + "Splash: "), false);
        }
        return new MessageBuilder().append(FormatUtil.filterEveryone(text)).setEmbeds(eb.build()).build();
    }
}
