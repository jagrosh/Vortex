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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class Filter
{
    public final static int MAX_NAME_LENGTH = 32;
    public final static int MAX_CONTENT_LENGTH = 255;
    public final static int MAX_STRIKES = 100;
    
    private final static String[] TEST_CASES = {"welcome", "i will follow the rules", "this is a sentence"};
    
    public final String name;
    public final int strikes;
    public final List<Item> items;
    
    private Filter(String name, int strikes)
    {
        this.name = name;
        this.strikes = strikes;
        this.items = new ArrayList<>();
    }
    
    public boolean test(String message)
    {
        if(items.isEmpty())
            return false;
        String lower = message.toLowerCase();
        return items.stream().anyMatch(item -> item.test(lower));
    }
    
    public String printContent()
    {
        StringBuilder sb = new StringBuilder();
        items.forEach(item -> sb.append(" ").append(item.print()));
        return sb.toString().trim();
    }
    
    public String printContentEscaped()
    {
        return printContent().replace("*", "\\*").replace("`", "\\`");
    }
    
    public static Filter parseFilter(String name, int strikes, String content) throws IllegalArgumentException
    {
        // pre checks
        if(content.length() > MAX_CONTENT_LENGTH + 50) // parsing may reduce a bit
            throw new IllegalArgumentException("Filter content is longer than " + MAX_CONTENT_LENGTH + " characters");
        if(name.length() > MAX_NAME_LENGTH)
            throw new IllegalArgumentException("Filter name `" + name + "` is longer than " + MAX_NAME_LENGTH + " characters");
        if(strikes < 0)
            throw new IllegalArgumentException("Filter strikes is less than 0");
        if(strikes > MAX_STRIKES)
            throw new IllegalArgumentException("Filter strikes is more than " + MAX_STRIKES);
        
        // begin parsing
        Filter filter = new Filter(name, strikes);
        String current = content.trim();
        while(!current.isEmpty())
        {
            switch(current.charAt(0))
            {
                case Quote.CHAR:
                {
                    int index = current.indexOf(Quote.CHAR, 1);
                    if(index == -1)
                        throw new IllegalArgumentException("Missing closing quotations within provided filtered quote");
                    filter.items.add(new Quote(current.substring(1, index)));
                    current = current.substring(index + 1).trim();
                    break;
                }
                case Regex.CHAR:
                {
                    int index = current.indexOf(Regex.CHAR, 1);
                    if(index == -1)
                        throw new IllegalArgumentException("Missing closing grave accent within provided filtered regex");
                    try
                    {
                        filter.items.add(new Regex(current.substring(1, index)));
                    }
                    catch(PatternSyntaxException ex)
                    {
                        throw new IllegalArgumentException("Invalid regex pattern `" + current.substring(1, index) + "`");
                    }
                    current = current.substring(index + 1).trim();
                    break;
                }
                default:
                {
                    String[] parts = current.split("\\s+", 2);
                    filter.items.add(new Glob(parts[0]));
                    current = parts.length == 1 ? "" : parts[1];
                    break;
                }
            }
        }
        
        // post checks
        if(filter.items.isEmpty())
            throw new IllegalArgumentException("Filter contains no valid filtered items");
        if(filter.printContent().length() > MAX_CONTENT_LENGTH)
            throw new IllegalArgumentException("Filter content is longer than " + MAX_CONTENT_LENGTH + " characters");
        
        // sanity checks
        if(filter.test(""))
            throw new IllegalArgumentException("Filter activates on empty content");
        for(String test: TEST_CASES)
            if(filter.test(test))
                throw new IllegalArgumentException("Filter activates on test case `" + test + "`");
        
        // return
        return filter;
    }
    
    public static abstract class Item
    {
        abstract boolean test(String message);
        abstract String print();
    }
    
    public static class Glob extends Item
    {
        private final boolean startWildcard, endWildcard;
        public final String glob;
        
        public Glob(String glob)
        {
            glob = glob.replaceAll("\\*+", "*"); // remove double wildcards
            this.startWildcard = glob.startsWith("*");
            this.endWildcard = glob.endsWith("*");
            this.glob = glob.substring(startWildcard ? 1 : 0, endWildcard ? glob.length()-1 : glob.length());
        }
        
        @Override
        public boolean test(String message) 
        {
            String lower = message.toLowerCase();
            int index = -1;
            while((index = lower.indexOf(glob, index + 1)) > -1)
            {
                if((startWildcard || isWordBoundary(lower, index - 1)) 
                        && (endWildcard || isWordBoundary(lower, index + glob.length())))
                    return true;
            }
            return false;
        }

        @Override
        String print()
        {
            return (startWildcard ? "*" : "") + glob.trim() + (endWildcard ? "*" : "");
        }
        
        private static boolean isWordBoundary(String str, int index)
        {
            if(index < 0 || index >= str.length())
                return true;
            char c = str.charAt(index);
            return !(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9');
        }
    }
    
    public static class Quote extends Item
    {
        public final static char CHAR = '"';
        public final String quote;
        
        public Quote(String quote)
        {
            this.quote = quote.toLowerCase();
        }

        @Override
        public boolean test(String message)
        {
            return message.contains(quote);
        }

        @Override
        String print()
        {
            return CHAR + quote + CHAR;
        }
    }
    
    public static class Regex extends Item
    {
        public final static char CHAR = '`';
        public final Pattern pattern;
        
        public Regex(String pattern) throws PatternSyntaxException
        {
            this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }

        @Override
        public boolean test(String message)
        {
            return pattern.matcher(message).find();
        }

        @Override
        String print()
        {
            return CHAR + pattern.pattern() + CHAR;
        }
    }
}
