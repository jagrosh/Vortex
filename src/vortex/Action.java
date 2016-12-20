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
package vortex;

/**
 *
 * @author John Grosh (jagrosh)
 */
public enum Action {
    
    BAN("banned", "\uD83D\uDD28"), KICK("kicked", "\uD83D\uDC62"), MUTE("muted", "\uD83D\uDD07"), WARN("warned", "\uD83D\uDDE3"), DELETE("deleted", "\uD83D\uDDD1");

    private final String verb;
    private final String emoji;
    
    private Action(String verb, String emoji)
    {
        this.verb = verb;
        this.emoji = emoji;
    }
    
    public String getVerb()
    {
        return verb;
    }
    
    public String getEmoji()
    {
        return emoji;
    }
    
    public static Action of(String action)
    {
        switch(action.toLowerCase())
        {
            case "ban": return BAN;
            case "kick": return KICK;
            case "mute": return MUTE;
            case "warn": return WARN;
            case "delete": return DELETE;
            default: return null;
        }
    }
}
