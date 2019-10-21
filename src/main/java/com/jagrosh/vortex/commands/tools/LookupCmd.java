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
import com.jagrosh.vortex.Vortex;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.RateLimitedException;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.api.utils.WidgetUtil;
import net.dv8tion.jda.api.utils.WidgetUtil.Widget;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LookupCmd extends Command
{
    private final static String BOT_EMOJI = "<:botTag:230105988211015680>";
    private final static String USER_EMOJI = "\uD83D\uDC64"; // 👤
    private final static String GUILD_EMOJI = "\uD83D\uDDA5"; // 🖥
    private final static String LINESTART = "\u25AB"; // ▫
    
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
            try
            {
                long id = Long.parseLong(event.getArgs());
                User u = vortex.getShardManager().getUserById(id);
                if(u==null) try
                {
                    u = event.getJDA().retrieveUserById(id).complete();
                }
                catch(Exception ignore) {}
                if(u!=null)
                {
                    String text = (u.isBot() ? BOT_EMOJI : USER_EMOJI)+" Information about **"+u.getName()+"**#"+u.getDiscriminator()+":";
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setThumbnail(u.getEffectiveAvatarUrl());
                    String str = LINESTART+"Discord ID: **"+u.getId()+"**";
                    if(u.getAvatarId()!=null && u.getAvatarId().startsWith("a_"))
                        str+= " <:nitro:314068430611415041>";
                    str+="\n"+LINESTART+"Account Creation: **"+MiscUtil.getDateTimeString(u.getTimeCreated())+"**";
                    eb.setDescription(str);
                    event.reply(new MessageBuilder().append(text).setEmbed(eb.build()).build());
                    return;
                }
                
                Widget widget = WidgetUtil.getWidget(id);
                if(widget!=null)
                {
                    if(!widget.isAvailable())
                    {
                        event.replySuccess("Guild with ID `"+id+"` found; no further information found.");
                        return;
                    }
                    Invite inv = null;
                    if(widget.getInviteCode()!=null)
                    {
                        try
                        {
                            inv = Invite.resolve(event.getJDA(), widget.getInviteCode()).complete();
                        }
                        catch(Exception ignore){}
                    }
                    String text = GUILD_EMOJI + " Information about **"+widget.getName()+"**:";
                    EmbedBuilder eb = new EmbedBuilder();
                    String str = LINESTART+"ID: **"+widget.getId()+"**\n"
                        +LINESTART+"Creation: **"+widget.getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
                        +LINESTART+"Channels: **"+widget.getVoiceChannels().size()+"** Voice\n"
                        +LINESTART+"Users: **"+widget.getMembers().size()+"** online\n";
                    if(inv!=null)
                    {
                        eb.setThumbnail(inv.getGuild().getIconUrl());
                        str+=LINESTART+"Invite: **"+inv.getCode()+"** "+(inv.getChannel().getType()==ChannelType.TEXT 
                                ? "#"+inv.getChannel().getName() : inv.getChannel().getName())+" (ID:"+inv.getChannel().getId()+")";
                        
                        if(inv.getGuild().getSplashId()!=null)
                        {
                            str += "\n"+LINESTART+"Splash: ";
                            eb.setImage(inv.getGuild().getSplashUrl()+"?size=1024");
                        }
                    }
                    eb.setDescription(str);
                    event.reply(new MessageBuilder().append(text).setEmbed(eb.build()).build());
                    return;
                }
            }
            catch(NumberFormatException ignore) {}
            catch(RateLimitedException ex)
            {
                event.reactWarning();
                return;
            }
            catch(Exception ex)
            {
                ex.printStackTrace();
            }
            String code = event.getArgs().substring(event.getArgs().indexOf("/")+1);
            Invite inv = null;
            try
            {
                inv = Invite.resolve(event.getJDA(), code).complete();
            }
            catch(Exception ignore){}
            if(inv==null)
            {
                event.replyError("No users, guilds, or invites found.");
                return;
            }
            Widget widget = null;
            try
            {
                widget = WidgetUtil.getWidget(inv.getGuild().getIdLong());
            }
            catch(RateLimitedException ignore) {}
            String text = GUILD_EMOJI + " Information about Invite Code **"+code+"**:";
            EmbedBuilder eb = new EmbedBuilder();
            eb.setThumbnail(inv.getGuild().getIconUrl());
            String str = LINESTART+"Guild: **"+inv.getGuild().getName()+"**\n"
                    + LINESTART+"Channel: **"+(inv.getChannel().getType()==ChannelType.TEXT?"#":"")+inv.getChannel().getName()+"** (ID:"+inv.getChannel().getId()+")\n"
                    + LINESTART+"Inviter: "+(inv.getInviter()==null?"N/A":"**"+inv.getInviter().getName()+"**#"+inv.getInviter().getDiscriminator()+" (ID:"+inv.getInviter().getId()+")");
            eb.setDescription(str);
            str = LINESTART+"ID: **"+inv.getGuild().getId()+"**\n"
                        +LINESTART+"Creation: **"+inv.getGuild().getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n";
            if(widget!=null && widget.isAvailable())
                str += LINESTART+"Channels: **"+widget.getVoiceChannels().size()+"** Voice\n"
                      +LINESTART+"Users: **"+widget.getMembers().size()+"** online";
            if(inv.getGuild().getSplashId()!=null)
            {
                str += "\n"+LINESTART+"Splash: ";
                eb.setImage(inv.getGuild().getSplashUrl()+"?size=1024");
            }
            eb.addField("Guild Info", str, false);
            event.reply(new MessageBuilder().append(text).setEmbed(eb.build()).build());
        });
    }
}
