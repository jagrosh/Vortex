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
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.utils.MiscUtil;
import net.dv8tion.jda.core.utils.WidgetUtil;
import net.dv8tion.jda.core.utils.WidgetUtil.Widget;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class LookupCmd extends Command
{
    private final Vortex vortex;
    private final String linestart = "\u25AB";
    
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
                catch(Exception ex) {}
                if(u!=null)
                {
                    String text = (u.isBot()?"<:botTag:230105988211015680>":"\uD83D\uDC64")+" Information about **"+u.getName()+"**#"+u.getDiscriminator()+":";
                    EmbedBuilder eb = new EmbedBuilder();
                    eb.setThumbnail(u.getEffectiveAvatarUrl());
                    String str = linestart+"Discord ID: **"+u.getId()+"**";
                    if(u.getAvatarId()!=null && u.getAvatarId().startsWith("a_"))
                        str+= " <:nitro:314068430611415041>";
                    str+="\n"+linestart+"Account Creation: **"+MiscUtil.getDateTimeString(u.getCreationTime())+"**";
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
                        catch(Exception ex){}
                    }
                    String text = "\uD83D\uDDA5 Information about **"+widget.getName()+"**:";
                    EmbedBuilder eb = new EmbedBuilder();
                    String str = linestart+"ID: **"+widget.getId()+"**\n"
                        +linestart+"Creation: **"+widget.getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n"
                        +linestart+"Channels: **"+widget.getVoiceChannels().size()+"** Voice\n"
                        +linestart+"Users: **"+widget.getMembers().size()+"** online\n";
                    if(inv!=null)
                    {
                        eb.setThumbnail(inv.getGuild().getIconUrl());
                        str+=linestart+"Invite: **"+inv.getCode()+"** "+(inv.getChannel().getType()==ChannelType.TEXT 
                                ? "#"+inv.getChannel().getName() : inv.getChannel().getName())+" (ID:"+inv.getChannel().getId()+")";
                        if(inv.getGuild().getSplashId()!=null)
                        {
                            str+="\n\n<:partner:314068430556758017> **Discord Partner** <:partner:314068430556758017>";
                            eb.setImage(inv.getGuild().getSplashUrl()+"?size=1024");
                        }
                    }
                    eb.setDescription(str);
                    event.reply(new MessageBuilder().append(text).setEmbed(eb.build()).build());
                    return;
                }
            }
            catch(NumberFormatException ex)
            {
            }
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
            catch(Exception ex){}
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
            catch(RateLimitedException ex) {}
            String text = "\uD83D\uDDA5 Information about Invite Code **"+code+"**:";
            EmbedBuilder eb = new EmbedBuilder();
            eb.setThumbnail(inv.getGuild().getIconUrl());
            String str = linestart+"Guild: **"+inv.getGuild().getName()+"**\n"
                    + linestart+"Channel: **"+(inv.getChannel().getType()==ChannelType.TEXT?"#":"")+inv.getChannel().getName()+"** (ID:"+inv.getChannel().getId()+")\n"
                    + linestart+"Inviter: "+(inv.getInviter()==null?"N/A":"**"+inv.getInviter().getName()+"**#"+inv.getInviter().getDiscriminator()+" (ID:"+inv.getInviter().getId()+")");
            eb.setDescription(str);
            str = linestart+"ID: **"+inv.getGuild().getId()+"**\n"
                        +linestart+"Creation: **"+inv.getGuild().getCreationTime().format(DateTimeFormatter.RFC_1123_DATE_TIME)+"**\n";
            if(widget!=null && widget.isAvailable())
                str += linestart+"Channels: **"+widget.getVoiceChannels().size()+"** Voice\n"
                      +linestart+"Users: **"+widget.getMembers().size()+"** online";
            if(inv.getGuild().getSplashId()!=null)
            {
                str+="\n\n<:partner:314068430556758017> **Discord Partner** <:partner:314068430556758017>";
                eb.setImage(inv.getGuild().getSplashUrl()+"?size=1024");
            }
            eb.addField("Guild Info", str, false);
            event.reply(new MessageBuilder().append(text).setEmbed(eb.build()).build());
        });
    }
}
