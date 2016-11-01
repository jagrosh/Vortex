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

import java.util.ArrayList;
import java.util.List;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.Command;
import vortex.Constants;
import vortex.ModLogger;
import vortex.entities.VortexStringBuilder;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class HackbanCmd extends Command {
    
    public HackbanCmd()
    {
        this.name = "hackban";
        this.arguments = "userId [userId...]";
        this.help = "bans all the listed user IDs";
        this.requiredPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.type = Type.GUILDONLY;
    }

    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        if(!PermissionUtil.checkPermission(event.getGuild(), event.getGuild().getSelfMember(), Permission.BAN_MEMBERS))
            return reply(String.format(Constants.BOT_NEEDS_PERMISSION,Permission.BAN_MEMBERS,"server"),event);
        String[] words = args.split("\\s+");
        List<String> ids = new ArrayList<>();
        for(String word: words)
        {
            if(word.matches("\\d{17,20}"))
                ids.add(word);
            else if(word.matches("<@\\d{17,20}>"))
                ids.add(word.substring(2,word.length()-1));
            else if(word.matches("<@!\\d{17,20}>"))
                ids.add(word.substring(3, word.length()-1));
        }
        if(ids.isEmpty())
            return reply(String.format(Constants.NEED_X, "User ID"),event);
        if(ids.size()>20)
            return reply(Constants.ERROR+"Up to 20 users can be banned at once.",event);
        VortexStringBuilder builder = new VortexStringBuilder(event.getMessage().getMentionedUsers().size(), (s) -> {reply(s,event);});
        ids.stream().forEach(id -> {
            Member m;
            User u = event.getJDA().getUserById(id);
            if(u!=null)
                m = event.getGuild().getMember(u);
            else
                m = null;
            String formatted = (u==null ? "User with ID "+id : FormatUtil.formatUser(u));
            if(m==null || PermissionUtil.canInteract(event.getMember(), m))
            {
                try 
                {
                    event.getGuild().getController().ban(id, 1).queue((v) -> {
                        builder.append("\n").append(Constants.SUCCESS).append("Successfully banned ").append("<@"+id+">").increment();
                    }, (t) -> {
                        if(t instanceof PermissionException)
                            builder.append("\n").append(Constants.ERROR).append("I do not have permission to ban ").append(formatted);
                        else
                            builder.append("\n").append(Constants.ERROR).append("I cannot ban ").append(formatted);
                        builder.increment();
                    });
                } catch(PermissionException e)
                {
                    builder.append("\n").append(Constants.ERROR).append("I do not have permission to ban ").append(formatted).increment();
                }
            }
            else
            {
                builder.append("\n").append(Constants.ERROR).append("You do not have permission to ban ").append(formatted).increment();
            }
        });
        ModLogger.logCommand(event.getMessage());
        return null;
    }
}
