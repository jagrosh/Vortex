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

import java.util.Arrays;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.impl.JDAImpl;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import org.json.JSONObject;
import vortex.ModLogger.Action;
import vortex.utils.DiscordUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class Bot extends ListenerAdapter {

    private final Command[] commands;
    
    public Bot(Command[] commands)
    {
        this.commands = commands;
    }

    @Override
    public void onReady(ReadyEvent event) {
        JSONObject content = new JSONObject()
                .put("game", new JSONObject().put("name","Type "+Constants.PREFIX+"help"))
                .put("status", "online")
                .put("since", System.currentTimeMillis())
                .put("afk", false);
        ((JDAImpl)event.getJDA()).getClient().send(new JSONObject().put("op", 3).put("d", content).toString());
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if(event.getAuthor().isBot())
            return;
        if(event.getMessage().getRawContent().toLowerCase().startsWith(Constants.PREFIX))
        {
            String[] parts = Arrays.copyOf(event.getMessage().getRawContent().substring(Constants.PREFIX.length()).trim().split("\\s+",2), 2);
            if(parts[0].equalsIgnoreCase("help"))
            {
                if(event.getChannelType()!=ChannelType.TEXT || event.getJDA().getSelfInfo().isBot())
                {
                    StringBuilder builder = new StringBuilder("**"+event.getJDA().getSelfInfo().getName()+"** commands:\n");
                    for(Command command : commands)
                        if(!command.ownerCommand || event.getAuthor().getId().equals(Constants.OWNER_ID))
                            builder.append("\n`").append(Constants.PREFIX).append(command.name)
                                    .append(command.arguments==null ? "`" : " "+command.arguments+"`")
                                    .append(" - ").append(command.help);
                    builder.append("\n\nFor additional help, contact **jagrosh**#4824 or join "+Constants.SERVER_INVITE);
                    if(!event.getAuthor().hasPrivateChannel())
                        DiscordUtil.queueAndBlock(event.getAuthor().openPrivateChannel());
                    event.getAuthor().getPrivateChannel().sendMessage(builder.toString()).queue((v)->{}, (t)->{
                        event.getChannel().sendMessage(Constants.WARNING+"I cannot send you help because you are blocking Direct Messages.").queue();
                    });
                }
            }
            else
                if(event.getChannelType()!=ChannelType.TEXT || PermissionUtil.checkPermission(event.getTextChannel(), event.getGuild().getMember(event.getJDA().getSelfInfo()), Permission.MESSAGE_WRITE))
                    for(Command command : commands)
                        if(parts[0].equalsIgnoreCase(command.name))
                            command.run(parts[1], event);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        //simple automod
        
        //user account takes no actions
        if(event.getJDA().getAccountType() == AccountType.CLIENT)
            return;
        
        Member me = event.getGuild().getSelfMember();
        
        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if(!PermissionUtil.canInteract(me, event.getMember()) ||
                PermissionUtil.checkPermission(event.getChannel(), event.getMember(), Permission.MESSAGE_MANAGE) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.KICK_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.BAN_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.MANAGE_SERVER))
            return;
        
        //check roles for automod actions
        me.getRoles().stream().forEach(r -> {
            
            //anti mention spam
            if(r.getName().toLowerCase().startsWith("antimention"))
            {
                try{
                    int maxmentions = Integer.parseInt(r.getName().split(":",2)[1].trim());
                    long mentions = event.getMessage().getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(event.getAuthor())).count();
                    if(maxmentions > 6 && mentions >= maxmentions)
                    {
                        event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                            ModLogger.logAction(Action.BAN, event.getGuild(), event.getAuthor(), "Mentioning "+event.getMessage().getMentionedUsers().size()+" users");
                        }, t -> {});
                    }
                } catch(Exception e){}
            }
        });
    }
}
