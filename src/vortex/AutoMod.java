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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.ReconnectedEvent;
import net.dv8tion.jda.core.events.ResumedEvent;
import net.dv8tion.jda.core.events.ShutdownEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleAddEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberRoleRemoveEvent;
import net.dv8tion.jda.core.events.message.guild.GenericGuildMessageEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageUpdateEvent;
import net.dv8tion.jda.core.events.role.GenericRoleEvent;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.entities.Invite;
import vortex.utils.RestUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class AutoMod extends ListenerAdapter {
    private final Pattern INVITES = Pattern.compile("discord(?:\\.gg|app.com\\/invite)\\/([A-Z0-9_]+)",Pattern.CASE_INSENSITIVE);
    private final HashMap<String,Integer> antimention = new HashMap<>();
    private final HashMap<String,Action> antiinvite = new HashMap<>();
    private final HashMap<String,OffsetDateTime> warnings = new HashMap<>();
    private final ScheduledExecutorService threadpool;
        
    public AutoMod(ScheduledExecutorService threadpool)
    {
        this.threadpool = threadpool;
    }

    @Override
    public void onReady(ReadyEvent event) {
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onResume(ResumedEvent event) {
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onReconnect(ReconnectedEvent event) {
        updateAllGuilds(event.getJDA());
    }

    @Override
    public void onShutdown(ShutdownEvent event) {
        threadpool.shutdown();
    }

    @Override
    public void onGuildMessageUpdate(GuildMessageUpdateEvent event) {
        performAutomod(event);
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
        performAutomod(event);
    }
    
    public void performAutomod(GenericGuildMessageEvent event) {
        //simple automod
        
        //ignore bots
        if(event.getAuthor().isBot())
            return;
        
        Member me = event.getGuild().getSelfMember();
        
        //ignore users with Manage Messages, Kick Members, Ban Members, Manage Server, or anyone the bot can't interact with
        if(!PermissionUtil.canInteract(me, event.getMember()) ||
                event.getMember().getRoles().stream().anyMatch(r -> r.getName().toLowerCase().equals("vortexshield")) ||
                PermissionUtil.checkPermission(event.getChannel(), event.getMember(), Permission.MESSAGE_MANAGE) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.KICK_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.BAN_MEMBERS) ||
                PermissionUtil.checkPermission(event.getGuild(), event.getMember(), Permission.MANAGE_SERVER))
            return;
        
        /*
            check for automod actions
            * AntiMention - prevent mass-mention spammers
            * AntiInvite - prevent invite links to other servers
        */
        
        // anti-mention
        int maxMentions = antimention.getOrDefault(event.getGuild().getId(), 0);
        if(maxMentions>6)
        {
            long mentions = event.getMessage().getMentionedUsers().stream().filter(u -> !u.isBot() && !u.equals(event.getAuthor())).count();
            if(mentions >= maxMentions)
            {
                try{
                    event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                            ModLogger.logAction(Action.BAN, event.getMessage(), "Mentioning "+mentions+" users");
                        });
                } catch(Exception e){}
            }
        }
        
        // anti-invite
        Action type = antiinvite.get(event.getGuild().getId());
        if(type!=null && (event.getChannel().getTopic()==null || !event.getChannel().getTopic().toLowerCase().contains("{invites}")))
        {
            List<String> invites = new ArrayList<>();
            Matcher m = INVITES.matcher(event.getMessage().getRawContent());
            while(m.find())
                invites.add(m.group(1));
            for(String inviteCode : invites)
            {
                Invite invite = RestUtil.resolveInvite(inviteCode);
                if(invite==null || !invite.getGuildId().equals(event.getGuild().getId()))
                {
                    try{
                        event.getMessage().deleteMessage().queue();
                    }catch(PermissionException ex){}
                    try{
                    switch(type)
                    {
                        case BAN:
                            event.getGuild().getController().ban(event.getMember(), 1).queue(v -> {
                                ModLogger.logAction(Action.BAN, event.getMessage(), "Posting an invite link: http://discordapp.com/invite/"+inviteCode);
                            });
                            break;
                        case KICK:
                            event.getGuild().getController().kick(event.getMember()).queue(v -> {
                                ModLogger.logAction(Action.KICK, event.getMessage(), "Posting an invite link: http://discordapp.com/invite/"+inviteCode);
                            });
                            break;
                        case MUTE:
                        case WARN:
                            String key = event.getAuthor().getId()+"|"+event.getGuild().getId();
                            OffsetDateTime lastWarning = warnings.get(key);
                            if(lastWarning==null || lastWarning.isBefore(event.getMessage().getCreationTime().minusMinutes(1)))
                            {
                                event.getChannel().sendMessage(event.getMember().getAsMention()+": Please do not post invite links here.").queue();
                                warnings.put(key, event.getMessage().getCreationTime());
                            }
                            break;
                    }
                    }catch(PermissionException ex){}
                    break;
                }
            }
        }
    }

    @Override
    public void onGenericRole(GenericRoleEvent event) {
        updateRoleSettings(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleAdd(GuildMemberRoleAddEvent event) {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            updateRoleSettings(event.getGuild());
    }

    @Override
    public void onGuildMemberRoleRemove(GuildMemberRoleRemoveEvent event)
    {
        if(event.getMember().equals(event.getGuild().getSelfMember()))
            updateRoleSettings(event.getGuild());
    }
    
    
    public void updateAllGuilds(JDA jda)
    {
        jda.getGuilds().stream().forEach(g -> {
            updateRoleSettings(g);
        });
    }
    
    public void updateRoleSettings(Guild guild)
    {
        antimention.remove(guild.getId());
        antiinvite.remove(guild.getId());
        guild.getSelfMember().getRoles().stream().forEach(r -> {
            if(r.getName().toLowerCase().startsWith("antimention"))
            {
                try{
                    int maxmentions = Integer.parseInt(r.getName().split(":",2)[1].trim());
                    antimention.put(guild.getId(), maxmentions);
                }catch(NumberFormatException | ArrayIndexOutOfBoundsException | NullPointerException e){}
            }
            else if(r.getName().toLowerCase().startsWith("antiinvite"))
            {
                try{
                    String type = r.getName().split(":",2)[1].trim().toUpperCase();
                    switch(type) {
                        case "BAN":
                            antiinvite.put(guild.getId(), Action.BAN);
                            break;
                        case "KICK":
                            antiinvite.put(guild.getId(), Action.KICK);
                            break;
                        case "MUTE":
                            antiinvite.put(guild.getId(), Action.MUTE);
                            break;
                        case "WARN":
                            antiinvite.put(guild.getId(), Action.WARN);
                            break;
                        case "DELETE":
                            antiinvite.put(guild.getId(), Action.DELETE);
                            break;
                    }
                }catch(Exception e){}
            }
        });
    }
}
