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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.Command;
import vortex.Constants;
import vortex.ModLogger;
import vortex.utils.DiscordUtil;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class KickCmd extends Command {
    
    public KickCmd()
    {
        this.name = "kick";
        this.arguments = "@user [@user...]";
        this.help = "kicks all mentioned users";
        this.requiredPermissions = new Permission[]{Permission.KICK_MEMBERS};
        this.type = Type.GUILDONLY;
    }

    @Override
    protected void execute(String args, MessageReceivedEvent event) {
        if(event.getMessage().getMentionedUsers().isEmpty())
        {
            event.getChannel().sendMessage(String.format(Constants.NEED_MENTION, "user")).queue();
            return;
        }
        if(event.getMessage().getMentionedUsers().size()>20)
        {
            event.getChannel().sendMessage(Constants.ERROR+"Up to 20 users can be kicked at once.").queue();
            return;
        }
        StringBuilder builder = new StringBuilder();
        event.getMessage().getMentionedUsers().stream().forEach((User u) -> {
            Member m = event.getGuild().getMember(u);
            if(m==null || PermissionUtil.canInteract(event.getMember(), m))
            {
                try 
                {
                    DiscordUtil.queueAndBlock(event.getGuild().getController().kick(m),
                            (v)->{ builder.append("\n").append(Constants.SUCCESS).append("Successfully kicked ").append(u.getAsMention());}, 
                            (t)->{
                                if(t instanceof PermissionException)
                                    builder.append("\n").append(Constants.ERROR).append("I do not have permission to kick ").append(FormatUtil.formatUser(u));
                                else
                                    builder.append("\n").append(Constants.ERROR).append("I cannot kick ").append(FormatUtil.formatUser(u));
                            });
                } catch(PermissionException e)
                {
                    builder.append("\n").append(Constants.ERROR).append("I do not have permission to kick ").append(FormatUtil.formatUser(u));
                }
            }
            else
            {
                builder.append("\n").append(Constants.ERROR).append("You do not have permission to kick ").append(FormatUtil.formatUser(u));
            }
        });
        event.getChannel().sendMessage(builder.toString().trim()).queue();
        ModLogger.logCommand(event.getMessage());
    }
}
