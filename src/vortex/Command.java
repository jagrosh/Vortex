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

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.utils.PermissionUtil;

/**
 *
 * @author John Grosh (jagrosh)
 */
public abstract class Command {
    protected String name;
    protected String help = "no help available";
    protected String arguments;
    protected Type type = Type.ALL;
    protected Permission[] requiredPermissions = new Permission[0];
    protected boolean ownerCommand = false;
    
    protected abstract Void execute(String args, MessageReceivedEvent event);
    
    public void run(String args, MessageReceivedEvent event)
    {
        if(ownerCommand && !event.getAuthor().getId().equals(Constants.OWNER_ID))
            return;
        
        if(!type.availableIn(event.getChannelType()))
        {
            event.getChannel().sendMessage(String.format(Constants.NOT_IN_X, event.getChannelType().name())).queue();
            return;
        }
        
        if(event.getChannelType()==ChannelType.TEXT)
        {
            for(Permission p : requiredPermissions)
            {
                if(p.isChannel())
                {
                    if(!PermissionUtil.checkPermission(event.getTextChannel(), event.getMember(), p))
                    {
                        event.getChannel().sendMessage(String.format(Constants.USER_NEEDS_PERMISSION, p, "channel")).queue();
                        return;
                    }
                }
                else
                {
                    if(!PermissionUtil.checkPermission(event.getGuild(), event.getMember(), p))
                    {
                        event.getChannel().sendMessage(String.format(Constants.USER_NEEDS_PERMISSION, p, "guild")).queue();
                        return;
                    }
                }
            }
        }
        
        execute(args, event);
    }
    
    protected static Void reply(String response, MessageReceivedEvent event)
    {
        event.getChannel().sendMessage(response).queue();
        return null;
    }
    
    public enum Type {
        ALL, GUILDONLY;
        
        public boolean availableIn(ChannelType type)
        {
            switch(this){
                case ALL:
                    return true;
                case GUILDONLY:
                    return type==ChannelType.TEXT;
                default:
                    return false;
            }
        }
    }
}
