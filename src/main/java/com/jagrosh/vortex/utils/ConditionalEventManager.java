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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.InterfacedEventManager;
import net.dv8tion.jda.api.sharding.ShardManager;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class ConditionalEventManager extends InterfacedEventManager
{
    protected abstract List<ShardManager> getOrderedShardManagers();
    
    @Override
    public void handle(GenericEvent ge)
    {
        // check if this guild is already loaded by some other shard
        long selfId;
        try
        {
            selfId = ge.getJDA().getSelfUser().getIdLong();
        }
        catch ( IllegalStateException ex) // selfid not ready yet
        {
            return;
        }
        Guild guild;
        try
        {
            guild = (Guild) ge.getClass().getMethod("getGuild").invoke(ge);
        }
        catch (NoSuchMethodException | InvocationTargetException ex) // no getGuild method or not in guild
        {
            guild = null;
        }
        catch (IllegalAccessException ex) // something actually went wrong
        {
            guild = null;
            ex.printStackTrace();
        }
        long guildId = guild == null ? 0 : guild.getIdLong();
        
        if(guildId != 0)
        {
            for(ShardManager bot: getOrderedShardManagers())
            {
                if(bot.getShards().get(0).getSelfUser().getIdLong() == selfId)
                {
                    break;
                }
                if(bot.getGuildById(guildId) != null)
                {
                    return;
                }
            }
        }
        
        // otherwise, continue as normal
        super.handle(ge);
    }
}
