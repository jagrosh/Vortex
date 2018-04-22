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
package com.jagrosh.vortex.commands.owner;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.commands.CommandExceptionListener.CommandErrorException;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ReloadCmd extends Command
{
    private final Vortex vortex;
    
    public ReloadCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "reload";
        this.arguments = "<ref|safe|copy>";
        this.help = "reloads a file";
        this.ownerCommand = true;
        this.guildOnly = false;
        this.hidden = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        switch(event.getArgs().toLowerCase())
        {
            case "ref":
                vortex.getAutoMod().loadReferralDomains();
                event.replySuccess("Reloaded ref domains");
                break;
            case "safe":
                vortex.getAutoMod().loadSafeDomains();
                event.replySuccess("Reloaded safe domains");
                break;
            case "copy":
                vortex.getAutoMod().loadCopypastas();
                event.replySuccess("Reloaded copypastas");
                break;
            default:
                throw new CommandErrorException("Invalid reload selection: `ref` `safe` `copy`");
        }
    }
}
