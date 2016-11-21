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
package vortex;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.utils.FormatUtil;
import static vortex.utils.FormatUtil.formatUser;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ModLogger {
    
    public static void logCommand(Message message)
    {
        logCommand(message, null);
    }
    
    public static void logCommand(Message message, String extra)
    {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(message.getGuild().getSelfMember().getColor())
                .setTimestamp(message.getCreationTime())
                .setFooter(message.getAuthor().getName()+" #"+message.getAuthor().getDiscriminator(), 
                        message.getAuthor().getAvatarUrl()==null ? message.getAuthor().getDefaultAvatarUrl() : message.getAuthor().getAvatarUrl())
                .setDescription(message.getTextChannel().getAsMention()+": "+message.getRawContent()+(extra==null ? "" : extra))
                .build();
        sendLog(message.getGuild(), embed);
    }
    
    public static void logAction(Action action, Message message, String reason)
    {
        MessageEmbed embed = new EmbedBuilder()
                .setColor(message.getGuild().getSelfMember().getColor())
                .setTimestamp(message.getCreationTime())
                .setFooter(message.getJDA().getSelfUser().getName()+" automod", 
                        message.getJDA().getSelfUser().getAvatarUrl()==null ? message.getJDA().getSelfUser().getDefaultAvatarUrl() : message.getJDA().getSelfUser().getAvatarUrl())
                .setDescription(message.getAuthor().getAsMention()+" was automatically **"+action.getVerb()+"** for:\n```\n"+reason+" ```")
                .build();
        sendLog(message.getGuild(), embed);
    }
    
    
    private static String formatActionLog(Action action, User user, String reason)
    {
        return time()+" User "+FormatUtil.formatFullUser(user)+" was automatically "+action.getVerb()+" for:\n```\n"+reason+" ```";
    }
    
    private static String formatCommandLog(Message command, String extra)
    {
        return time()+" "+formatUser(command.getAuthor())+" used the following command in "+command.getTextChannel().getAsMention()
                +":\n```\n"+command.getContent()+(extra==null ? "" : extra)+" ```";
    }
    
    private static String time()
    {
        return "`["+(OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).split("\\.")[0])+"]`";
    }
    
    private static void sendLog(Guild guild, MessageEmbed embed)
    {
        guild.getTextChannels()
                .stream().filter(tc -> ((tc.getName().startsWith("mod") && tc.getName().endsWith("log")) || tc.getName().contains("modlog")) 
                        && PermissionUtil.checkPermission(tc, guild.getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                .findFirst().ifPresent(tc -> tc.sendMessage(new MessageBuilder().setEmbed(embed).build()).queue());
    }
    
    public enum Action {
        BAN("banned"), KICK("kicked"), MUTE("muted"), WARN("warned"), DELETE("deleted");
        
        private final String verb;
        private Action(String verb)
        {
            this.verb = verb;
        }
        public String getVerb()
        {
            return verb;
        }
    }
}
