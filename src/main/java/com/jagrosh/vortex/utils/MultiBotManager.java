/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.api.sharding.ShardManager;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MultiBotManager
{
    private final List<ShardManager> bots;
    
    protected MultiBotManager(List<DefaultShardManagerBuilder> builders) throws LoginException, IllegalArgumentException
    {
        this.bots = new ArrayList<>();
        for(DefaultShardManagerBuilder b: builders)
        {
            b.setEventManagerProvider(i -> new MultiBotEventManager());
            b.setBulkDeleteSplittingEnabled(false);
            b.setRequestTimeoutRetry(true);
            bots.add(b.build());
        }
    }
    
    public List<ShardManager> getShardManagers()
    {
        return bots;
    }
    
    public RestAction<User> retrieveUserById(long id)
    {
        if(!bots.isEmpty())
            return bots.get(0).retrieveUserById(id);
        return null;
    }
    
    public Guild getGuildById(long id)
    {
        for(ShardManager shards: bots)
            if(shards.getGuildById(id) != null)
                return shards.getGuildById(id);
        return null;
    }
    
    public User getUserById(long id)
    {
        for(ShardManager shards: bots)
            if(shards.getUserById(id) != null)
                return shards.getUserById(id);
        return null;
    }
    
    public TextChannel getTextChannelById(long id)
    {
        for(ShardManager shards: bots)
            if(shards.getTextChannelById(id) != null)
                return shards.getTextChannelById(id);
        return null;
    }
    
    private class MultiBotEventManager extends ConditionalEventManager
    {
        @Override
        protected List<ShardManager> getOrderedShardManagers()
        {
            return bots;
        }
    }
    
    public static class MultiBotManagerBuilder
    {
        private final List<DefaultShardManagerBuilder> builders = new ArrayList<>();
        
        public MultiBotManagerBuilder addBot(String token, Collection<GatewayIntent> intents)
        {
            builders.add(DefaultShardManagerBuilder.create(token, intents));
            return this;
        }
        
        public MultiBotManagerBuilder setMemberCachePolicy(MemberCachePolicy policy)
        {
            builders.forEach(b -> b.setMemberCachePolicy(policy));
            return this;
        }
        
        public MultiBotManagerBuilder enableCache(CacheFlag... flags)
        {
            return enableCache(Arrays.asList(flags));
        }
        
        public MultiBotManagerBuilder enableCache(Collection<CacheFlag> flags)
        {
            builders.forEach(b -> b.enableCache(flags));
            return this;
        }
        
        public MultiBotManagerBuilder disableCache(CacheFlag... flags)
        {
            return disableCache(Arrays.asList(flags));
        }
        
        public MultiBotManagerBuilder disableCache(Collection<CacheFlag> flags)
        {
            builders.forEach(b -> b.disableCache(flags));
            return this;
        }
        
        public MultiBotManagerBuilder addEventListeners(Object... listeners)
        {
            builders.forEach(b -> b.addEventListeners(listeners));
            return this;
        }
        
        public MultiBotManagerBuilder setStatus(OnlineStatus status)
        {
            builders.forEach(b -> b.setStatus(status));
            return this;
        }
        
        public MultiBotManagerBuilder setActivity(Activity activity)
        {
            builders.forEach(b -> b.setActivity(activity));
            return this;
        }
        
        public MultiBotManager build() throws LoginException, IllegalArgumentException
        {
            return new MultiBotManager(builders);
        }
    }
}
