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
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
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
        sendLog(message.getGuild(), formatCommandLog(message));
    }
    
    public static void logAction(Action action, Guild guild, User user, String reason)
    {
        sendLog(guild, formatActionLog(action,user,reason));
    }
    
    
    private static String formatActionLog(Action action, User user, String reason)
    {
        return time()+" User "+FormatUtil.formatFullUser(user)+" was automatically "+action.getVerb()+" for:\n```\n"+reason+" ```";
    }
    
    private static String formatCommandLog(Message command)
    {
        return time()+" "+formatUser(command.getAuthor())+" used the following command in "+command.getTextChannel().getAsMention()
                +":\n```\n"+command.getContent()+" ```";
    }
    
    private static String time()
    {
        return "`["+(OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME).split("\\.")[0])+"]`";
    }
    
    private static void sendLog(Guild guild, String toSend)
    {
        guild.getTextChannels()
                .stream().filter(tc -> ((tc.getName().startsWith("mod") && tc.getName().endsWith("log")) || tc.getName().contains("modlog")) 
                        && PermissionUtil.checkPermission(tc, guild.getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ))
                .findFirst().ifPresent(tc -> tc.sendMessage(toSend).queue());
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
