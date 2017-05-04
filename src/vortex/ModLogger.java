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

import com.jagrosh.jdautilities.commandclient.CommandEvent;
import java.time.OffsetDateTime;
import java.util.List;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.utils.PermissionUtil;
import net.dv8tion.jda.core.utils.SimpleLog;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class ModLogger {
    
    private final SimpleLog LOG = SimpleLog.getLog("ModLogger");
    private final DatabaseManager manager;
    
    public ModLogger(DatabaseManager manager)
    {
        this.manager = manager;
    }
    
    public void logCommand(Message msg)
    {
        TextChannel tc = manager.getModlogChannel(msg.getGuild());
        if(tc==null || !PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS))
            return;
        tc.sendMessage(new EmbedBuilder()
                .setColor(tc.getGuild().getSelfMember().getColor())
                .setTimestamp(msg.getCreationTime())
                .setFooter(msg.getAuthor().getName()+"#"+msg.getAuthor().getDiscriminator()+" | #"+msg.getTextChannel().getName(), msg.getAuthor().getEffectiveAvatarUrl())
                .setDescription(msg.getRawContent())
                .build()).queue();
    }
    
    public void logAutomod(Message msg, Action action, String reason)
    {
        TextChannel tc = manager.getModlogChannel(msg.getGuild());
        if(tc==null || !PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS))
            return;
        tc.sendMessage(new EmbedBuilder()
                .setColor(tc.getGuild().getSelfMember().getColor())
                .setTimestamp(msg.getCreationTime())
                .setFooter(msg.getJDA().getSelfUser().getName()+" automod | #"+msg.getTextChannel().getName(), msg.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .setDescription(msg.getAuthor().getAsMention()+" was **"+action.getVerb()+"** for "+reason)
                .build()).queue();
    }
    
    public void logAutomod(Member member, Action action, String reason)
    {
        TextChannel tc = manager.getModlogChannel(member.getGuild());
        if(tc==null || !PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS))
            return;
        tc.sendMessage(new EmbedBuilder()
                .setColor(tc.getGuild().getSelfMember().getColor())
                .setTimestamp(OffsetDateTime.now())
                .setFooter(member.getJDA().getSelfUser().getName()+" automod", member.getJDA().getSelfUser().getEffectiveAvatarUrl())
                .setDescription(member.getUser().getAsMention()+" was **"+action.getVerb()+"** for "+reason)
                .build()).queue();
    }
    
    public void logEmbed(Guild guild, MessageEmbed embed)
    {
        TextChannel tc = manager.getModlogChannel(guild);
        if(tc==null || !PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS))
            return;
        tc.sendMessage(embed).queue();
    }
    
    public void logMessage(Guild guild, String message)
    {
        TextChannel tc = manager.getModlogChannel(guild);
        if(tc==null || !PermissionUtil.checkPermission(tc, tc.getGuild().getSelfMember(), Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS))
            return;
        List<String> msgs = CommandEvent.splitMessage(message);
        msgs.forEach(msg -> tc.sendMessage(msg).queue());
    }
    
    public static Role getMutedRole(Guild guild)
    {
        return guild.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("muted")).findFirst().orElse(null);
    }
}
