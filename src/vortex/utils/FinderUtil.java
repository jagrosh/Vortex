/*
 * Copyright 2016 jagrosh.
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
package vortex.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.VoiceChannel;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FinderUtil {
    
    
    public static List<VoiceChannel> findVoiceChannel(String query, Guild guild)
    {
        String id;
        if(query.matches("<#\\d+>"))
        {
            id = query.replaceAll("<#(\\d+)>", "$1");
            VoiceChannel vc = guild.getJDA().getVoiceChannelById(id);
            if(vc!=null && vc.getGuild().equals(guild))
                return Collections.singletonList(vc);
        }
        ArrayList<VoiceChannel> exact = new ArrayList<>();
        ArrayList<VoiceChannel> wrongcase = new ArrayList<>();
        ArrayList<VoiceChannel> startswith = new ArrayList<>();
        ArrayList<VoiceChannel> contains = new ArrayList<>();
        String lowerquery = query.toLowerCase();
        guild.getVoiceChannels().stream().forEach((vc) -> {
            if(vc.getName().equals(lowerquery))
                exact.add(vc);
            else if (vc.getName().equalsIgnoreCase(lowerquery) && exact.isEmpty())
                wrongcase.add(vc);
            else if (vc.getName().toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty())
                startswith.add(vc);
            else if (vc.getName().toLowerCase().contains(lowerquery) && startswith.isEmpty())
                contains.add(vc);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
    
    public static List<Role> findRole(String query, Guild guild)
    {
        String id= query.replaceAll("<@&(\\d+)>", "$1");
        try {
            Role roler = guild.getRoleById(id);
            if(roler!=null)
                return Collections.singletonList(roler);
        } catch(Exception e) {}
        
        ArrayList<Role> exact = new ArrayList<>();
        ArrayList<Role> wrongcase = new ArrayList<>();
        ArrayList<Role> startswith = new ArrayList<>();
        ArrayList<Role> contains = new ArrayList<>();
        String lowerquery = query.toLowerCase();
        guild.getRoles().stream().forEach((role) -> {
            if(role.getName().equals(lowerquery))
                exact.add(role);
            else if (role.getName().equalsIgnoreCase(lowerquery) && exact.isEmpty())
                wrongcase.add(role);
            else if (role.getName().toLowerCase().startsWith(lowerquery) && wrongcase.isEmpty())
                startswith.add(role);
            else if (role.getName().toLowerCase().contains(lowerquery) && startswith.isEmpty())
                contains.add(role);
        });
        if(!exact.isEmpty())
            return exact;
        if(!wrongcase.isEmpty())
            return wrongcase;
        if(!startswith.isEmpty())
            return startswith;
        return contains;
    }
}
