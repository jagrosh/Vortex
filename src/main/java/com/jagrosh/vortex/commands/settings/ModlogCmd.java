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
package com.jagrosh.vortex.commands.settings;

import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.LogCommand;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ModlogCmd extends LogCommand
{
    public ModlogCmd(Vortex vortex)
    {
        super(vortex);
        this.name = "modlog";
        this.help = "sets channel to log moderation actions";
        this.botPermissions = new Permission[]{Permission.VIEW_AUDIT_LOGS};
    }

    @Override
    protected void showCurrentChannel(CommandEvent event)
    {
        TextChannel tc = vortex.getDatabase().settings.getSettings(event.getGuild()).getModLogChannel(event.getGuild());
        if(tc==null)
            event.replyWarning("Moderation Logs are not currently enabled on the server. Please include a channel name.");
        else
            event.replySuccess("Moderation Logs are currently being sent in "+tc.getAsMention()
                    +(event.getSelfMember().hasPermission(tc, REQUIRED_PERMS) ? "" : "\n"+event.getClient().getWarning()+String.format(REQUIRED_ERROR, tc.getAsMention())));
    }

    @Override
    protected void setLogChannel(CommandEvent event, TextChannel tc)
    {
        vortex.getDatabase().settings.setModLogChannel(event.getGuild(), tc);
        vortex.getModLogger().setNeedUpdate(event.getGuild());
        if(tc==null)
            event.replySuccess("Moderation Logs will not be sent");
        else
            event.replySuccess("Moderation Logs will now be sent in "+tc.getAsMention());
    }
}
