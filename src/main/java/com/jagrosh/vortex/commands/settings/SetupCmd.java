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
import com.jagrosh.jdautilities.command.CooldownScope;
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.managers.AutomodManager.AutomodSettings;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.PermissionOverride;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class SetupCmd extends Command
{
    private final String CANCEL = "\u274C"; // ❌
    private final String CONFIRM = "\u2611"; // ☑
    
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
        this.children = new Command[]{new MuteSetupCmd(), new GravelSetupCmd(), new AutomodSetupCmd()};
    }
    
    @Override
    protected void execute(CommandEvent event) 
    {
        StringBuilder sb = new StringBuilder("The following commands can be used to set up a **").append(event.getSelfUser().getName()).append("** feature:\n");
        for(Command cmd: children)
            sb.append("\n`").append(event.getClient().getPrefix()).append(name).append(" ").append(cmd.getName()).append("` - ").append(cmd.getHelp());
        event.replySuccess(sb.toString());
    }
    
    private class AutomodSetupCmd extends Command
    {
        private AutomodSetupCmd()
        {
            this.name = "automod";
            this.aliases = new String[]{"auto"};
            this.category = new Category("Settings");
            this.help = "sets up the auto-moderator";
            this.guildOnly = true;
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
            this.botPermissions = new Permission[]{Permission.ADMINISTRATOR};
            this.cooldown = 20;
            this.cooldownScope = CooldownScope.GUILD;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            waitForConfirmation(event, "This will enable all automod defaults. Any existing settings will not be overwritten.", () -> 
            {
                event.getChannel().sendTyping().queue();
                StringBuilder sb = new StringBuilder("**Automod setup complete!**");
                if(vortex.getDatabase().actions.useDefaultSettings(event.getGuild()))
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Set up default punishments");
                AutomodSettings ams = vortex.getDatabase().automod.getSettings(event.getGuild());
                if(ams.inviteStrikes==0)
                {
                    vortex.getDatabase().automod.setInviteStrikes(event.getGuild(), 2);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Anti-invite set to `2` strikes");
                }
                if(ams.refStrikes==0)
                {
                    vortex.getDatabase().automod.setRefStrikes(event.getGuild(), 3);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Anti-referral set to `3` strikes");
                }
                if(!ams.useAntiDuplicate())
                {
                    vortex.getDatabase().automod.setDupeSettings(event.getGuild(), 1, 2, 4);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Anti-duplicate will start deleting at duplicate `2`, and will assign `1` strike each duplicate starting at duplicate `4`");
                }
                if(ams.copypastaStrikes==0)
                {
                    vortex.getDatabase().automod.setCopypastaStrikes(event.getGuild(), 1);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Anti-copypasta set to `1` strikes");
                }
                if(ams.maxMentions==0)
                {
                    vortex.getDatabase().automod.setMaxMentions(event.getGuild(), 10);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Maximum mentions set to `10` mentions");
                }
                if(ams.maxRoleMentions==0)
                {
                    vortex.getDatabase().automod.setMaxRoleMentions(event.getGuild(), 4);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Maximum role mentions set to `4` mentions");
                }
                if(ams.maxLines==0)
                {
                    vortex.getDatabase().automod.setMaxLines(event.getGuild(), 10);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Maximum lines set to `10` lines");
                }
                if(!ams.useAutoRaidMode())
                {
                    vortex.getDatabase().automod.setAutoRaidMode(event.getGuild(), 10, 10);
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Anti-Raid Mode will activate upon `10` joins in `10` seconds");
                }
                if(ams.dehoistChar==(char)0)
                {
                    vortex.getDatabase().automod.setDehoistChar(event.getGuild(), '!');
                    sb.append("\n").append(event.getClient().getSuccess()).append(" Names starting with `!` will be dehoisted");
                }
                sb.append("\n").append(event.getClient().getWarning()).append(" Any settings not shown here were not set due to being already set. Please check the automod section of the wiki (<")
                        .append(Constants.Wiki.AUTOMOD).append(">) for more information about the automod.");
                event.replySuccess(sb.toString());
            });
        }
    }
    
    private class MuteSetupCmd extends Command
    {
        private MuteSetupCmd()
        {
            this.name = "muterole";
            this.aliases = new String[]{"muted","mute","mutedrole"};
            this.category = new Category("Settings");
            this.help = "sets up the 'Muted' role";
            this.guildOnly = true;
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
            this.botPermissions = new Permission[]{Permission.ADMINISTRATOR};
            this.cooldown = 20;
            this.cooldownScope = CooldownScope.GUILD;
        }
        
        @Override
        protected void execute(CommandEvent event)
        {
            Role muted = vortex.getDatabase().settings.getSettings(event.getGuild()).getMutedRole(event.getGuild());
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
        }
    }

    private class GravelSetupCmd extends Command
    {
        private GravelSetupCmd()
        {
            this.name = "gravelrole";
            this.aliases = new String[]{"graveled","gravel","graveledrole"};
            this.category = new Category("Settings");
            this.help = "sets up the 'Graveled' role";
            this.guildOnly = true;
            this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
            this.botPermissions = new Permission[]{Permission.ADMINISTRATOR};
            this.cooldown = 20;
            this.cooldownScope = CooldownScope.GUILD;
        }

        @Override
        protected void execute(CommandEvent event)
        {
            Role graveled = vortex.getDatabase().settings.getSettings(event.getGuild()).getGravelRole(event.getGuild());
            String confirmation;
            if(graveled!=null)
            {
                if(!event.getSelfMember().canInteract(graveled))
                {
                    event.replyError("I cannot interact with the existing '"+graveled.getName()+"' role. Please move my role(s) higher and then try again.");
                    return;
                }
                if(!event.getMember().canInteract(graveled))
                {
                    event.replyError("You do not have permission to interact with the existing '"+graveled.getName()+"' role.");
                    return;
                }
                confirmation = "This will modify the existing '"+graveled.getName()+"' role and assign it overrides in every channel.";
            }
            else
                confirmation = "This will create a role called 'Graveled' and assign it overrides in every channel.";
            waitForConfirmation(event, confirmation, () -> setUpGraveledRole(event, graveled));
        }
    }

    private void setUpMutedRole(CommandEvent event, Role role)
    {
        StringBuilder sb = new StringBuilder(event.getClient().getSuccess()+" Muted role setup started!\n");
        event.reply(sb + Constants.LOADING+" Initializing role...", m -> event.async(() -> 
        {
            try
            {
                Role mutedRole;
                if(role==null)
                {
                    mutedRole = event.getGuild().createRole().setName("Muted").setPermissions().setColor(1).complete();
                }
                else
                {
                    role.getManager().setPermissions().complete();
                    mutedRole = role;
                }
                sb.append(event.getClient().getSuccess()).append(" Role initialized!\n");
                m.editMessage(sb + Constants.LOADING+" Making Category overrides...").complete();
                PermissionOverride po;
                for(net.dv8tion.jda.api.entities.channel.concrete.Category cat: event.getGuild().getCategories())
                {
                    po = cat.getPermissionOverride(mutedRole);
                    Permission[] deniedMutePerms = {Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS, Permission.CREATE_PRIVATE_THREADS, Permission.MESSAGE_ADD_REACTION, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK};
                    if(po==null)
                        cat.upsertPermissionOverride(mutedRole).deny(deniedMutePerms).complete();
                    else
                        po.getManager().deny(deniedMutePerms).complete();
                }
                sb.append(event.getClient().getSuccess()).append(" Category overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Text Channel overrides...").complete();
                for(TextChannel tc: event.getGuild().getTextChannels())
                {
                    po = tc.getPermissionOverride(mutedRole);
                    Permission[] deniedMutePerms = {Permission.MESSAGE_SEND, Permission.CREATE_PUBLIC_THREADS, Permission.CREATE_PRIVATE_THREADS, Permission.MESSAGE_ADD_REACTION};
                    if(po==null)
                        tc.upsertPermissionOverride(mutedRole).deny(deniedMutePerms).complete();
                    else
                        po.getManager().deny(deniedMutePerms).complete();
                }
                sb.append(event.getClient().getSuccess()).append(" Text Channel overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Voice Channel overrides...").complete();
                for(VoiceChannel vc: event.getGuild().getVoiceChannels())
                {
                    po = vc.getPermissionOverride(mutedRole);
                    if(po==null)
                        vc.upsertPermissionOverride(mutedRole).deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                    else
                        po.getManager().deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                }
                m.editMessage(sb + event.getClient().getSuccess()+" Voice Channel overrides complete!\n\n" + event.getClient().getSuccess()+" Muted role setup has completed!").queue();
            }
            catch(Exception ex)
            {
                m.editMessage(sb + event.getClient().getError()+" An error occurred setting up the Muted role. Please check that I have the Administrator permission and that the Muted role is below my roles.").queue();
            }
        }));
    }

    private void setUpGraveledRole(CommandEvent event, Role role)
    {
        StringBuilder sb = new StringBuilder(event.getClient().getSuccess()+" Graveled role setup started!\n");
        event.reply(sb + Constants.LOADING+" Initializing role...", m -> event.async(() ->
        {
            try
            {
                Role graveledRole;
                if(role==null)
                {
                    graveledRole = event.getGuild().createRole().setName("Graveled").setPermissions().setColor(1).complete();
                }
                else
                {
                    role.getManager().setPermissions().complete();
                    graveledRole = role;
                }
                sb.append(event.getClient().getSuccess()).append(" Role initialized!\n");
                m.editMessage(sb + Constants.LOADING+" Making Category overrides...").complete();
                PermissionOverride po;
                for(net.dv8tion.jda.api.entities.channel.concrete.Category cat: event.getGuild().getCategories())
                {
                    po = cat.getPermissionOverride(graveledRole);
                    Permission[] deniedGravelPerms = {Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.VIEW_CHANNEL, Permission.CREATE_PRIVATE_THREADS, Permission.CREATE_PUBLIC_THREADS, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK};
                    if(po==null)
                        cat.upsertPermissionOverride(graveledRole).deny().complete();
                    else
                        po.getManager().deny(deniedGravelPerms).complete();
                }
                sb.append(event.getClient().getSuccess()).append(" Category overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Text Channel overrides...").complete();
                for(TextChannel tc: event.getGuild().getTextChannels())
                {
                    po = tc.getPermissionOverride(graveledRole);
                    Permission[] deniedGravelPerms = {Permission.MESSAGE_SEND, Permission.VIEW_CHANNEL, Permission.VIEW_CHANNEL, Permission.CREATE_PRIVATE_THREADS, Permission.CREATE_PUBLIC_THREADS};
                    if(po==null)
                        tc.upsertPermissionOverride(graveledRole).deny(deniedGravelPerms).complete();
                    else
                        po.getManager().deny(deniedGravelPerms).complete();
                }
                sb.append(event.getClient().getSuccess()).append(" Text Channel overrides complete!\n");
                m.editMessage(sb + Constants.LOADING + " Making Voice Channel overrides...").complete();
                for(VoiceChannel vc: event.getGuild().getVoiceChannels())
                {
                    po = vc.getPermissionOverride(graveledRole);
                    if(po==null)
                        vc.upsertPermissionOverride(graveledRole).deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                    else
                        po.getManager().deny(Permission.VOICE_CONNECT, Permission.VOICE_SPEAK).complete();
                }
                m.editMessage(sb + event.getClient().getSuccess()+" Voice Channel overrides complete!\n\n" + event.getClient().getSuccess()+" Graveled role setup has completed!").queue();
            }
            catch(Exception ex)
            {
                m.editMessage(sb + event.getClient().getError()+" An error occurred setting up the Graveled role. Please check that I have the Administrator permission and that the Gravel role is below my roles.").queue();
            }
        }));
    }

    private void waitForConfirmation(CommandEvent event, String message, Runnable confirm)
    {
        new ButtonMenu.Builder()
                .setChoices(CONFIRM, CANCEL)
                .setEventWaiter(vortex.getEventWaiter())
                .setTimeout(1, TimeUnit.MINUTES)
                .setText(event.getClient().getWarning()+" "+message+"\n\n"+CONFIRM+" Continue\n"+CANCEL+" Cancel")
                .setFinalAction(m -> m.delete().queue(s->{}, f->{}))
                .setUsers(event.getAuthor())
                .setAction(re ->
                {
                    if(re.getName().equals(CONFIRM))
                        confirm.run();
                }).build().display(event.getChannel());
    }
}