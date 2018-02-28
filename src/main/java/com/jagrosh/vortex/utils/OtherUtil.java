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
package com.jagrosh.vortex.utils;

import net.dv8tion.jda.core.Region;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class OtherUtil
{
    public static Role getMutedRole(Guild guild)
    {
        return guild.getRoles().stream().filter(r -> r.getName().equalsIgnoreCase("Muted")).findFirst().orElse(null);
    }
    
    public static void safeDM(User user, String message, Runnable then)
    {
        if(user==null)
            then.run();
        else
            user.openPrivateChannel()
                    .queue(pc -> pc.sendMessage(message).queue(s->then.run(), 
                            f->then.run()), f->then.run());
    }
    
    public static Member findMember(String username, String discriminator, Guild guild)
    {
        return guild.getMembers().stream().filter(m -> m.getUser().getName().equals(username) && m.getUser().getDiscriminator().equals(discriminator)).findAny().orElse(null);
    }
    
    public static int parseTime(String timestr)
    {
        timestr = timestr.replaceAll("(?i)(\\s|,|and)","")
                .replaceAll("(?is)(-?\\d+|[a-z]+)", "$1 ")
                .trim();
        String[] vals = timestr.split("\\s+");
        int timeinseconds = 0;
        try
        {
            for(int j=0; j<vals.length; j+=2)
            {
                int num = Integer.parseInt(vals[j]);
                if(vals[j+1].toLowerCase().startsWith("m"))
                    num*=60;
                else if(vals[j+1].toLowerCase().startsWith("h"))
                    num*=60*60;
                else if(vals[j+1].toLowerCase().startsWith("d"))
                    num*=60*60*24;
                timeinseconds+=num;
            }
        }
        catch(Exception ex)
        {
            return 0;
        }
        return timeinseconds;
    }
    
    public static String regionToFlag(Region region)
    {
        switch(region)
        {
            case AMSTERDAM:        return "\uD83C\uDDF3\uD83C\uDDF1";
            case BRAZIL:           return "\uD83C\uDDE7\uD83C\uDDF7";
            case EU_CENTRAL:       return "\uD83C\uDDEA\uD83C\uDDFA";
            case EU_WEST:          return "\uD83C\uDDEA\uD83C\uDDFA";
            case FRANKFURT:        return "\uD83C\uDDE9\uD83C\uDDEA";
            case HONG_KONG:        return "\uD83C\uDDED\uD83C\uDDF0";
            case JAPAN:            return "\uD83C\uDDEF\uD83C\uDDF5";
            case LONDON:           return "\uD83C\uDDEC\uD83C\uDDE7";
            case RUSSIA:           return "\uD83C\uDDF7\uD83C\uDDFA";
            case SINGAPORE:        return "\uD83C\uDDF8\uD83C\uDDEC";
            case SYDNEY:           return "\uD83C\uDDE6\uD83C\uDDFA";
            case US_CENTRAL:       return "\uD83C\uDDFA\uD83C\uDDF8";
            case US_EAST:          return "\uD83C\uDDFA\uD83C\uDDF8";
            case US_SOUTH:         return "\uD83C\uDDFA\uD83C\uDDF8";
            case US_WEST:          return "\uD83C\uDDFA\uD83C\uDDF8";
            case VIP_AMSTERDAM:    return "\uD83C\uDDF3\uD83C\uDDF1";
            case VIP_BRAZIL:       return "\uD83C\uDDE7\uD83C\uDDF7";
            case VIP_EU_CENTRAL:   return "\uD83C\uDDEA\uD83C\uDDFA";
            case VIP_EU_WEST:      return "\uD83C\uDDEA\uD83C\uDDFA";
            case VIP_FRANKFURT:    return "\uD83C\uDDE9\uD83C\uDDEA";
            case VIP_JAPAN:        return "\uD83C\uDDEF\uD83C\uDDF5";
            case VIP_LONDON:       return "\uD83C\uDDEC\uD83C\uDDE7";
            case VIP_SINGAPORE:    return "\uD83C\uDDF8\uD83C\uDDEC";
            case VIP_SYDNEY:       return "\uD83C\uDDE6\uD83C\uDDFA";
            case VIP_US_CENTRAL:   return "\uD83C\uDDFA\uD83C\uDDF8";
            case VIP_US_EAST:      return "\uD83C\uDDFA\uD83C\uDDF8";
            case VIP_US_SOUTH:     return "\uD83C\uDDFA\uD83C\uDDF8";
            case VIP_US_WEST:      return "\uD83C\uDDFA\uD83C\uDDF8";
            case UNKNOWN: default: return "\uD83C\uDFF3";
        }
    }
}
