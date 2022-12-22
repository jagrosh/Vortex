/*
 * Copyright 2018 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.utils.FixedCache;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.Collections;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Invite;
import net.dv8tion.jda.api.utils.MiscUtil;
import net.dv8tion.jda.internal.entities.InviteImpl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class InviteResolver
{
    private final static String[] BASE_URLS = {"discordapp.com", "ptb.discordapp.com", "canary.discordapp.com", 
        /*"discord.com",*/ "ptb.discord.com", "canary.discord.com", "discord.co", "watchanimeattheoffice.com", "discord.new"};
    private final Logger log = LoggerFactory.getLogger(InviteResolver.class);
    private final FixedCache<String,Long> cached = new FixedCache<>(5000);
    private final OkHttpClient client = new OkHttpClient.Builder().build();
    
    public Invite resolveFull(String code, JDA jda)
    {
        return resolveFull(code, jda.getToken());
    }
    
    public Invite resolveFull(String code, String token)
    {
        log.debug("Attempting to resolve " + code);
        try
        {
            return getInvite(code, token);
        }
        catch(Exception ex)
        {
            return null;
        }
    }
    
    public long resolve(String code, JDA jda)
    {
        return resolve(code, jda.getToken());
    }
    
    public long resolve(String code, String token)
    {
        log.debug("Attempting to resolve " + code);
        if(cached.contains(code))
            return cached.get(code);
        try
        {
            long guildId = getInviteGuild(code, token);
            //long guildId = Invite.resolve(jda, code).complete(false).getGuild().getIdLong();
            cached.put(code, guildId);
            return guildId;
        }
        catch(Exception ex)
        {
            cached.put(code, 0L);
            return 0L;
        }
    }
    
    private long getInviteGuild(String code, String token) throws IOException
    {
        JSONObject json = getInviteJSON(code, token);
        if(json != null)
            return MiscUtil.parseSnowflake(json.getJSONObject("guild").getString("id"));
        return 0L;
    }
    
    private InviteImpl getInvite(String code, String token) throws IOException
    {
        JSONObject json = getInviteJSON(code, token);
        if(json != null)
        {   
            Invite.Channel channel;
            if(json.has("channel"))
            {
                JSONObject c = json.getJSONObject("channel");
                channel = new InviteImpl.ChannelImpl(c.optLong("id"), c.optString("name"), c.optInt("type") == 0 ? ChannelType.TEXT : ChannelType.UNKNOWN);
            }
            else
                channel = null;
            Invite.Guild guild;
            if(json.has("guild"))
            {
                JSONObject g = json.getJSONObject("guild");
                guild = new InviteImpl.GuildImpl(g.optLong("id"), g.optString("icon"), g.optString("name"), g.optString("splash"), Guild.VerificationLevel.UNKNOWN, -1, -1, Collections.emptySet());
            }
            else
                guild = null;
            return new InviteImpl(null, code, false, null, 0, 0, false, OffsetDateTime.now(), 0, channel, guild, null, null, Invite.InviteType.GUILD);
        }
        return null;
    }
    
    private JSONObject getInviteJSON(String code, String token) throws IOException
    {
        Response res = client.newCall(new Request.Builder().get()
                .addHeader("Authorization", token)
                .addHeader("User-Agent", "DiscordBot (VortexInviteResolver, 1.0)")
                .url("https://" + rand(BASE_URLS) + "/api/invites/" + code)
                .build()).execute();
        if(res.isSuccessful())
        {
            return new JSONObject(res.body().string());
        }
        
        // if it failed, log why (usually unknown invite or cloudflare issue)
        try
        {
            String message = new JSONObject(res.body().string()).getString("message");
            log.warn(String.format("Unsuccessful resolving '%s': %s", code, message));
        }
        catch(Exception ex)
        {
            log.warn(String.format("Unsuccessful resolving '%s' | Headers: %s | Body: %s", code, 
                    res.headers().toMultimap().toString(), res.body() == null ? null : res.body().string()));
        }
        return null;
    }
    
    private <T> T rand(T[] items)
    {
        return items[(int)(Math.random() * items.length)];
    }
}
