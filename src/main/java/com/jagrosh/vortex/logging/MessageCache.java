/*
 * Copyright 2017 John Grosh (jagrosh).
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

import com.jagrosh.vortex.utils.FixedCache;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class MessageCache
{
    private final static int SIZE = 1000;
    private final HashMap<Long,FixedCache<Long,Message>> cache = new HashMap<>();
    
    public Message putMessage(Message m)
    {
        if(!cache.containsKey(m.getGuild().getIdLong()))
            cache.put(m.getGuild().getIdLong(), new FixedCache<>(SIZE));
        return cache.get(m.getGuild().getIdLong()).put(m.getIdLong(), m);
    }
    
    public Message pullMessage(Guild guild, long messageId)
    {
        if(!cache.containsKey(guild.getIdLong()))
            return null;
        return cache.get(guild.getIdLong()).pull(messageId);
    }
    
    public List<Message> getMessages(Guild guild, Predicate<Message> predicate)
    {
        if(!cache.containsKey(guild.getIdLong()))
            return Collections.EMPTY_LIST;
        return cache.get(guild.getIdLong()).getValues().stream().filter(predicate).collect(Collectors.toList());
    }
}
