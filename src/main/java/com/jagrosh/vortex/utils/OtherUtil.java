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

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class OtherUtil
{
    private final static Logger LOG = LoggerFactory.getLogger(OtherUtil.class);
    
    public final static char[] DEHOIST_ORIGINAL =     {'!',      '"',      '#',      '$',      '%',      
        '&',      '\'',     '(',      ')',      '*',      '+',      ',',      '-',      '.',      '/'};
    public final static char[] DEHOIST_REPLACEMENTS = {'\u01C3', '\u201C', '\u2D4C', '\uFF04', '\u2105',     // visually
        '\u214B', '\u2018', '\u2768', '\u2769', '\u2217', '\u2722', '\u201A', '\u2013', '\u2024', '\u2044'}; // similar
    public final static String DEHOIST_JOINED = "`"+FormatUtil.join("`, `", DEHOIST_ORIGINAL)+"`";
    
    public final static boolean dehoist(Member m, char symbol)
    {
        if(!m.getGuild().getSelfMember().canInteract(m))
            return false;
        if(m.getEffectiveName().charAt(0)>symbol)
            return false;
        
        String newname = m.getEffectiveName();
        for(int i=0; i<DEHOIST_ORIGINAL.length; i++)
        {
            if(DEHOIST_ORIGINAL[i] == newname.charAt(0))
            {
                newname = DEHOIST_REPLACEMENTS[i] + (newname.length() == 1 ? "" : newname.substring(1));
                break;
            }
        }
        m.getGuild().getController().setNickname(m, newname).reason("Dehoisting").queue();
        return true;
    }
    
    public static void safeDM(User user, String message, boolean shouldDM, Runnable then)
    {
        if(user==null || !shouldDM)
            then.run();
        else try
        {
            user.openPrivateChannel()
                    .queue(pc -> pc.sendMessage(message).queue(s->then.run(), 
                            f->then.run()), f->then.run());
        }
        catch(Exception ignore) {}
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
    
    public static String[] readLines(String filename)
    {
        try
        {
            List<String> values = Files.readAllLines(Paths.get("lists" + File.separator + filename + ".txt")).stream()
                    .map(str -> str.replace("\uFEFF", "").trim()).filter(str -> !str.isEmpty() && !str.startsWith("//")).collect(Collectors.toList());
            String[] list = new String[values.size()];
            for(int i=0; i<list.length; i++)
                list[i] = values.get(i);
            LOG.info("Successfully read "+list.length+" entries from '"+filename+"'");
            return list;
        }
        catch(Exception ex)
        {
            LOG.error("Failed to read '"+filename+"':"+ ex);
            return new String[0];
        }
    }
}
