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

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public abstract class Filter 
{
    public final int strikes;
    public abstract boolean test(String message);
    
    private Filter(int strikes)
    {
        this.strikes = strikes;
    }
    
    public static class WordFilter extends Filter
    {
        public final String word;
        
        public WordFilter(int strikes, String word)
        {
            super(strikes);
            this.word = " "+word.replaceAll("\\s+", " ").toLowerCase()+" ";
        }
        
        @Override
        public boolean test(String message) 
        {
            return (" "+message.replaceAll("\\s+", " ").toLowerCase()+" ").contains(word);
        }
    }
    
    public static class GlobFilter extends Filter
    {
        public final String glob;
        
        public GlobFilter(int strikes, String glob)
        {
            super(strikes);
            this.glob = glob.replaceAll("\\s+", " ").toLowerCase();
        }

        @Override
        public boolean test(String message)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
    
    public static class RegexFilter extends Filter
    {
        public final Pattern pattern;
        
        public RegexFilter(int strikes, String pattern) throws PatternSyntaxException
        {
            super(strikes);
            this.pattern = Pattern.compile(pattern);
        }

        @Override
        public boolean test(String message)
        {
            return pattern.matcher(message).find();
        }
    }
}
