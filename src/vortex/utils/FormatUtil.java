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
package vortex.utils;

import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class FormatUtil {
    
    public static String filterEveryone(String input)
    {
        return input.replace("@everyone","@veryone").replace("@here","@hre");
    }
    
    public static String formatUser(User user)
    {
        return filterEveryone("**"+user.getName()+"**#"+user.getDiscriminator());
    }
    
    public static String formatFullUser(User user)
    {
        return filterEveryone("**"+user.getName()+"** (ID:"+user.getId()+")");
    }
}
