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
import com.jagrosh.jdautilities.command.CommandClient;
import com.jagrosh.jdautilities.command.CommandEvent;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.time.temporal.TemporalAmount;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import com.jagrosh.vortex.database.Database;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import java.awt.Color;
import java.util.Collections;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.utils.TimeFormat;
import net.dv8tion.jda.api.utils.TimeUtil;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FormatUtil {
    
    private final static String MULTIPLE_FOUND = "**Multiple %s found matching \"%s\":**";
    private final static String CMD_EMOJI = "\uD83D\uDCDC"; // 📜

    public static String formatRoleColor(Role role) {
        Color color = role == null || role.getColor() == null ? ToycatPallete.DEFAULT_ROLE_WHITE : role.getColor();
        return formatColor(color.getRGB());
    }

    public static String formatColor(int rgb) {
        return String.format("#%06X", rgb);
    }

    public static String formatRolePermissions(Role role) {
        if (role == null) {
            return "";
        }

        if (role.getPermissions().isEmpty()) {
            return "None";
        }

        if (role.getPermissions().contains(Permission.ADMINISTRATOR)) {
            return "Administrator";
        }

        String seperator = ", ";
        StringBuilder str = new StringBuilder();
        role.getPermissions()
            .stream()
            .filter(p -> p != Permission.UNKNOWN)
            .forEach(p -> str.append(p).append(seperator));

        str.deleteCharAt(str.length() - seperator.length());
        return str.toString();
    }
    public static String filterEveryone(String input)
    {
        return input.replace("\u202E","") // RTL override
                .replace("@everyone","@\u0435veryone") // cyrillic e
                .replace("@here","@h\u0435re") // cyrillic e
                .replace("discord.gg/", "discord\u2024gg/") // one dot leader
                .replace("@&", "\u0DB8&"); // role failsafe
    }
    
    public static String formatMessage(Message m)
    {
        StringBuilder sb = new StringBuilder(m.getContentRaw());
        m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        return sb.length()>2048 ? sb.toString().substring(0, 2040) : sb.toString();
    }
    
    public static String formatMessage(CachedMessage m)
    {
        StringBuilder sb = new StringBuilder(m.getContentRaw());
        m.getAttachments().forEach(att -> sb.append("\n").append(att.getUrl()));
        return sb.length()>2048 ? sb.toString().substring(0, 2040) : sb.toString();
    }
    
    public static String formatFullUserId(long userId)
    {
        return "<@"+userId+"> (ID:"+userId+")";
    }
    
    public static String formatCachedMessageFullUser(CachedMessage msg)
    {
        return filterEveryone("**"+msg.getUsername()+"**#"+msg.getDiscriminator()+" (ID:"+msg.getAuthorId()+")");
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
    
    public static String join(String delimiter, char... items)
    {
        if(items==null || items.length==0)
            return "";
        StringBuilder sb = new StringBuilder().append(items[0]);
        for(int i=1; i<items.length; i++)
            sb.append(delimiter).append(items[i]);
        return sb.toString();
    }

    public static <T> String join(String delimiter, Function<T,String> function, T... items)
    {
        if(items==null || items.length==0)
            return "";
        StringBuilder sb = new StringBuilder(function.apply(items[0]));
        for(int i=1; i<items.length; i++)
            sb.append(delimiter).append(function.apply(items[i]));
        return sb.toString();
    }
    
    public static String listOfVoice(List<VoiceChannel> list, String query)
    {
        StringBuilder out = new StringBuilder(String.format(MULTIPLE_FOUND, "voice channels", query));
        for(int i=0; i<6 && i<list.size(); i++)
            out.append("\n - ").append(list.get(i).getName()).append(" (ID:").append(list.get(i).getId()).append(")");
        if(list.size()>6)
            out.append("\n**And ").append(list.size() - 6).append(" more...**");
        return out.toString();
    }
    
    public static String listOfRoles(List<Role> list, String query)
    {
        StringBuilder out = new StringBuilder(String.format(MULTIPLE_FOUND, "roles", query));
        for(int i=0; i<6 && i<list.size(); i++)
            out.append("\n - ").append(list.get(i).getName()).append(" (ID:").append(list.get(i).getId()).append(")");
        if(list.size()>6)
            out.append("\n**And ").append(list.size() - 6).append(" more...**");
        return out.toString();
    }

    public static String listOfRolesMention(List<Role> roles) {
        return formatList(" ", roles.stream().map(Role::getAsMention).toArray(String[]::new));
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
    
    public static String listOfUser(List<User> list, String query)
    {
        StringBuilder out = new StringBuilder(String.format(MULTIPLE_FOUND, "users", query));
        for(int i=0; i<6 && i<list.size(); i++)
            out.append("\n - **").append(list.get(i).getName()).append("**#").append(list.get(i).getDiscriminator()).append(" (ID:").append(list.get(i).getId()).append(")");
        if(list.size()>6)
            out.append("\n**And ").append(list.size() - 6).append(" more...**");
        return out.toString();
    }
    
    public static String listOfMember(List<Member> list, String query)
    {
        StringBuilder out = new StringBuilder(String.format(MULTIPLE_FOUND, "members", query));
        for(int i=0; i<6 && i<list.size(); i++)
            out.append("\n - **").append(list.get(i).getUser().getName()).append("**#").append(list.get(i).getUser().getDiscriminator()).append(" (ID:").append(list.get(i).getUser().getId()).append(")");
        if(list.size()>6)
            out.append("\n**And ").append(list.size() - 6).append(" more...**");
        return out.toString();
    }

    public static String formatList(Iterable<String> list, String seperator) {
        StringBuilder builder = new StringBuilder();
        for (String str : list) {
            builder.append(str).append(seperator);
        }

        if (builder.length() == 0) {
            return "";
        }

        builder.delete(builder.length() - seperator.length(), builder.length());
        return builder.toString();
    }

    public static String formatList(String seperator, String... items) {
        return formatList(Arrays.asList(items), seperator);
    }

    @Deprecated
    public static String secondsToTime(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0)
        {
            builder.append("**").append(years).append("** years, ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*7));
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

    @Deprecated
    public static String secondsToTimeCompact(long timeseconds)
    {
        StringBuilder builder = new StringBuilder();
        int years = (int)(timeseconds / (60*60*24*365));
        if(years>0)
        {
            builder.append("**").append(years).append("**y ");
            timeseconds = timeseconds % (60*60*24*365);
        }
        int weeks = (int)(timeseconds / (60*60*24*7));
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
    
    public static MessageCreateData formatHelp(CommandEvent event, Vortex vortex)
    {
        EmbedBuilder builder = new EmbedBuilder()
            .setColor(event.getGuild()==null ? Color.LIGHT_GRAY : event.getSelfMember().getColor());
        
        List<Command> commandsInCategory;
        String content;
        if(event.getArgs().isEmpty())
        {
            commandsInCategory = Collections.EMPTY_LIST;
            content = event.getClient().getSuccess()+" **"+event.getSelfUser().getName()+"** Commands Categories:";
        }
        else
        {
            commandsInCategory = event.getClient().getCommands().stream().filter(cmd -> 
                    {
                        if(cmd.isHidden() || cmd.isOwnerCommand())
                            return false;
                        if(cmd.getCategory()==null)
                            return "general".startsWith(event.getArgs().toLowerCase());
                        return cmd.getCategory().getName().toLowerCase().startsWith(event.getArgs().toLowerCase());
                    }).collect(Collectors.toList());
            if(commandsInCategory.isEmpty())
                content = event.getClient().getWarning()+" No Category `"+event.getArgs()+"` found.";
            else
                content = event.getClient().getSuccess()+" **"+event.getSelfUser().getName()+"** "
                        +(commandsInCategory.get(0).getCategory()==null ? "General" : commandsInCategory.get(0).getCategory().getName())
                        +" Commands:";
        }
        
        if(commandsInCategory.isEmpty())
        {
            builder.addField(CMD_EMOJI+" General Commands", "[**"+event.getClient().getPrefix()+"help general**]("+Constants.Wiki.COMMANDS+"#-general-commands)\n\u200B", false);
            event.getClient().getCommands().stream().filter(cmd -> cmd.getCategory()!=null).map(cmd -> cmd.getCategory().getName()).distinct()
                    .forEach(cat -> builder.addField(CMD_EMOJI+" "+cat+" Commands", "[**"+event.getClient().getPrefix()+"help "+cat.toLowerCase()+"**]("
                            +Constants.Wiki.COMMANDS+"#-"+cat.toLowerCase()+"-commands)\n\u200B", false));
        }
        else
        {
            commandsInCategory.forEach(cmd -> builder.addField(event.getClient().getPrefix()+cmd.getName()+(cmd.getArguments()==null ? "" : " "+cmd.getArguments()), 
                    "[**"+cmd.getHelp()+"**]("+Constants.Wiki.COMMANDS+"#-"+(cmd.getCategory()==null?"general":cmd.getCategory().getName().toLowerCase())+"-commands)\n\u200B", false));
        }
        
        builder.addField("Additional Help", helpLinks(event.getJDA(), event.getClient()), false);
        return new MessageCreateBuilder().addContent(filterEveryone(content)).setEmbeds(builder.build()).build();
    }
    
    public static String helpLinks(JDA jda, CommandClient commandClient)
    {
        return "\uD83D\uDD17 ["+jda.getSelfUser().getName()+" Wiki]("+Constants.Wiki.WIKI_BASE+")\n" // 🔗
                + "<:discord:314003252830011395> [Support Server]("+commandClient.getServerInvite()+")\n"
                +  CMD_EMOJI + " [Full Command Reference]("+Constants.Wiki.COMMANDS+")\n"
                + "<:patreon:417455429145329665> [Donations]("+Constants.DONATION_LINK+")";
    }

    public static String formatModlogCase(Vortex vortex, Guild guild, Database.Modlog modlog)
    {
        String type = "", punisher = "", savior = "";

        switch (modlog.getType()) {
            case GRAVEL:
                type = "Gravel";
                punisher = "Graveler";
                savior = "Ungraveler";
                break;
            case MUTE:
                type = "Mute";
                punisher = "Muter";
                savior = "Unmuter";
                break;
            case WARN:
                type = "Warning";
                punisher = "Warner";
                break;
            case BAN:
                type = "Ban";
                punisher = "Banner";
                savior = "Unbanner";
                break;
            case SOFTBAN:
                type = "Softban";
                punisher = "Banner";
                savior = "Unbanner";
                break;
            case KICK:
                type = "Kick";
                punisher = "Kicker";
        }

        String value = "Type: "+type;
        if (modlog.getModId() > 0)
            value += "\n"+punisher+": <@"+modlog.getModId()+">";
        else
            value += "\n"+punisher+": _Automod_";
        if (modlog.getSaviorId() > 0)
            value += "\n"+savior+": <@"+modlog.getSaviorId()+">";
        else if (modlog.getSaviorId() == 0)
            value += "\n"+savior+": _Automod_";
        if (modlog.getReason() != null && !modlog.getReason().trim().isEmpty())
            value += "\nReason: "+modlog.getReason().trim();
        if (modlog.getStart() != null) {
            String label;

            switch (modlog.getType()) {
                case WARN:
                case KICK:
                case SOFTBAN:
                    label = "Time";
                    break;
                default:
                    label = "Started ";
            }

            value += "\n" + FormatUtil.formatModlogTime(label, modlog.getStart());
        }
        if (modlog.getFinnish() != null)
        {
            if (modlog.getFinnish().getEpochSecond() == Instant.MAX.getEpochSecond())
            {
                value += "\nFinishes: Never";
            }
            else
            {
                String label = "Finishe" + (modlog.getFinnish().compareTo(Instant.now()) <= 0 ? "d" : "s");
                value += "\n" + FormatUtil.formatModlogTime(label, modlog.getFinnish());
            }
        }

        return value;
    }

    @Deprecated
    public static String formatModlogTime(Vortex vortex, Guild guild, Instant instant, String label) {
        ZonedDateTime zdt = instant.atZone(vortex.getDatabase().settings.getSettings(guild).getTimezone());
        return String.format("\n%s: %d/%d/%s at %d:%s %s",
                label,
                zdt.getDayOfMonth(),
                zdt.getMonthValue(),
                Integer.valueOf(zdt.getYear()).toString().substring(2),
                zdt.getHour() % 12 == 0 ? 12 : zdt.getHour() % 12,
                zdt.getMinute() < 10 ? "0" + zdt.getMinute() : zdt.getMinute(),
                zdt.getHour() < 12 ? "AM" : "PM"
        );
    }

    public static String formatModlogTime(String label, TemporalAccessor temporalAccessor) {
        return label + ": " + TimeFormat.DATE_TIME_SHORT.format(temporalAccessor);
    }

    public static String formatCreationTime(TemporalAccessor temporal) {
        Instant creationTime = Instant.from(temporal);
        boolean recent = Instant.now().minusSeconds(creationTime.getEpochSecond()).getEpochSecond() < 60 * 60 * 24 * 31; // ~1 month
        return (recent ? TimeFormat.DATE_TIME_SHORT : TimeFormat.DATE_SHORT).format(temporal);
    }

    public static String toMentionableRoles(String[] roles) {
        StringBuilder str = new StringBuilder();

        if (roles == null || roles.length == 0)
            return "nothing";

        for (int i = 0; i < roles.length; i++)
        {
            String afterRoleChars;
            if (i != roles.length - 1)
            {
                if (i == roles.length - 2)
                {
                    if (roles.length > 2)
                        afterRoleChars = ", and ";
                    else
                        afterRoleChars = " and ";
                }
                else
                    afterRoleChars = ", ";
            }
            else
                afterRoleChars = "";

            str.append("<@&").append(roles[i]).append(">").append(afterRoleChars);
        }

        return str.toString();
    }

    public static class IconURLFieldBuilder {
        private final List<String> formattedIconUrls = new LinkedList<>();

        public IconURLFieldBuilder add(String name, String url) {
            if (url != null) {
                formattedIconUrls.add(String.format("[%s](%s)", name, url));
            }

            return this;
        }

        public boolean isEmpty() {
            return formattedIconUrls.isEmpty();
        }

        public int size() {
            return formattedIconUrls.size();
        }

        @Override
        public String toString() {
            return formatList(formattedIconUrls, ", ");
        }
    }

    public static String formatPing(long ping) {
        String formattedPing = "";

        if (ping >= 60000) {
            formattedPing += (ping / 60000) + "m " ;
        }

        if (ping >= 1000) {
            formattedPing += ((ping %= 60000) / 1000) + "s ";
        }

        return formattedPing + (ping % 1000) + "ms";
    }
}
