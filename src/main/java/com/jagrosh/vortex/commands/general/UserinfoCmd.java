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
package com.jagrosh.vortex.commands.general;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.command.SlashCommand;
import com.jagrosh.jdautilities.command.SlashCommandEvent;
import com.jagrosh.jdautilities.commons.utils.FinderUtil;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandTools;
import com.jagrosh.vortex.utils.FormatUtil;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.jagrosh.vortex.utils.ToycatPallete;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.TimeFormat;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class UserinfoCmd extends SlashCommand
{
    private final static String BOT_EMOJI = "<:botTag:230105988211015680>";
    private final static String USER_EMOJI = "\uD83D\uDC64"; // 👤
    private final static String LINESTART = "\u25AB"; // ▫
    private final Vortex vortex;
    
    public UserinfoCmd(Vortex vortex)
    {
        this.name = "whois";
        this.aliases = new String[]{"user","uinfo","memberinfo","userinfo","whothis","newphonewhothis"};
        this.help = "shows info on a member";
        this.arguments = "[user]";
        this.guildOnly = true;
        this.vortex = vortex;
        this.options = Collections.singletonList(new OptionData(OptionType.USER, "user", "The user", true));
    }

    @Override
    protected void execute(SlashCommandEvent event) {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE)) {
            event.reply(CommandTools.COMMAND_NO_PERMS).setEphemeral(true).queue();
            return;
        }

        User u = event.getOption("user").getAsUser();
        Member m = null;
        if (event.getGuild() != null) {
            try {
                m = event.getGuild().retrieveMember(u).complete();
            } catch (ErrorResponseException ignore) {}
        }

        event.replyEmbeds(generateInfoEmbed(u, m)).queue();
    }

    @Override
    protected void execute(CommandEvent event) 
    {
        if (!CommandTools.hasGeneralCommandPerms(vortex, event, Permission.MESSAGE_MANAGE))
            return;

        Member member;
        if(event.getArgs().isEmpty())
        {
            member = event.getMember();
        }
        else
        {
            List<Member> found = FinderUtil.findMembers(event.getArgs(), event.getGuild());
            if(found.isEmpty())
            {
                event.replyError("I couldn't find the member you were looking for!");
                return;
            }
            else if(found.size()>1)
            {
                event.replyWarning(FormatUtil.listOfMember(found, event.getArgs()));
                return;
            }
            else
            {
                member = found.get(0);
            }
        }

        User user = member.getUser();
        event.reply(generateInfoEmbed(user, member));
/*
        String title = (user.isBot() ? BOT_EMOJI : USER_EMOJI)+" Information about **"+user.getName()+"** #"+user.getDiscriminator()+":";
        StringBuilder str = new StringBuilder(LINESTART + "Discord ID: **" + user.getId() + "**" + (user.getAvatarId() != null && user.getAvatarId().startsWith("a_") ? " <:nitro:314068430611415041>" : ""));
        if(member.getNickname()!=null)
            str.append("\n" + LINESTART + "Nickname: **").append(member.getNickname()).append("**");
        String roles="";
        roles = member.getRoles().stream().map((rol) -> "`, `"+rol.getName()).reduce(roles, String::concat);
        if(roles.isEmpty())
            roles="None";
        else
            roles=roles.substring(3)+"`";
        str.append("\n" + LINESTART + "Roles: ").append(roles);
        str.append("\n" + LINESTART + "Status: ").append(statusToEmote(member.getOnlineStatus(), member.getActivities())).append("**").append(member.getOnlineStatus().name()).append("**");
        List<String> formattedActivities = new LinkedList<>();
        for (Activity activity : member.getActivities()) {
            if (activity != null) {
                formattedActivities.add(formatActivity(activity));
            }
        }
        str.append(FormatUtil.formatList(formattedActivities, ", "));
        str.append("\n" + LINESTART + "Account Creation: ").append(TimeFormat.DATE_TIME_SHORT.format(user.getTimeCreated()));
        List<Member> joins = new ArrayList<>(event.getGuild().getMembers());
        joins.sort(Comparator.comparing(Member::getTimeJoined));

        
        event.reply(new MessageCreateBuilder()
                .setContent(FormatUtil.filterEveryone(title))
                .addEmbeds(new EmbedBuilder()
                        .setDescription(str.toString())
                        .setThumbnail(user.getEffectiveAvatarUrl())
                        .setColor(member.getColor()).build())
                .build());*/
    }
    
    private static String statusToEmote(OnlineStatus status, List<Activity> activities)
    {
        for (Activity activity : activities) {
            if  (activity != null && activity.getType() == Activity.ActivityType.STREAMING && Activity.isValidStreamingUrl(activity.getUrl())) {
                return "<:streaming:313956277132853248>";
            }
        }

        switch(status) {
            case ONLINE: return "<:online:313956277808005120>";
            case IDLE: return "<:away:313956277220802560>";
            case DO_NOT_DISTURB: return "<:dnd:313956276893646850>";
            case INVISIBLE: return "<:invisible:313956277107556352>";
            case OFFLINE: return "<:offline:313956277237710868>";
            default: return "";
        }
    }
    
    private static String formatActivity(Activity activity)
    {
        String str;
        switch(activity.getType())
        {
            case STREAMING:
                return "Streaming [*"+activity.getName()+"*]("+activity.getUrl()+")";
            case LISTENING: 
                str="Listening to"; 
                break;
            case WATCHING: 
                str="Watching"; 
                break;
            case COMPETING:
                str="Competing in";
                break;
            default:
                str="Playing"; 
                break;
        }
        return activity.getType() == Activity.ActivityType.CUSTOM_STATUS ? activity.getName() : str+" *"+activity.getName()+"*";
    }

    private static MessageEmbed generateInfoEmbed(User u, Member m) {
        EmbedBuilder builder = new EmbedBuilder();
        User.Profile p = u.retrieveProfile().complete();

        String username = u.getName();
        try {
            int discrim = Integer.parseInt(u.getDiscriminator());
            if (discrim != 0) {
                username = u.getAsTag();
            }
        } catch (NumberFormatException ignore) {}


        StringBuilder badges = new StringBuilder();
        if (u.isBot()) { //TODO: Show a seperate badge for system accounts
            badges.append(u.getFlags().contains(User.UserFlag.VERIFIED_BOT) ? Constants.VERIFIED_BOT : Constants.BOT);
        }

        badges.append((m != null && m.isOwner()) ? Constants.SERVER_OWNER : "")
                     .append(u.getFlags().contains(User.UserFlag.STAFF) ? Constants.DISCORD_STAFF : "")
                     .append(u.getFlags().contains(User.UserFlag.PARTNER) ? Constants.PARTNERED_USER : "")
        .append(m != null && OffsetDateTime.now().minusWeeks(1).isBefore(m.getTimeJoined()) ? Constants.NEW_MEMBER : "");

        for (User.UserFlag flag : u.getFlags()) {
            badges.append(getEmojiFromMiscFlag(flag));
        }

        List<String> formattedActivities = new ArrayList<>();
        if (m != null) {
            for (Activity activity : m.getActivities()) {
                if (activity != null) {
                    formattedActivities.add(formatActivity(activity));
                }
            }
        }
        builder.getDescriptionBuilder().append(FormatUtil.formatList(formattedActivities, ", "));

        builder.setTitle(String.format("Showing Info For %s %s", username, badges))
                .setColor((m != null && m.getColor() != null) ? m.getColor() : ToycatPallete.DEFAULT_ROLE_WHITE)
                .setThumbnail(m == null ? u.getEffectiveAvatarUrl() : m.getEffectiveAvatarUrl())
                .addField("ID", u.getId(), true)
                .addField("Created At", TimeFormat.DATE_TIME_SHORT.format(u.getTimeCreated()), true);

        if (m != null) {
               builder.addField("Joined At", TimeFormat.DATE_TIME_SHORT.format(m.getTimeJoined()), true);
        }

        builder.addField("Images", getIconURLList(m, u, p), true);

        StringBuilder statusBuilder = new StringBuilder();
        if (m != null) {
            switch (m.getOnlineStatus(ClientType.DESKTOP)) {
                case ONLINE:
                    statusBuilder.append(Constants.DESKTOP_ONLINE);
                    break;
                case IDLE:
                    statusBuilder.append(Constants.DESKTOP_IDLE);
                    break;
                case DO_NOT_DISTURB:
                    statusBuilder.append(Constants.DESKTOP_DND);
                    break;
                case OFFLINE:
                case INVISIBLE:
                    statusBuilder.append(Constants.DESKTOP_OFFLINE);
            }

            switch (m.getOnlineStatus(ClientType.MOBILE)) {
                case ONLINE:
                    statusBuilder.append(Constants.MOBILE_ONLINE);
                    break;
                case IDLE:
                    statusBuilder.append(Constants.MOBILE_IDLE);
                    break;
                case DO_NOT_DISTURB:
                    statusBuilder.append(Constants.MOBILE_DND);
                    break;
                case OFFLINE:
                case INVISIBLE:
                    statusBuilder.append(Constants.MOBILE_OFFLINE);
            }

            switch (m.getOnlineStatus(ClientType.WEB)) {
                case ONLINE:
                    statusBuilder.append(Constants.BROWSER_ONLINE);
                    break;
                case IDLE:
                    statusBuilder.append(Constants.BROWSER_IDLE);
                    break;
                case DO_NOT_DISTURB:
                    statusBuilder.append(Constants.BROWSER_DND);
                    break;
                case OFFLINE:
                case INVISIBLE:
                    statusBuilder.append(Constants.BROWSER_OFFLINE);
            }
        }

        if (m != null) {
            builder.addField("Status", statusBuilder.toString(), true);
        }

        if (p.getAccentColor() != null && p.getBanner() == null) {
            builder.addField("Accent Colour", FormatUtil.formatColor(p.getAccentColorRaw()), true);
        }

        if (m != null && m.isBoosting()) {
            builder.addField("Boosting Since", TimeFormat.DATE_TIME_SHORT.format(m.getTimeBoosted()), true);
        }

        if (m != null) {
            String rolesFormatted = FormatUtil.listOfRolesMention(m.getRoles()).trim();
            builder.addField("Roles", rolesFormatted.isEmpty() ? "None" : rolesFormatted, false);
        }

        return builder.build();
    }

    private static String getIconURLList(Member m, User u, User.Profile p) {
        FormatUtil.IconURLFieldBuilder ibuilder = new FormatUtil.IconURLFieldBuilder();
        if (m != null && m.getAvatarUrl() != null) {
            ibuilder.add("Server PFP", m.getAvatarUrl());
        }

        ibuilder.add("PFP", u.getEffectiveAvatarUrl() + "?size=4096");
        if (p.getBannerUrl() != null) {
            ibuilder.add("Banner", p.getBannerUrl());
        }

        return ibuilder.toString();
    }

    private static String getEmojiFromMiscFlag(User.UserFlag flag) {
        switch (flag) {
            case EARLY_SUPPORTER:
                return Constants.EARLY_NITRO_SUB;
            case ACTIVE_DEVELOPER:
                return Constants.ACTIVE_DEVELOPER;
            case HYPESQUAD_BALANCE:
                return Constants.HYPESQUAD_BALANCE;
            case HYPESQUAD_BRAVERY:
                return Constants.HYPESQUAD_BRAVERY;
            case HYPESQUAD_BRILLIANCE:
                return Constants.HYPESQUAD_BRILIANCE;
            case HYPESQUAD:
                return Constants.HYPESQUAD_EVENTS;
            case BUG_HUNTER_LEVEL_1:
                return Constants.BUG_HUNTER_LEVEL_1;
            case BUG_HUNTER_LEVEL_2:
                return Constants.BUG_HUNTER_LEVEL_2;
            case CERTIFIED_MODERATOR:
                return Constants.MODERATOR_ALUMNI;
            default:
                return "";
        }
    }
}
