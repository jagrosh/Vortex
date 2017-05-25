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
import java.util.LinkedList;
import java.util.List;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import vortex.Constants;
import vortex.ModLogger;
import vortex.utils.FormatUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class HackbanCmd extends Command {
    
    private final ModLogger modlog;
    public HackbanCmd(ModLogger modlog)
    {
        this.modlog = modlog;
        this.name = "hackban";
        this.category = new Category("Moderation");
        this.arguments = "userId [userId...]";
        this.help = "bans all listed user IDs";
        this.userPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.botPermissions = new Permission[]{Permission.BAN_MEMBERS};
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event) {
        String[] words = event.getArgs().split("\\s+");
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
        {
            event.reply(String.format(Constants.NEED_X, "User ID"));
            return;
        }
        if(event.getMessage().getMentionedUsers().size()>20)
        {
            event.reply(event.getClient().getError()+" Up to 20 users can be banned at once.");
            return;
        }
        StringBuilder builder = new StringBuilder();
        LinkedList<String> banIds = new LinkedList<>();
        ids.stream().forEach((id) -> {
            Member m = event.getGuild().getMemberById(id);
            if(m==null)
            {
                banIds.add(id);
            }
            else if(!event.getMember().canInteract(m))
            {
                builder.append("\n").append(event.getClient().getError()).append(" You do not have permission to ban ").append(FormatUtil.formatUser(m.getUser()));
            }
            else if (!event.getSelfMember().canInteract(m))
            {
                builder.append("\n").append(event.getClient().getError()).append(" I do not have permission to ban ").append(FormatUtil.formatUser(m.getUser()));
            }
            else
            {
                banIds.add(id);
            }
        });
        String reason = event.getAuthor().getName()+"#"+event.getAuthor().getDiscriminator()+" [hackban]: "+event.getMessage().getRawContent().replaceAll("\\d{17,20}", "");
                if(reason.length()>512)
                    reason = reason.substring(0,512);
        if(banIds.isEmpty())
            event.reply(builder.toString());
        else
        {
            for(int i=0; i<banIds.size(); i++)
            {
                String id = banIds.get(i);
                User u = event.getJDA().getUserById(id);
                boolean last = i+1==banIds.size();
                event.getGuild().getController().ban(id, 1).reason(reason).queue((v) -> {
                        builder.append("\n").append(event.getClient().getSuccess()).append(" Successfully banned ").append(u==null ? "User with ID `"+id+"`" : u.getAsMention());
                        if(last)
                            event.reply(builder.toString());
                    }, (t) -> {
                        builder.append("\n").append(event.getClient().getError()).append(" I failed to ban ").append(u==null ? "User with ID `"+id+"`" : FormatUtil.formatUser(u));
                        if(last)
                            event.reply(builder.toString());
                    });
            }
        }
        modlog.logCommand(event.getMessage());
    }
}
