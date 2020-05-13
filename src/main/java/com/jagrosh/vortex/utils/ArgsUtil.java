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

import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ArgsUtil
{
    private final static Pattern MENTION = Pattern.compile("^<@!?(\\d{17,19})>");
    private final static Pattern BROKEN_MENTION = Pattern.compile("^@(\\S.{0,30}\\S)#(\\d{4})");
    private final static Pattern ID = Pattern.compile("^(\\d{17,19})");
    private final static String TIME_REGEX = "(?is)^((\\s*-?\\s*\\d+\\s*(d(ays?)?|h((ou)?rs?)?|m(in(ute)?s?)?|s(ec(ond)?s?)?)\\s*,?\\s*(and)?)*).*";
    
    public static ResolvedArgs resolve(String args, Guild guild)
    {
        return resolve(args, false, guild);
    }
    
    public static ResolvedArgs resolve(String args, boolean allowTime, Guild guild)
    {
        Set<Member> members = new LinkedHashSet<>();
        Set<User> users = new LinkedHashSet<>();
        Set<Long> ids = new LinkedHashSet<>();
        Set<String> unresolved = new LinkedHashSet<>();
        Matcher mat;
        User u;
        long i;
        boolean found = true;
        while(!args.isEmpty() && found)
        {
            found = false;
            mat = MENTION.matcher(args);
            if(mat.find())
            {
                i = Long.parseLong(mat.group(1));
                u = guild.getJDA().getUserById(i);
                if(u==null)
                    ids.add(i);
                else if(guild.isMember(u))
                    members.add(guild.getMember(u));
                else
                    users.add(u);
                args = args.substring(mat.group().length()).trim();
                found = true;
                continue;
            }
            mat = BROKEN_MENTION.matcher(args);
            if(mat.find())
            {
                for(User user: guild.getJDA().getUserCache().asList())
                {
                    if(user.getName().equals(mat.group(1)) && user.getDiscriminator().equals(mat.group(2)))
                    {
                        if(guild.isMember(user))
                            members.add(guild.getMember(user));
                        else
                            users.add(user);
                        found = true;
                        break;
                    }
                }
                args = args.substring(mat.group().length()).trim();
                if(found)
                    continue;
                unresolved.add(FormatUtil.filterEveryone(mat.group()));
                found = true;
                continue;
            }
            mat = ID.matcher(args);
            if(mat.find())
            {
                try
                {
                    i = Long.parseLong(mat.group(1));
                }catch(NumberFormatException ex)
                {
                    i = 0;
                }
                u = guild.getJDA().getUserById(i);
                if(u==null)
                    ids.add(i);
                else if(guild.isMember(u))
                    members.add(guild.getMember(u));
                else
                    users.add(u);
                args = args.substring(mat.group().length()).trim();
                found = true;
            }
        }
        int time = 0;
        if(allowTime)
        {
            String timeString = args.replaceAll(TIME_REGEX, "$1");
            if(!timeString.isEmpty())
            {
                args = args.substring(timeString.length()).trim();
                time = OtherUtil.parseTime(timeString);
            }
        }
        return new ResolvedArgs(members, users, ids, unresolved, time, args);
    }
    
    public static class ResolvedArgs
    {
        public final Set<Member> members;
        public final Set<User> users;
        public final Set<Long> ids;
        public final Set<String> unresolved;
        public final int time;
        public final String reason;
        
        private ResolvedArgs(Set<Member> members, Set<User> users, Set<Long> ids, Set<String> unresolved, int time, String reason)
        {
            this.members = members;
            this.users = users;
            this.ids = ids;
            this.unresolved = unresolved;
            this.time = time;
            this.reason = reason;
        }
        
        public boolean isEmpty()
        {
            return members.isEmpty() && users.isEmpty() && ids.isEmpty() && unresolved.isEmpty();
        }
    }
}
