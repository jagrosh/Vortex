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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import vortex.Command;
import vortex.Constants;
import vortex.EventWaiter;
import vortex.ModLogger;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class CleanCmd extends Command {
    private final EventWaiter waiter;
    private final String CANCEL = "\u274C";
    private final Pattern LINK_PATTERN = Pattern.compile("https?:\\/\\/.+");
    private final String QUOTES_REGEX = "\"(.*?)\"";
    private final Pattern QUOTES_PATTERN = Pattern.compile(QUOTES_REGEX);
    
    public CleanCmd(EventWaiter waiter)
    {
        this.waiter = waiter;
        this.name = "clean";
        this.arguments = "@user(s) | \"text\" | bots | embeds | links | all";
        this.help = "cleans messages in the past 100, matching the given criteria";
        this.requiredPermissions = new Permission[]{Permission.MESSAGE_MANAGE};
        this.type = Type.GUILDONLY;
    }
    
    @Override
    protected Void execute(String args, MessageReceivedEvent event) {
        if(args==null || args.isEmpty())
        {
            event.getChannel().sendMessage("No parameters provided! Please select a cleaning option below!"
                    + "\n"+CleanType.ROBOT.getUnicode()+" - **Bot messages**"
                    + "\n"+CleanType.EMBEDS.getUnicode()+" - **Embeds**"
                    + "\n"+CleanType.LINKS.getUnicode()+" - **Links**"
                    + "\n"+CANCEL+" - **Cancel**").queue((Message m) -> {
                        for(CleanType type : CleanType.values())
                            m.addReaction(type.getUnicode()).queue();
                        m.addReaction(CANCEL).queue();
                        waiter.waitForEvent(MessageReactionAddEvent.class, e -> {
                            return e.getUser().equals(event.getAuthor())
                                    && e.getMessageId().equals(m.getId())
                                    && (e.getReaction().getEmote().getName().equals(CANCEL)
                                        || CleanType.of(e.getReaction().getEmote().getName())!=null);
                        }, ev -> {
                            m.deleteMessage().queue();
                            CleanType type = CleanType.of(ev.getReaction().getEmote().getName());
                            if(type!=null)
                                executeClean(type.getText(), event, " "+type.getText());
                        });
                    });
            return null;
        }
        else
            return executeClean(args, event, null);
    }
    
    private enum CleanType {
        ROBOT("\uD83E\uDD16", "bots"),
        EMBEDS("\uD83D\uDDBC", "embeds"),
        LINKS("\uD83D\uDD17", "links");
        
        private final String unicode, text;
        
        private CleanType(String unicode, String text)
        {
            this.unicode = unicode;
            this.text = text;
        }
        
        public String getUnicode()
        {
            return unicode;
        }
        
        public String getText()
        {
            return text;
        }
        
        public static CleanType of(String unicode)
        {
            for(CleanType type: values())
                if(type.getUnicode().equals(unicode))
                    return type;
            return null;
        }
    }
    
    protected Void executeClean(String args, MessageReceivedEvent event, String extra) {
        List<String> texts = new ArrayList<>();
        Matcher ma = QUOTES_PATTERN.matcher(args);
        while(ma.find())
            texts.add(ma.group(1).trim().toLowerCase());
        String newargs = args.replaceAll(QUOTES_REGEX, " ").toLowerCase();
        try
        {
            boolean all = newargs.contains("all");
            boolean bots = newargs.contains("bots");
            boolean embeds = newargs.contains("embeds");
            boolean links = newargs.contains("links");
            
            if(!all && !bots && !embeds && !links && texts.isEmpty())
                return reply(Constants.ERROR+"No valid arguments provided!\nValid arguments: `"+this.arguments+"`", event);
            
            event.getChannel().getHistory().retrievePast(100).queue(messages -> {
                List<Message> toClean;
                if(all)
                    toClean = messages;
                else
                {
                    toClean = messages.stream().filter(m -> {
                        String lowerCaseContent = m.getRawContent().toLowerCase();
                        if(event.getMessage().getMentionedUsers().contains(m.getAuthor()))
                            return true;
                        if(bots && m.getAuthor().isBot())
                            return true;
                        if(embeds && !m.getEmbeds().isEmpty())
                            return true;
                        if(links && LINK_PATTERN.matcher(m.getRawContent()).find())
                            return true;
                        return texts.stream().anyMatch(str -> lowerCaseContent.contains(str));
                    }).collect(Collectors.toList());
                }
                toClean.remove(event.getMessage());
                if(toClean.isEmpty())
                {
                    reply(Constants.WARNING+"No messages found matching the given criteria!", event);
                    return;
                }
                try
                {
                    if(toClean.size()==1)
                        toClean.get(0).deleteMessage().queue(v -> reply("Cleaned "+toClean.size()+" messages.",event));
                    else
                        ((TextChannel)event.getChannel()).deleteMessages(toClean).queue(v -> reply("Cleaned "+toClean.size()+" messages.",event));
                    ModLogger.logCommand(event.getMessage(), extra);
                }
                catch(PermissionException ex)
                {
                    reply(String.format(Constants.BOT_NEEDS_PERMISSION, Permission.MESSAGE_MANAGE, "channel"), event);
                }
            });
        } 
        catch(PermissionException e)
        {
            return reply(String.format(Constants.BOT_NEEDS_PERMISSION, Permission.MESSAGE_HISTORY, "channel"), event);
        }
        return null;
    }
}
