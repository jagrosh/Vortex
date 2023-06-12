/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.logging;

import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database.Modlog;
import com.jagrosh.vortex.logging.MessageCache.CachedMessage;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.LogUtil;
import com.jagrosh.vortex.utils.Usage;
import com.typesafe.config.Config;
import java.awt.Color;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.Temporal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.guild.member.GuildMemberRemoveEvent;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceUpdateEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateAvatarEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateDiscriminatorEvent;
import net.dv8tion.jda.api.events.user.update.UserUpdateNameEvent;
import net.dv8tion.jda.api.exceptions.PermissionException;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.utils.FileUpload;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class BasicLogger
{
    private final static String REDIRECT = "\uD83D\uDD00"; // 🔀
    private final static String REDIR_MID = "\uD83D\uDD39"; // 🔹
    private final static String REDIR_END = "\uD83D\uDD37"; // 🔷

    private final Vortex vortex;
    private final AvatarSaver avatarSaver;
    private final Usage usage = new Usage();
    
    public BasicLogger(Vortex vortex, Config config)
    {
        this.vortex = vortex;
        this.avatarSaver = new AvatarSaver(config);
    }

    @Deprecated
    public Usage getUsage()
    {
        return usage;
    }

    @Deprecated
    private void log(OffsetDateTime now, TextChannel tc, String emote, String message, MessageEmbed embed)
    {
        try
        {
            usage.increment(tc.getGuild().getIdLong());
            tc.sendMessage(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now, vortex.getDatabase().settings.getSettings(tc.getGuild()).getTimezone(), emote, message)))
                    .setEmbeds(embed).queue();
        }
        catch(PermissionException ignore) {}
    }

    @Deprecated
    private void logFile(OffsetDateTime now, TextChannel tc, String emote, String message, byte[] file, String filename)
    {
        try
        {
            usage.increment(tc.getGuild().getIdLong());
            tc.sendMessage(FormatUtil.filterEveryone(LogUtil.basiclogFormat(now, vortex.getDatabase().settings.getSettings(tc.getGuild()).getTimezone(), emote, message)))
                            .addFiles(FileUpload.fromData(file, filename)).queue();
        }
        catch(PermissionException ignore) {}
    }
    
    // Message Logs
    
    public void logMessageEdit(Message newMessage, CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(newMessage.getGuild());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(newMessage.getGuild()).getMessageLogChannel(newMessage.getGuild());
        if(tc==null)
            return;
        if(newMessage.getContentRaw().equals(oldMessage.getContentRaw()))
            return;

        User u = newMessage.getAuthor();
        Guild guild = newMessage.getGuild();
        EmbedBuilder edit = new EmbedBuilder();
            edit.setColor(Color.BLUE)
            .setAuthor(getLoggingName(guild, u), null, u.getEffectiveAvatarUrl())
                .appendDescription(
                        String.format("Message edited in <#%s> [Jump to Message](https://discordapp.com/channels/%s/%s/%s)\n",
                                newMessage.getChannel().getId(),
                                guild.getId(),
                                newMessage.getChannel().getId(),
                                newMessage.getId()
                        )
                )
                .setFooter("User ID: " + u.getId() + " | Message ID: " + newMessage.getId(), null)
                .setTimestamp(newMessage.getTimeEdited()==null ? newMessage.getTimeCreated() : newMessage.getTimeEdited())
                .addField("Before:", FormatUtil.formatMessage(oldMessage), false)
                .addField("After:", FormatUtil.formatMessage(newMessage), false);
        /*String newm = FormatUtil.formatMessage(newMessage);
        if(edit.getDescriptionBuilder().length()+9+newm.length()>2048)
            edit.addField("To:\n", newm.length()>1024 ? newm.substring(0,1016)+" (...)" : newm, false);
        else
            edit.appendDescription("\n**To:**\n"+newm);*/
        log(guild, edit);
    }
    
    public void logMessageDelete(CachedMessage oldMessage)
    {
        if(oldMessage==null)
            return;
        Guild guild = oldMessage.getGuild(vortex.getJda());
        if(guild==null)
            return;
        TextChannel mtc = oldMessage.getTextChannel(vortex.getJda());
        PermissionOverride po = mtc.getPermissionOverride(guild.getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getMessageLogChannel(guild);
        if(tc==null)
            return;
        String formatted = FormatUtil.formatMessage(oldMessage);
        if(formatted.isEmpty())
            return;
        User author = oldMessage.getAuthor(vortex.getJda());
        log(guild, embedBuilder -> embedBuilder
            .setColor(Color.yellow)
            .setAuthor(author==null ? getLoggingName(oldMessage) : getLoggingName(guild, author), null, author == null ? null : author.getEffectiveAvatarUrl())
            .appendDescription("**Message sent by <@" + (author == null ? oldMessage.getAuthorId() : author.getId()) + "> deleted in <#" + oldMessage.getTextChannelId() +">**\n")
            .appendDescription(formatted)
            .setFooter("Author ID: " + (author==null ? oldMessage.getAuthorId() : author.getId()) + " | Message ID: " + oldMessage.getId(), null)
            .setTimestamp(Instant.now())
        );
    }
    
    public void logMessageBulkDelete(List<CachedMessage> messages, int count, TextChannel text)
    {
        if(count==0)
            return;
        Guild guild = text.getGuild();
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if(tc==null)
            return;
        if(messages.isEmpty())
            return;
        TextChannel mtc = messages.get(0).getTextChannel(vortex.getJda());
        PermissionOverride po = mtc.getPermissionOverride(mtc.getGuild().getSelfMember());
        if(po!=null && po.getDenied().contains(Permission.MESSAGE_HISTORY))
            return;
        if(messages.size()==1)
            logMessageDelete(messages.get(0));
        vortex.getTextUploader().upload(
                LogUtil.logCachedMessagesForwards("Deleted Messages", messages, vortex.getJda()), "DeletedMessages", (view, download) ->
                    log(guild, embedBuilder -> embedBuilder
                            .setColor(Color.YELLOW)
                            .setDescription(
                                    String.format("**Bulk delete in %s, %d messages were deleted**\nClick to [view](%s) or [download](%s) the deleted messages.",
                                            text.getAsMention(), count, view, download
                                    )
                            )
                            .setFooter("Channel ID: " + text.getId(), null)
                            .setTimestamp(Instant.now())
                    )
        );
    }

    public void postCleanCase(Member moderator, OffsetDateTime now, int numMessages, TextChannel target, String criteria, String reason, String view, String download)
    {
        log(target.getGuild(), embedBuilder -> embedBuilder
                .setColor(Color.YELLOW)
                .setAuthor(getLoggingName(moderator.getGuild(), moderator.getUser()), null, moderator.getUser().getEffectiveAvatarUrl())
                .appendDescription(
                    String.format("**%s purged %d messages in %s**\n**Criteria:** %s\n%sClick to [view](%s) or [download](%s) the deleted messages.",
                        moderator.getAsMention(),
                        numMessages,
                        target.getAsMention(),
                        criteria,
                        reason == null || reason.trim().isEmpty() ? "" : "**Reason:** " + reason + "\n",
                        view,
                        download
                    )
                )
                .setFooter("Mod ID: " + moderator.getUser().getId() + " | Channel ID: " + target.getId(), null)
                .setTimestamp(now)
        );
    }

    //TODO: Will be worked on in the future
    public void logRedirectPath(Message message, String link, List<String> redirects)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(message.getGuild()).getMessageLogChannel(message.getGuild());
        if(tc==null)
            return;
        StringBuilder sb = new StringBuilder(REDIR_END+" **"+link+"**");
        for(int i=0; i<redirects.size(); i++)
            sb.append("\n").append(redirects.size()-1==i ? REDIR_END + " **" : REDIR_MID).append(redirects.get(i)).append(redirects.size()-1==i ? "**" : "");
        log(OffsetDateTime.now(), tc, REDIRECT, 
                FormatUtil.formatFullUser(message.getAuthor())+"'s message in "+message.getChannel().getAsMention()+" contained redirects:",
                new EmbedBuilder().setColor(Color.BLUE.brighter().brighter()).appendDescription(sb.toString()).build());

        /*log(tc.getGuild(), embedBuilder -> embedBuilder
            .setColor(Color.YELLOW)
                .setAuthor(author==null ? getLoggingName(oldMessage) : getLoggingName(guild, author), author == null ? null : author.getEffectiveAvatarUrl())*/
    }

    public void logNameChange(UserUpdateNameEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        User user = event.getUser();
        user.getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getServerLogChannel(guild))
            .filter(Objects::nonNull)
            .forEachOrdered(tc ->
            {
                log(tc.getGuild(), embedBuilder -> embedBuilder
                        .setAuthor(getLoggingName(tc.getGuild(), user), null, user.getEffectiveAvatarUrl())
                        .setColor(Color.GREEN)
                        .setDescription(
                            String.format("**%s has changed their username from %s#%s to %s#%s**",
                                user.getAsMention(),
                                event.getOldName(),
                                user.getDiscriminator(),
                                event.getNewName(),
                                user.getDiscriminator()
                            )
                        )
                        .setFooter("User ID: " + event.getUser().getId(), null)
                        .setTimestamp(now)
                );
            });
    }
    
    public void logNameChange(UserUpdateDiscriminatorEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        User user = event.getUser();
        event.getUser().getMutualGuilds().stream()
            .map(guild -> vortex.getDatabase().settings.getSettings(guild).getServerLogChannel(guild))
            .filter(Objects::nonNull)
            .forEachOrdered(tc ->
            {
                log(tc.getGuild(), embedBuilder -> embedBuilder
                        .setAuthor(getLoggingName(tc.getGuild(), user), null, user.getEffectiveAvatarUrl())
                        .setColor(Color.GREEN)
                        .setDescription(
                                String.format("**%s has changed their username from %s#%s to %s#%s**",
                                        user.getAsMention(),
                                        user.getName(),
                                        event.getOldDiscriminator(),
                                        user.getName(),
                                        event.getNewDiscriminator()
                                )
                        )
                        .setFooter("User ID: " + event.getUser().getId(), null)
                        .setTimestamp(now)
                );
            });
    }

    public void logGuildJoin(GuildMemberJoinEvent event, OffsetDateTime now)
    {
        long seconds = event.getUser().getTimeCreated().until(now, ChronoUnit.SECONDS);
        String newText;

        if (event.getUser().getTimeCreated().isAfter(now))
            newText = "This account has joined \"before\" being created and is deffintly an alt.";
        else
            newText = String.format("Account created on %s (%s ago)",
                            event.getUser().getTimeCreated().format(DateTimeFormatter.RFC_1123_DATE_TIME),
                            FormatUtil.secondsToTimeCompact(seconds));

        Guild guild = event.getGuild();
        User user = event.getUser();
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(Color.BLUE)
                .appendDescription("**" + user.getAsMention() + " joined the server**\n")
                .appendDescription(newText)
                .setFooter("User ID: " + user.getId(), null)
                .setTimestamp(now)
        );
    }
    
    public void logGuildLeave(GuildMemberRemoveEvent event)
    {
        OffsetDateTime now = OffsetDateTime.now();
        long seconds = event.getMember().getTimeJoined().until(now, ChronoUnit.SECONDS);
        StringBuilder rlist;
        Member member = event.getMember();
        Guild guild = event.getGuild();
        User user = event.getUser();

        if(member.getRoles().isEmpty())
            rlist = new StringBuilder();
        else
        {
            rlist= new StringBuilder("\n\nThey had the "+member.getRoles().get(0).getAsMention());
            int size = member.getRoles().size();
            for(int i=1; i<size; i++)
                rlist.append("`, `").append(member.getRoles().get(i).getAsMention());
            rlist.append(" role").append(size > 1 ? "s" : "").append(" before leaving");
        }

        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setThumbnail(user.getEffectiveAvatarUrl())
                .setColor(Color.BLUE)
                .appendDescription("**" + user.getAsMention() + " left or was kicked/banned from the server**")
                .appendDescription(String.format("\nThis user originally joined on %s (%s ago)",
                        member.getTimeJoined().format(DateTimeFormatter.RFC_1123_DATE_TIME),
                        FormatUtil.secondsToTimeCompact(seconds)
                    )
                )
                .appendDescription(rlist.toString().isEmpty() ? "" : rlist.toString())
                .setFooter("User ID: " + user.getId(), null)
                .setTimestamp(now)
        );
    }

    public void logVoiceUpdate(GuildVoiceUpdateEvent event) {
        if (event.getChannelJoined() != null && event.getChannelLeft() != null) {
            logVoiceMove(event);
        } else if (event.getChannelJoined() != null) {
            logVoiceJoin(event);
        } else {
            logVoiceLeave(event);
        }
    }
    // Voice Logs
    private void logVoiceJoin(GuildVoiceUpdateEvent event)
    {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String vcId = event.getChannelJoined().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setColor(Color.BLUE)
                .appendDescription(String.format("**%s joined the voice channel <#%s>**",
                        user.getAsMention(),
                        vcId
                    )
                )
                .setFooter("User ID: " + user.getId() + " |  VC ID: " + vcId, null)
                .setTimestamp(OffsetDateTime.now())
        );
    }
    
    private void logVoiceMove(GuildVoiceUpdateEvent event)
    {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String newVcId = event.getChannelJoined().getId();
        String oldVcId = event.getChannelLeft().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setColor(Color.BLUE)
                .appendDescription(String.format("**%s moved voice channels from <#%s> to <#%s>**",
                        user.getAsMention(),
                        oldVcId,
                        newVcId
                    )
                )
                .setFooter(String.format("User ID: %s | Old VC Id: %s | New VC ID: %s",
                        user.getId(),
                        oldVcId,
                        newVcId
                ), null)
                .setTimestamp(OffsetDateTime.now())
        );
    }
    
    private void logVoiceLeave(GuildVoiceUpdateEvent event)
    {
        Guild guild = event.getGuild();
        Member member = event.getMember();
        String vcId = event.getChannelLeft().getId();
        User user = member.getUser();
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setColor(Color.BLUE)
                .appendDescription(String.format("**%s left the voice channel <#%s>**",
                        user.getAsMention(),
                        vcId
                        )
                )
                .setFooter("User ID: " + user.getId() + " |  VC ID: " + vcId, null)
                .setTimestamp(OffsetDateTime.now())
        );
    }
    
    
    // Avatar Logs
    public void logAvatarChange(UserUpdateAvatarEvent event) {
        OffsetDateTime now = OffsetDateTime.now();
        List<TextChannel> logs = event.getUser()
                .getMutualGuilds()
                .stream()
                .map(guild -> vortex.getDatabase().settings.getSettings(guild).getAvatarLogChannel(guild))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (logs.isEmpty())
            return;
        User user = event.getUser();
        vortex.getThreadpool().execute(() ->
        {
            byte[] im = avatarSaver.makeAvatarImage(event.getUser(), event.getOldAvatarUrl(), event.getOldAvatarId());
            if (im != null)
                logs.forEach(tc ->
                {
                    Guild guild = tc.getGuild();
                    try {
                        tc.sendMessage(event.getUser().getAvatarId() != null && event.getUser().getAvatarId().startsWith("a_") ? " <:gif:314068430624129039>" : "")
                                .setFiles(FileUpload.fromData(im, "AvatarChange.png"))
                                .addEmbeds(new EmbedBuilder()
                                        .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                                        .setColor(Color.BLUE)
                                        .setDescription("**" + user.getAsMention() + " changed their avatar**")
                                        .setFooter("User ID: " + user.getId(), null)
                                        .setTimestamp(now)
                                        .build()
                                ).queue();

                    } catch (PermissionException ignore) {
                    }
                });
        });
    }

    public void logModlog(Guild guild, Modlog modlog)
    {
        String verb;
        boolean punishing = modlog.getSaviorId() == -1;

        switch (modlog.getType()) {
        case GRAVEL:
            verb = "graveled";
            break;
        case MUTE:
            verb = "muted";
            break;
        case WARN:
            verb = "warned";
            break;
        case BAN:
            verb = "banned";
            break;
        case SOFTBAN:
            verb = "softbanned";
            break;
        case KICK:
            verb = "kicked";
            break;
        default:
            verb = "";
        }


        String howLong = "";
        Instant finish = modlog.getFinnish(), start = modlog.getStart();
        if (finish != null && start != null && finish.getEpochSecond() != Instant.MAX.getEpochSecond()) {
            howLong = " for " + FormatUtil.secondsToTimeCompact(finish.getEpochSecond() - start.getEpochSecond());
            if (!punishing)
                howLong = " after they were " + verb + " " + howLong;
        }
        final String HOW_LONG = howLong;

        User user = guild.getJDA().getUserById(modlog.getUserId());
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, user), null, user.getEffectiveAvatarUrl())
                .setColor(Color.RED)
                .appendDescription(String.format("<@%d> %s <@%d>%s",
                        punishing ? modlog.getModId() : modlog.getSaviorId(),
                        punishing ? verb : "un" + verb,
                        modlog.getUserId(),
                        HOW_LONG
                    )
                )
                .setFooter(String.format("User ID: %d | %s ID: %d",
                        modlog.getUserId(),
                        punishing ? "Mod" : "Savior",
                        punishing ? modlog.getModId() : modlog.getSaviorId()
                    )
                , null)
                .appendDescription("\n**Case ID:**" + modlog.getId())
                .appendDescription(modlog.getReason() == null || modlog.getReason().trim().isEmpty() ? "" : "\n**Reason:**" + modlog.getReason())
                .setTimestamp(punishing ? modlog.getStart() : modlog.getFinnish())
        );
    }

    public void logModlogUpdate(Guild guild, long caseId, User updatingModerator, String oldReason, String newReason, Temporal now)
    {
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, updatingModerator), null, updatingModerator.getEffectiveAvatarUrl())
                .setColor(Color.ORANGE.darker())
                .appendDescription(updatingModerator.getAsMention() + " updated the reason for case " + caseId)
                .addField("Old Reason", oldReason.isEmpty() ? "_No Reason Specified_" : oldReason, false)
                .addField("New Reason", newReason, false)
                .setFooter("Updating Moderator ID: " + updatingModerator.getId(), null)
                .setTimestamp(now)
        );
    }

    public void logModlogDeletion(Guild guild, Modlog modlog, User deletingModerator) {
        log(guild, embedBuilder -> embedBuilder
                .setAuthor(getLoggingName(guild, deletingModerator), null, deletingModerator.getEffectiveAvatarUrl())
                .setColor(Color.ORANGE.darker())
                .appendDescription(deletingModerator.getAsMention() + " deleted a modlog")
                .addField("Case " + modlog.getId(), FormatUtil.formatModlogCase(vortex, guild, modlog), true)
                .setFooter(String.format("Deleting Moderator ID: %s | User ID: %d | Punishing Moderator ID:%d%s",
                        deletingModerator.getId(),
                        modlog.getUserId(),
                        modlog.getModId(),
                        modlog.getSaviorId() == -1 ? "" : " | Savior ID: " + modlog.getSaviorId()
                    ), null)
                .setTimestamp(Instant.now())
        );
    }

    public void logAuditLogEntry(AuditLogEntry ale)
    {
        Guild guild = ale.getGuild();
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if(tc==null)
            return;
        Function<EmbedBuilder, EmbedBuilder> builder = null;
        switch (ale.getType())
        {
            case MEMBER_ROLE_UPDATE:
                {
                    String field;
                    String[] addedRoles, removedRoles;

                    try
                    {
                        ArrayList<HashMap<String,String>> newValue= ale.getChangeByKey(AuditLogKey.MEMBER_ROLES_ADD).getNewValue();
                        addedRoles = new String[newValue.size()];
                        for (int i = 0; i < addedRoles.length; i++)
                        {
                            addedRoles[i] = newValue.get(i).get("id");
                        }
                    }
                    catch (NullPointerException e)
                    {
                        addedRoles = null;
                    }

                    try
                    {
                        ArrayList<HashMap<String,String>> newValue= ale.getChangeByKey(AuditLogKey.MEMBER_ROLES_REMOVE).getNewValue();
                        removedRoles = new String[newValue.size()];
                        for (int i = 0; i < removedRoles.length; i++)
                        {
                            removedRoles[i] = newValue.get(i).get("id");
                        }
                    }
                    catch (NullPointerException e)
                    {
                        removedRoles = null;
                    }

                    boolean hasRemovedRoles = removedRoles != null && removedRoles.length > 0;
                    boolean hasAddedRoles = addedRoles != null && addedRoles.length > 0;
                    String targetMention = "<@"+ale.getTargetId()+">";
                    String modMention = ale.getUser() == null ? "an unknown moderator" : "<@"+ale.getUser().getId()+">";

                    if (hasAddedRoles && !hasRemovedRoles)
                        field = targetMention + " was given " + FormatUtil.toMentionableRoles(addedRoles) + " by " + modMention;
                    else if (hasRemovedRoles && !hasAddedRoles)
                        field = targetMention + " had " + FormatUtil.toMentionableRoles(removedRoles) + " removed by " + modMention;
                    else if (hasAddedRoles)
                        field = String.format("%s was given %s and had %s removed by %s",
                            targetMention,
                            FormatUtil.toMentionableRoles(addedRoles),
                            FormatUtil.toMentionableRoles(removedRoles),
                            modMention
                        );
                    else
                        return;

                    builder = embedBuilder -> embedBuilder
                            .setColor(Color.BLUE)
                            .setAuthor(getLoggingName(ale), null, getTargetProfilePictureURL(ale))
                            .addField("", field, true)
                            .setFooter("User ID: " + ale.getTargetId() + (ale.getUser() == null ? "" : " | Mod ID: "+ale.getUser().getId()), null)
                            .setTimestamp(ale.getTimeCreated());
                }
                break;
            case MESSAGE_DELETE:
            {

            }
        }
       log(guild, builder);
    }

    public String getLoggingName(AuditLogEntry ale)
    {
        User u = ale.getJDA().getUserById(ale.getTargetIdLong());
        String nickname = ale.getGuild().getMember(u).getEffectiveName();
        return u.getName() + "#" + u.getDiscriminator() + (nickname != null && !nickname.equals(u.getName()) ? " (" + nickname + ")" : "");
    }

    public String getLoggingName(Guild guild, User u)
    {
        if (u == null)
            return "An Unknown User";
        Member m = guild.getMember(u);
        String nickname = m == null ? null : m.getNickname();
        return u.getName() + "#" + u.getDiscriminator() + (nickname != null && !nickname.equals(u.getName()) ? " (" + nickname + ")" : "");
    }

    public String getLoggingName(CachedMessage m)
    {
        return m.getUsername() + "#" + m.getDiscriminator();
    }

    public String getTargetProfilePictureURL(AuditLogEntry ale)
    {
        return ale.getJDA().getUserById(ale.getTargetIdLong()).getEffectiveAvatarUrl();
    }

    public void log(Guild guild, Function<EmbedBuilder, EmbedBuilder> builderFunction)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if(tc==null || builderFunction == null)
            return;

        try
        {
            tc.sendMessageEmbeds(builderFunction.apply(new EmbedBuilder()).build()).queue();
        }
        catch (PermissionException ignore) {}
    }

    public void log(Guild guild, EmbedBuilder embedBuilder)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(guild).getModLogChannel(guild);
        if(tc==null || embedBuilder == null)
            return;

        try
        {
            tc.sendMessageEmbeds(embedBuilder.build()).queue();
        }
        catch (PermissionException ignore) {}
    }
}