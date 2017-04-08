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

import java.awt.Color;
import java.util.concurrent.TimeUnit;
import com.jagrosh.jdautilities.commandclient.Command;
import com.jagrosh.jdautilities.commandclient.CommandEvent;
import com.jagrosh.jdautilities.menu.buttonmenu.ButtonMenuBuilder;
import com.jagrosh.jdautilities.waiter.EventWaiter;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Role;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;
import net.dv8tion.jda.core.utils.PermissionUtil;
import vortex.ModLogger;
import vortex.data.DatabaseManager;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SetupCmd extends Command {

    private final DatabaseManager manager;
    private final ButtonMenuBuilder buttons;
    private final ButtonMenuBuilder confirmation;
    private final String MUTE = "\uD83D\uDD07";
    private final String MODLOG = "\uD83D\uDCD3";
    private final String CANCEL = "\u274C";
    private final String CONFIRM = "\u2611";
    public SetupCmd(EventWaiter waiter, DatabaseManager manager)
    {
        this.manager = manager;
        this.name = "setup";
        this.category = new Category("Settings");
        this.help = "does server setup";
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.botPermissions = new Permission[]{Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ADD_REACTION};
        this.buttons = new ButtonMenuBuilder()
                .setText("Please select a setup option!")
                .setDescription(MUTE+" 'Muted' Role\n"+MODLOG+" Moderation Log\n"+CANCEL+" Cancel")
                .setChoices(MUTE,MODLOG,CANCEL)
                .setEventWaiter(waiter)
                .setTimeout(1, TimeUnit.MINUTES)
                ;
        this.confirmation = new ButtonMenuBuilder()
                .setChoices(CONFIRM, CANCEL)
                .setEventWaiter(waiter)
                .setTimeout(1, TimeUnit.MINUTES)
                ;
    }
    
    @Override
    protected void execute(CommandEvent event) {
        buttons.setAction(e -> {
                    switch(e.getName()) {
                        case MUTE:
                            Role m = ModLogger.getMutedRole(event.getGuild());
                            if(!PermissionUtil.checkPermission(event.getGuild(), event.getSelfMember(), Permission.MANAGE_ROLES))
                            {
                                event.reply(event.getClient().getError()+" I need the "+Permission.MANAGE_ROLES+" permission to set up the muted role!");
                                return;
                            }
                            if(m!=null)
                            {
                                if(!PermissionUtil.canInteract(event.getSelfMember(), m))
                                {
                                    event.reply(event.getClient().getError()+" I cannot interact with the muted role!");
                                    return;
                                }
                                if(!PermissionUtil.canInteract(event.getMember(), m))
                                {
                                    event.reply(event.getClient().getError()+" You do not have permission to interact with the muted role!");
                                    return;
                                }
                            }
                            confirmation.setText(event.getClient().getWarning()+(m==null ? " This will create and set up a 'Muted' role. Continue?" : 
                                    " This will edit the existing 'Muted' role. Continue?"))
                                        .setAction(e2 -> {
                                            if(e2.getName().equals(CONFIRM))
                                            {
                                                if(m==null)
                                                    event.getGuild().getController().createRole().queue(role -> {
                                                        role.getManagerUpdatable()
                                                                .getNameField().setValue("Muted")
                                                                .getPermissionField().setPermissions(new Permission[0])
                                                                .getColorField().setValue(new Color(54, 57, 62))
                                                                .update().queue(v -> setUpMutedRole(role, event), 
                                                                        t -> event.reply(event.getClient().getError()+" Failed to edit the newly-created role"));
                                                    }, t -> event.reply(event.getClient().getError()+" Failed to create the role"));
                                                else
                                                    setUpMutedRole(m, event);
                                            }
                                        }).setUsers(event.getAuthor())
                                        .build().display(event.getChannel());
                            break;
                            
                        case MODLOG:
                            TextChannel tc = manager.getModlogChannel(event.getGuild());
                            if(tc!=null)
                                event.reply(event.getClient().getError()+" A modlog channel already exists: <#"+tc.getId()+">");
                            else
                            {
                                if(!PermissionUtil.checkPermission(event.getGuild(), event.getSelfMember(), Permission.MANAGE_CHANNEL))
                                {
                                    event.reply(event.getClient().getError()+" I need the "+Permission.MANAGE_CHANNEL+" permission to set up the moderation log!");
                                    return;
                                }
                                confirmation.setText(event.getClient().getWarning()+" This will create a new channel to track moderation options. Continue?")
                                        .setAction(e2 -> {
                                            if(e2.getName().equals(CONFIRM))
                                            {
                                                event.getGuild().getController().createTextChannel("modlog").queue(tchan -> {
                                                    tchan.getManager().setTopic("Log of moderation actions").queue();
                                                    tchan.createPermissionOverride(tchan.getGuild().getSelfMember()).queue(po -> {
                                                        po.getManager().grant(Permission.MESSAGE_WRITE, Permission.MESSAGE_READ, Permission.MESSAGE_EMBED_LINKS).queue();
                                                    });
                                                    tchan.createPermissionOverride(tchan.getGuild().getPublicRole()).queue(po -> {
                                                        po.getManager().deny(Permission.MESSAGE_WRITE).queue();
                                                    });
                                                }, t -> {
                                                    event.reply(event.getClient().getError()+" I failed to create the modlog channel.");
                                                });
                                            }
                                        }).setUsers(event.getAuthor())
                                        .build().display(event.getChannel());
                            }
                            break;
                    }
                }).setUsers(event.getAuthor())
                .setColor(event.getSelfMember().getColor())
                .build().display(event.getChannel());
    }
    
    private void setUpMutedRole(Role role, CommandEvent event)
    {
        StringBuilder builder = new StringBuilder();
        for(TextChannel tc : role.getGuild().getTextChannels())
        {
            try{
                if(tc.getPermissionOverride(role)!=null)
                    tc.getPermissionOverride(role).getManager().deny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue();
                else
                    tc.createPermissionOverride(role).queue(po -> po.getManager().deny(Permission.MESSAGE_WRITE, Permission.MESSAGE_ADD_REACTION).queue());
            }catch(PermissionException e) {
                builder.append("\n").append(event.getClient().getError()).append(" I cannot set up the muted role in ").append(tc.getAsMention());
            }
        }
        for(int i=0; i<role.getGuild().getVoiceChannels().size(); i++)
        {
            VoiceChannel vc = role.getGuild().getVoiceChannels().get(i);
            boolean last = i+1 == role.getGuild().getVoiceChannels().size();
            try{
                if(vc.getPermissionOverride(role)!=null)
                    vc.getPermissionOverride(role).getManager().deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                        .queue(last ? (v -> event.reply(event.getClient().getSuccess()+" Muted role setup has completed.")) : null);
                else
                    vc.createPermissionOverride(role).queue(po -> po.getManager().deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK)
                        .queue(last ? (v -> event.reply(event.getClient().getSuccess()+" Muted role setup has completed.")) : null));
            }catch(PermissionException e) {
                builder.append("\n").append(event.getClient().getError()).append(" I cannot set up the muted role in ").append(vc.getName());
            }
        }
        if(!builder.toString().isEmpty())
            event.reply(builder.toString());
    }
}