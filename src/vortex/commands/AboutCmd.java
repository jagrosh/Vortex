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
package vortex.commands;

import java.awt.Color;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDAInfo;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import vortex.Bot;
import vortex.Command;
import vortex.Constants;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AboutCmd extends Command {

    public AboutCmd()
    {
        this.name = "about";
        this.help = "shows info about the bot";
    }
    
    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        EmbedBuilder builder = new EmbedBuilder();
        builder.setColor(event.getGuild()==null ? Color.CYAN : event.getGuild().getSelfMember().getColor());
        builder.setAuthor("All about "+event.getJDA().getSelfUser().getName()+"!", null, event.getJDA().getSelfUser().getAvatarUrl());
        builder.setDescription("Hello! I am **"+event.getJDA().getSelfUser().getName()+"** and I'm here to keep your Discord server safe and make moderating easy!"
                + "\nI was written in Java by **jagrosh** using the JDA library ("+JDAInfo.VERSION+") <:jda:230988580904763393>"
                + "\nTake a look at my commands by typing `"+Constants.PREFIX+"help`"
                + "\nJoin my server [`here`]("+Constants.SERVER_INVITE+"), or [`invite`]("+Constants.BOT_INVITE+") me to your server!"
                + "\n\nSome of my features include: ```css"
                + "\n\u2611 Moderation commands"
                + "\n\u2611 Configurable automoderation"
                + "\n\u2611 Very easy setup [coming soon] ```");
        builder.addField("Servers", Integer.toString(event.getJDA().getGuilds().size()), true);
        builder.addField("Users", event.getJDA().getUsers().size()+" unique\n"
                +event.getJDA().getUsers().stream().filter(u -> 
                    {
                        try{
                            OnlineStatus status = event.getJDA().getGuilds().stream().filter(g -> g.isMember(u)).findAny().get().getMember(u).getOnlineStatus();
                            return status == OnlineStatus.ONLINE || status == OnlineStatus.IDLE || status == OnlineStatus.DO_NOT_DISTURB || status == OnlineStatus.INVISIBLE;
                        } catch(Exception e){
                            return false;
                        }
                    }
                ).count()+" online", true);
        builder.addField("Channels", event.getJDA().getTextChannels().size()+" Text\n"+event.getJDA().getVoiceChannels().size()+" Voice", true);
        builder.setFooter("Last restart", null);
        builder.setTimestamp(Bot.start);
        return reply(builder.build(),event);
    }
    
}
