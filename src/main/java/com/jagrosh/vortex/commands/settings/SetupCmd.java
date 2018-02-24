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
package com.jagrosh.vortex.commands.settings;

import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.PermissionOverride;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SetupCmd extends Command
{
    private final ButtonMenu.Builder buttons;
    private final String MUTE = "\uD83D\uDD07";
    private final String LOGS = "\uD83D\uDCDD";
    private final String AUTOMOD = "\uD83E\uDD16";
    
    private final String CANCEL = "\u274C";
    private final String CONFIRM = "\u2611";
    
    private final Vortex vortex;
    
    public SetupCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "setup";
        this.category = new Category("Settings");
        this.help = "server setup";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.botPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.buttons = new ButtonMenu.Builder()
                .setText(Constants.SUCCESS+" **Please select a setup option**:\n\n"
                        +MUTE+" 'Muted' Role\n"
                        +AUTOMOD+" Automod\n"
                        +CANCEL+" Cancel")
                .setChoices(MUTE,AUTOMOD,CANCEL)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES)
                .setFinalAction(m -> m.delete().queue())
                ;
        this.cooldown = 20;
        this.cooldownScope = CooldownScope.GUILD;
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        buttons.setAction(re -> 
        {
            switch(re.getName())
            {
                case MUTE:
                    Role muted = OtherUtil.getMutedRole(event.getGuild());
                    String confirmation;
                    if(muted!=null)
                    {
                        if(!event.getSelfMember().canInteract(muted))
                        {
                            event.replyError("I cannot interact with the existing '"+muted.getName()+"' role. Please move my role(s) higher and then try again.");
                            return;
                        }
                        if(!event.getMember().canInteract(muted))
                        {
                            event.replyError("You do not have permission to interact with the existing '"+muted.getName()+"' role.");
                            return;
                        }
                        confirmation = "This will modify the existing '"+muted.getName()+"' role and assign it overrides in every channel.";
                    }
                    else
                        confirmation = "This will create a role called 'Muted' and assign it overrides in every channel.";
                    waitForConfirmation(event, confirmation, () -> setUpMutedRole(event, muted));
                    break;
                case AUTOMOD:
                    break;
            }
        }).setUsers(event.getAuthor()).build().display(event.getChannel());
    }
    
    private void setUpMutedRole(CommandEvent event, Role role)
    {
        StringBuilder sb = new StringBuilder(Constants.SUCCESS+" Muted role setup started!\n");
        event.reply(sb + Constants.LOADING+" Initializing role...", m -> event.async(() -> 
        {
            try
            {
                Role mutedRole;
                if(role==null)
                {
                    mutedRole = event.getGuild().getController().createRole().setName("Muted").setPermissions().setColor(1).complete();
                }
                else
                {
                    role.getManager().setPermissions().complete();
                    mutedRole = role;
                }
                sb.append(Constants.SUCCESS+" Role initialized!\n");
                m.editMessage(sb + Constants.LOADING+" Making Category overrides...").complete();
                PermissionOverride po;
                for(net.dv8tion.jda.core.entities.Category cat: event.getGuild().getCategories())
                {
                    po = cat.getPermissionOverride(mutedRole);
                    if(po==null)
                        cat.createPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                    else
                        po.getManager().deny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                }
                sb.append(Constants.SUCCESS+" Category overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Text Channel overrides...").complete();
                for(TextChannel tc: event.getGuild().getTextChannels())
                {
                    po = tc.getPermissionOverride(mutedRole);
                    if(po==null)
                        tc.createPermissionOverride(mutedRole).setDeny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).complete();
                    else
                        po.getManager().deny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).complete();
                }
                sb.append(Constants.SUCCESS+" Text Channel overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Voice Channel overrides...").complete();
                for(VoiceChannel vc: event.getGuild().getVoiceChannels())
                {
                    po = vc.getPermissionOverride(mutedRole);
                    if(po==null)
                        vc.createPermissionOverride(mutedRole).setDeny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                    else
                        po.getManager().deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                }
                m.editMessage(sb + Constants.SUCCESS+" Voice Channel overrides complete!\n\n" + Constants.SUCCESS+" Muted role setup has completed!").queue();
            }
            catch(Exception ex)
            {
                m.editMessage(sb + Constants.ERROR+" An error occurred setting up the Muted role. Please check that I have the Administrator permission and that the Muted role is below my roles.").queue();
            }
        }));
    }
    
    private void waitForConfirmation(CommandEvent event, String message, Runnable confirm)
    {
        new ButtonMenu.Builder()
                .setChoices(CONFIRM, CANCEL)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES)
                .setText(Constants.WARNING+" "+message+"\n\n"+CONFIRM+" Continue\n"+CANCEL+" Cancel")
                .setFinalAction(m -> m.delete().queue())
                .setUsers(event.getAuthor())
                .setAction(re ->
                {
                    if(re.getName().equals(CONFIRM))
                        confirm.run();
                }).build().display(event.getChannel());
    }
}