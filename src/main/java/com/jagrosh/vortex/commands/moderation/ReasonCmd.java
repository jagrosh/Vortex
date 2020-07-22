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
package com.jagrosh.vortex.commands.moderation;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.ModCommand;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ReasonCmd extends ModCommand
{
    public ReasonCmd(Vortex vortex)
    {
        super(vortex, Permission.BAN_MEMBERS);
        this.name = "reason";
        this.help = "updates a modlog reason";
        this.arguments = "[case] <reason>";
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        int caseNum;
        String[] parts = event.getArgs().split("\\s+", 2);
        String str;
        try
        {
            caseNum = Integer.parseInt(parts[0]);
            str = parts.length==1 ? null : parts[1];
        }
        catch(NumberFormatException ex)
        {
            caseNum = -1;
            str = event.getArgs();
        }
        
        if(caseNum<-1 || caseNum==0)
        {
            event.replyError("Case number must be a positive integer! The case number can be omitted to use the latest un-reasoned case.");
            return;
        }
        if(str==null || str.isEmpty())
        {
            event.replyError("Please provide a reason!");
            return;
        }
        
        String fstr = str;
        int fcaseNum = caseNum;
        event.async(() -> 
        {
            int result = vortex.getModLogger().updateCase(event.getGuild(), fcaseNum, fstr);
            switch (result)
            {
                case -1:
                    event.replyError("No modlog is set on this server!");
                    break;
                case -2:
                    event.replyError("I am unable to Read, Write or retrieve History in the modlog!");
                    break;
                case -3:
                    event.replyError("Case `"+fcaseNum+"` could not be found among the recent cases in the modlog!");
                    break;
                case -4:
                    event.replyError("A recent case with no reason could not be found in the modlog!");
                    break;
                default:
                    event.replySuccess("Updated case **"+result+"** in "+vortex.getDatabase().settings.getSettings(event.getGuild()).getModLogChannel(event.getGuild()).getAsMention());
                    break;
            }
        });
    }
}
