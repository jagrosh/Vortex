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

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
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
public class SoftbanCmd extends Command {
    private final ScheduledExecutorService threadpool;
    
    public SoftbanCmd(ScheduledExecutorService threadpool)
    {
        this.threadpool = threadpool;
        this.name = "softban";
        this.arguments = "@user [@user...]";
        this.help = "softbans all mentioned users (bans and unbans to remove messages)";
        this.requiredPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.type = Type.GUILDONLY;
    }

    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        if(!PermissionUtil.checkPermission(event.getGuild(), event.getGuild().getSelfMember(), Permission.BAN_MEMBERS))
            return reply(String.format(Constants.BOT_NEEDS_PERMISSION,Permission.BAN_MEMBERS,"server"),event);
        if(event.getMessage().getMentionedUsers().isEmpty())
            return reply(String.format(Constants.NEED_MENTION, "user"),event);
        if(event.getMessage().getMentionedUsers().size()>20)
            return reply(Constants.ERROR+"Up to 20 users can be softbanned at once.",event);
        VortexStringBuilder builder = new VortexStringBuilder(event.getMessage().getMentionedUsers().size(), s -> {reply(s,event);});
        event.getMessage().getMentionedUsers().stream().forEach(u -> {
            Member m = event.getGuild().getMember(u);
            if(m==null || PermissionUtil.canInteract(event.getMember(), m))
            {
                try 
                {
                    event.getGuild().getController().ban(u, 1).queue(v -> {
                        builder.append("\n").append(Constants.SUCCESS).append("Successfully softbanned ").append(u.getAsMention()).increment();
                        threadpool.schedule(()->{
                            event.getGuild().getController().unban(u.getId()).queue();
                        }, 5, TimeUnit.SECONDS);
                    }, t -> {
                        if(t instanceof PermissionException)
                            builder.append("\n").append(Constants.ERROR).append("I do not have permission to softban ").append(FormatUtil.formatUser(u));
                        else
                            builder.append("\n").append(Constants.ERROR).append("I cannot softban ").append(FormatUtil.formatUser(u));
                        builder.increment();
                    });
                } catch(PermissionException e)
                {
                    builder.append("\n").append(Constants.ERROR).append("I do not have permission to softban ").append(FormatUtil.formatUser(u)).increment();
                }
            }
            else
            {
                builder.append("\n").append(Constants.ERROR).append("You do not have permission to softban ").append(FormatUtil.formatUser(u)).increment();
            }
        });
        ModLogger.logCommand(event.getMessage());
        return null;
    }
}
