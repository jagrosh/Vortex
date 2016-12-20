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
package vortex.entities;

import vortex.Action;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class GuildConfig {
    
    private int antiMentionLevel;
    private Action antiMentionAction;
    
    private int antiInviteLevel;
    private Action antiInviteAction;
    
    // setters
    public void setAntiMention(int level, Action action)
    {
        this.antiMentionLevel = level;
        this.antiMentionAction = action;
    }
    
    public void setAntiInvite(int level, Action action)
    {
        this.antiInviteLevel = level;
        this.antiInviteAction = action;
    }
    
    // booleans
    public boolean isAntiMention()
    {
        return antiMentionAction!=null;
    }
    
    public boolean isAntiInvite()
    {
        return antiInviteAction!=null;
    }
    
    // getters
    public int getAntiMentionLevel()
    {
        return antiMentionLevel;
    }
    
    public Action getAntiMentionAction()
    {
        return antiMentionAction;
    }
    
    public int getAntiInviteLevel()
    {
        return antiInviteLevel;
    }
    
    public Action getAntiInviteAction()
    {
        return antiInviteAction;
    }
}
