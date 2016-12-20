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
package vortex.commands;

import me.jagrosh.jdacommands.Command;
import me.jagrosh.jdacommands.CommandEvent;
import net.dv8tion.jda.core.Permission;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SetupCmd extends Command {

    public SetupCmd()
    {
        this.name = "setup";
        this.help = "sets up the bot on the server";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.botPermissions = new Permission[]{Permission.ADMINISTRATOR};
    }
    
    @Override
    protected void execute(CommandEvent event) {
    }
    
}
