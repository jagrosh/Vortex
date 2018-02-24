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
package com.jagrosh.vortex.utils;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import java.util.List;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.entities.VoiceChannel;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FormatUtil {
    
    private final static String MULTIPLE_FOUND = Constants.WARNING+" **Multiple %s found matching \"%s\":**";
    
    public static String filterEveryone(String input)
    {
        return input.replace("@everyone","@\u0435veryone").replace("@here","@h\u0435re");
    }
    
    public static String formatMessage(Message m)
    {
        StringBuilder sb = new StringBuilder(m.getContentRaw());
        m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        return sb.toString();
    }
    
    public static String formatUser(User user)
    {
        return filterEveryone("**"+user.getName()+"**#"+user.getDiscriminator());
    }
    
    public static String formatFullUser(User user)
    {
        return filterEveryone("**"+user.getName()+"**#"+user.getDiscriminator()+" (ID:"+user.getId()+")");
    }
    
    public static String capitalize(String input)
    {
        if(input==null || input.isEmpty())
            return "";
        if(input.length()==1)
            return input.toUpperCase();
        return Character.toUpperCase(input.charAt(0))+input.substring(1).toLowerCase();
    }
    
    public static String listOfVoice(List<VoiceChannel> list, String query)
    {
        String out = String.format(MULTIPLE_FOUND, "voice channels", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (ID:"+list.get(i).getId()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    public static String listOfRoles(List<Role> list, String query)
    {
        String out = String.format(MULTIPLE_FOUND, "roles", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" (ID:"+list.get(i).getId()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    public static String listOfText(List<TextChannel> list, String query)
    {
        String out = String.format(MULTIPLE_FOUND, "text channels", query);
        for(int i=0; i<6 && i<list.size(); i++)
            out+="\n - "+list.get(i).getName()+" ("+list.get(i).getAsMention()+")";
        if(list.size()>6)
            out+="\n**And "+(list.size()-6)+" more...**";
        return out;
    }
    
    public static String secondsToTime(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0)
        {
            builder.append("**").append(years).append("** years, ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*365));
        if(weeks>0)
        {
            builder.append("**").append(weeks).append("** weeks, ");
            timeseconds = timeseconds % (60*60*24*7);
        }
        int days = (int)(timeseconds / (60*60*24));
        if(days>0)
        {
            builder.append("**").append(days).append("** days, ");
            timeseconds = timeseconds % (60*60*24);
        }
        int hours = (int)(timeseconds / (60*60));
        if(hours>0)
        {
            builder.append("**").append(hours).append("** hours, ");
            timeseconds = timeseconds % (60*60);
        }
        int minutes = (int)(timeseconds / (60));
        if(minutes>0)
        {
            builder.append("**").append(minutes).append("** minutes, ");
            timeseconds = timeseconds % (60);
        }
        if(timeseconds>0)
            builder.append("**").append(timeseconds).append("** seconds");
        String str = builder.toString();
        if(str.endsWith(", "))
            str = str.substring(0,str.length()-2);
        if(str.isEmpty())
            str="**No time**";
        return str;
    }
    
    public static String secondsToTimeCompact(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0)
        {
            builder.append("**").append(years).append("**y ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*365));
        if(weeks>0)
        {
            builder.append("**").append(weeks).append("**w ");
            timeseconds = timeseconds % (60*60*24*7);
        }
        int days = (int)(timeseconds / (60*60*24));
        if(days>0)
        {
            builder.append("**").append(days).append("**d ");
            timeseconds = timeseconds % (60*60*24);
        }
        int hours = (int)(timeseconds / (60*60));
        if(hours>0)
        {
            builder.append("**").append(hours).append("**h ");
            timeseconds = timeseconds % (60*60);
        }
        int minutes = (int)(timeseconds / (60));
        if(minutes>0)
        {
            builder.append("**").append(minutes).append("**m ");
            timeseconds = timeseconds % (60);
        }
        if(timeseconds>0)
            builder.append("**").append(timeseconds).append("**s");
        String str = builder.toString();
        if(str.endsWith(", "))
            str = str.substring(0,str.length()-2);
        if(str.isEmpty())
            str="**No time**";
        return str;
    }
    
    public static String formatHelp(CommandEvent event, Vortex vortex)
    {
        StringBuilder builder = new StringBuilder("**"+event.getSelfUser().getName()+"** commands:\n");
        Command.Category category = null;
        for(Command command : event.getClient().getCommands())
        {
            if(!command.isHidden() && (!command.isOwnerCommand() || event.isOwner()))
            {
                if(category==null)
                {
                    if(command.getCategory()!=null)
                    {
                        category = command.getCategory();
                        builder.append("\n\n  __").append(category==null ? "No Category" : category.getName()).append("__:\n");
                    }
                }
                else
                {
                    if(command.getCategory()==null || !command.getCategory().getName().equals(category.getName()))
                    {
                        category = command.getCategory();
                        builder.append("\n\n  __").append(category==null ? "No Category" : category.getName()).append("__:\n");
                    }
                }
                builder.append("\n`").append(event.getClient().getTextualPrefix()).append(event.getClient().getPrefix()==null?" ":"").append(command.getName())
                       .append(command.getArguments()==null ? "`" : " "+command.getArguments()+"`")
                       .append(" - ").append(command.getHelp());
            }
        }
        User owner = vortex.getShardManager().getUserById(event.getClient().getOwnerIdLong());
        if(owner!=null)
        {
            builder.append("\n\nFor additional help, contact **").append(owner.getName()).append("**#").append(owner.getDiscriminator());
            if(event.getClient().getServerInvite()!=null)
                builder.append(" or join ").append(event.getClient().getServerInvite());
        }
        return builder.toString();
    }
}
