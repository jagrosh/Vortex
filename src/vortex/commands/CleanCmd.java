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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import java.util.LinkedList;
import java.util.concurrent.ScheduledExecutorService;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import vortex.ModLogger;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class CleanCmd extends Command {
    private final Pattern LINK_PATTERN = Pattern.compile("https?:\\/\\/.+");
    private final Pattern QUOTES_PATTERN = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
    private final Pattern CODE_PATTERN = Pattern.compile("`(.*?)`", Pattern.DOTALL);
    private final Pattern MENTION_PATTERN = Pattern.compile("<@!?(\\d{17,22})>");
    private final Pattern ID_PATTERN = Pattern.compile("(?:^|\\s)(\\d{17,22})(?:$|\\s)");
    private final Pattern NUM_PATTERN = Pattern.compile("(?:^|\\s)(\\d{1,4})(?:$|\\s)");
    private final String week2limit = " Note: Messages older than 2 weeks cannot be cleaned.";
    private final String noparams = "**No valid cleaning paramaters included!**\n"
                +"This command is to remove many messages quickly. Pinned messages are ignored. "
                + "Messages can be filtered with various parameters. Mutliple arguments can be used, and "
                + "the order of parameters does not matter. The following parameters are supported:\n"
                + " `<numPosts>` - number of posts to delete; between 2 and 1000\n"
                + " `bots` - cleans messages by bots\n"
                + " `embeds` - cleans messages with embeds\n"
                + " `links` - cleans messages containing links\n"
                + " `images` - cleans messages with uploaded or embeded images or videos\n"
                + " `@user` - cleans messages only from the provided user\n"
                + " `userId` - cleans messages only from the provided user (via id)\n"
                + " `\"quotes\"` - cleans messages containing the text in quotes\n"
                + " `` `regex` `` - cleans messages that match the regex";
    
    private final ModLogger modlog;
    private final ScheduledExecutorService threadpool;
    
    public CleanCmd(ModLogger modlog, ScheduledExecutorService threadpool)
    {
        this.modlog = modlog;
        this.threadpool = threadpool;
        this.category = new Category("Moderation");
        this.name = "clean";
        this.arguments = "@user(s) | \"text\" | bots | embeds | links | images | <number>";
        this.help = "cleans messages in the past 100, matching the given criteria";
        this.userPermissions = new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY};
        this.botPermissions = new Permission[]{Permission.MESSAGE_MANAGE, Permission.MESSAGE_HISTORY};
        this.guildOnly = true;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        if(event.getArgs().isEmpty())
        {
            event.replyWarning(noparams);
            return;
        }
        int num = -1;
        List<String> quotes = new LinkedList<>();
        String pattern = null;
        List<String> ids = new LinkedList<>();
        String parameters = event.getArgs();
        
        Matcher m = QUOTES_PATTERN.matcher(parameters);
        while(m.find())
            quotes.add(m.group(1).trim().toLowerCase());
        parameters = parameters.replaceAll(QUOTES_PATTERN.pattern(), " ");
        
        m = CODE_PATTERN.matcher(parameters);
        if(m.find())
            pattern= m.group(1);
        parameters = parameters.replaceAll(CODE_PATTERN.pattern(), " ");
        
        m = MENTION_PATTERN.matcher(parameters);
        while(m.find())
            ids.add(m.group(1));
        parameters = parameters.replaceAll(MENTION_PATTERN.pattern(), " ");
        
        m = ID_PATTERN.matcher(parameters);
        while(m.find())
            ids.add(m.group(1));
        parameters = parameters.replaceAll(ID_PATTERN.pattern(), " ");
        
        m = NUM_PATTERN.matcher(parameters);
        if(m.find())
            num = Integer.parseInt(m.group(1));
        parameters = parameters.replaceAll(NUM_PATTERN.pattern(), " ").toLowerCase();
        
        boolean bots = parameters.contains("bot");
        boolean embeds = parameters.contains("embed");
        boolean links = parameters.contains("link");
        boolean images = parameters.contains("image");
        
        boolean all = quotes.isEmpty() && pattern==null && ids.isEmpty() && !bots && !embeds && !links && !images;
        
        if(num==-1)
        {
            if(all)
            {
                event.replyWarning(noparams);
                return;
            }
            else
                num=100;
        }
        if(num>1000 || num<2)
        {
            event.replyError("Number of messages must be between 2 and 1000");
            return;
        }
        
        int val2 = num+1;
        String p = pattern;
        threadpool.submit(() -> {
            int val = val2;
            List<Message> msgs = new LinkedList<>();
            MessageHistory mh = event.getChannel().getHistory();
            OffsetDateTime earliest = event.getMessage().getCreationTime().minusWeeks(2).plusMinutes(1);
            while(val>100)
            {
                msgs.addAll(mh.retrievePast(100).complete());
                val-=100;
                if(msgs.get(msgs.size()-1).getCreationTime().isBefore(earliest))
                {
                    val=0;
                    break;
                }
            }
            if(val>0)
                msgs.addAll(mh.retrievePast(val).complete());

            msgs.remove(event.getMessage());
            boolean week2 = false;
            List<Message> del = new LinkedList<>();
            for(Message msg : msgs)
            {
                if(msg.getCreationTime().isBefore(earliest))
                {
                    week2 = true;
                    break;
                }
                if(all || ids.contains(msg.getAuthor().getId()) || (bots && msg.getAuthor().isBot()) || (embeds && !msg.getEmbeds().isEmpty())
                    || (links && LINK_PATTERN.matcher(msg.getRawContent()).find()) || (images && hasImage(msg)))
                {
                    del.add(msg);
                    continue;
                }
                String lowerContent = msg.getRawContent().toLowerCase();
                if(quotes.stream().anyMatch(quote -> lowerContent.contains(quote)))
                {
                    del.add(msg);
                    continue;
                }
                try{
                    if(p!=null && msg.getRawContent().matches(p))
                        del.add(msg);
                }catch(Exception e){}
            }

            if(del.isEmpty())
            {
                event.replyWarning("There were no messages to clean!"+(week2?week2limit:""));
                return;
            }
            try{
                int index = 0;
                while(index < del.size())
                {
                    if(index+100>del.size())
                        if(index+1==del.size())
                            del.get(del.size()-1).delete().complete();
                        else
                            event.getTextChannel().deleteMessages(del.subList(index, del.size())).complete();
                    else
                        event.getTextChannel().deleteMessages(del.subList(index, index+100)).complete();
                    index+=100;
                }
            }catch(Exception e)
            {
                event.replyError("Failed to delete "+del.size()+" messages.");
                return;
            }
            event.replySuccess("Cleaned **"+del.size()+"** messages."+(week2?week2limit:""));
            modlog.logCommand(event.getMessage());
        });
    }
    
    private static boolean hasImage(Message message)
    {
        if(message.getAttachments().stream().anyMatch(a -> a.isImage()))
            return true;
        if(message.getEmbeds().stream().anyMatch(e -> e.getImage()!=null || e.getVideoInfo()!=null))
            return true;
        return false;
    }
}
