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

import java.util.List;
import java.util.stream.Collectors;
import me.jagrosh.jdautilities.commandclient.Command;
import me.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.Constants;
import vortex.ModLogger;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class MuteCmd extends Command {
    
    public MuteCmd()
    {
        this.name = "mute";
        this.arguments = "@user [@user...]";
        this.help = "applies a muted role all mentioned users";
        this.userPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.botPermissions = new Permission[]{Permission.MANAGE_ROLES};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        if(event.getMessage().getMentionedUsers().isEmpty())
        {
            event.reply(String.format(Constants.NEED_MENTION, "user"));
            return;
        }
        if(event.getMessage().getMentionedUsers().size()>20)
        {
            event.reply(event.getClient().getError()+" Up to 20 users can be muted at once.");
            return;
        }
        Role muteRole = event.getGuild().getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("muted")).findFirst().orElse(null);
        if(muteRole == null)
        {
            event.reply(event.getClient().getError()+" No role called 'Muted' exists!");
            return;
        }
        if(!PermissionUtil.canInteract(event.getMember(), muteRole))
        {
            event.reply(event.getClient().getError()+" You do not have permissions to assign the 'Muted' role!");
            return;
        }
        if(!PermissionUtil.canInteract(event.getSelfMember(), muteRole))
        {
            event.reply(event.getClient().getError()+" I do not have permissions to assign the 'Muted' role!");
            return;
        }
        StringBuilder builder = new StringBuilder();
        List<Member> members = event.getMessage().getMentionedUsers().stream()
                .map(u -> event.getGuild().getMember(u))
                .filter((Member m) -> {
                    if(m==null)
                    {
                        builder.append("\n").append(event.getClient().getWarning()).append(" User ").append(FormatUtil.formatUser(m.getUser())).append(" is not in the server!");
                        return false;
                    }
                    if(m.equals(event.getSelfMember()))
                    {
                        builder.append("\n").append(event.getClient().getWarning()).append(" I will not mute myself.");
                        return false;
                    }
                    if(m.getRoles().contains(muteRole))
                    {
                        builder.append("\n").append(event.getClient().getWarning()).append(" Member ").append(FormatUtil.formatUser(m.getUser())).append(" is already muted!");
                        return false;
                    }
                    return true;
                }).collect(Collectors.toList());
        if(members.isEmpty())
        {
            event.reply(builder.toString());
        }
        else for(int i=0; i<members.size(); i++)
        {
            Member m = members.get(i);
            boolean last = i+1==members.size();
            event.getGuild().getController().addRolesToMember(m, muteRole).queue(v -> {
                    builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully muted ").append(FormatUtil.formatUser(m.getUser()));
                    if(last)
                        event.reply(builder.toString());
            }, t -> {
                    builder.append("\n").append(event.getClient().getError()).append(" I failed to mute ").append(FormatUtil.formatUser(m.getUser()));
                    if(last)
                        event.reply(builder.toString());
            });
        }
        ModLogger.logCommand(event.getMessage());
    }
}
