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
 * limitations under the License. Furthermore, I'm putting this sentence in all files because I messed up git and its not showing files as edited -\\_( :) )_/-
 */
package com.jagrosh.vortex.commands.automod;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.commands.CommandTools;
import net.dv8tion.jda.api.Permission;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class AntiinviteCmd extends Command
{
    private final Vortex vortex;
    
    public AntiinviteCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "antiinvite";
        this.guildOnly = true;
        this.aliases = new String[] {
                "antinvite", "antiinvites", "antinvites", "invitefiltering",
                "anti-invite", "anti-invites", "invite-filter", "invitefilter"
        };
        this.category = new Category("AutoMod");
        this.arguments = "<on/off|whitelist...>";
        this.help = "enables/disabled invite filter, adds invites to whitelist";
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.children = new Command[] {new WhitelistInvitesCmd(vortex)};
    }

    @Override
    protected void execute(CommandEvent event) {
        boolean enabled = false;
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please specify if you want to turn on or off invites");
            try {
                enabled = CommandTools.parseEnabledDisabled(event.getArgs().split(" ")[0]);
            } catch (IllegalArgumentException e){
                event.replyError("Please specify if invite filtering should be on or off.");
                return;
            }
        }

        vortex.getDatabase().automod.enableInviteFilter(event.getGuild(), enabled);
        event.replySuccess("Invite filter turned " + (enabled ? "on." : "off."));
    }
}