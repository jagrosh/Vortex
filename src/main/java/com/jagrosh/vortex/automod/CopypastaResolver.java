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

import com.jagrosh.vortex.utils.OtherUtil;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class CopypastaResolver
{
    private final HashMap<String, String[]> copypastas = new HashMap<>();
    
    public void load()
    {
        String[] lines = OtherUtil.readLines("copypastas");
        if(lines.length!=0)
        {
            copypastas.clear();
            String name;
            String[] words;
            for(String line: lines)
            {
                name = line.substring(0, line.indexOf("||")).trim();
                words = line.substring(line.indexOf("||")+2).trim().split("\\s+&&\\s+");
                for(int i=0; i<words.length; i++)
                    words[i] = words[i].trim().toLowerCase();
                copypastas.put(name, words);
            }
        }
    }
    
    public String getCopypasta(String message)
    {
        String lower = message.toLowerCase();
        boolean contains;
        String[] words;
        for(String name: copypastas.keySet())
        {
            words = copypastas.get(name);
            if(words==null || words.length==0)
                continue;
            contains = true;
            for(String word: words)
            {
                if(!lower.contains(word))
                {
                    contains = false;
                    break;
                }
            }
            if(contains)
                return name;
        }
        return null;
    }
}
