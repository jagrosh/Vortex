/*
 * Copyright 2020 John Grosh (john.a.grosh@gmail.com).
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
import net.dv8tion.jda.api.Permission;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ExportCmd extends Command
{
    private final Vortex vortex;
    
    public ExportCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "export";
        this.arguments = "<serverid>";
        this.help = "exports all server data as json";
        this.category = new Category("Tools");
        this.userPermissions = new Permission[]{Permission.MESSAGE_ATTACH_FILES};
        this.ownerCommand = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        long gid;
        try
        {
            gid = Long.parseLong(event.getArgs());
        }
        catch(NumberFormatException ex)
        {
            event.reactError();
            return;
        }
        JSONObject obj = new JSONObject();
        
    }
}
