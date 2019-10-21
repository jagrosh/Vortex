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

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.commands.CommandExceptionListener;
import com.jagrosh.vortex.database.managers.PunishmentManager;
import com.jagrosh.vortex.utils.FormatUtil;
import com.jagrosh.vortex.utils.OtherUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PunishmentCmd extends Command
{
    private final static String SETTING_STRIKES = "\n\nUsage: `"+Constants.PREFIX+"punishment <number> <action> [time]`\n"
            + "`<number>` - the number of strikes at which to perform the action\n"
            + "`<action>` - the action, such as `None`, `Kick`, `Mute`, `Softban`, or `Ban`\n"
            + "`[time]` - optional, the amount of time to keep the user muted or banned\n"
            + "Do not include <> nor [] in your commands!\n\n"
            + "The `"+Constants.PREFIX+"settings` command can be used to view current punishments.";
    private final Vortex vortex;
    
    public PunishmentCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "punishment";
        this.help = "sets strikes for a punishment";
        this.aliases = new String[]{"setstrikes","setstrike","punishments"};
        this.arguments = "<number> <action> [time]";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
    }

    @Override
    protected void execute(CommandEvent event)
    {
        String[] parts = event.getArgs().split("\\s+", 3);
        if(parts.length<2)
            throw new CommandExceptionListener.CommandErrorException("Please include a number of strikes and an action to perform!"+SETTING_STRIKES);
        int numstrikes;
        try
        {
            numstrikes = Integer.parseInt(parts[0]);
        }
        catch(NumberFormatException ex)
        {
            event.replyError("`"+parts[0]+"` is not a valid integer!"+SETTING_STRIKES);
            return;
        }
        if(numstrikes<1 || numstrikes>PunishmentManager.MAX_STRIKES)
        {
            event.replyError("`<numstrikes>` must be between 1 and "+PunishmentManager.MAX_STRIKES+"!"+SETTING_STRIKES);
            return;
        }
        
        if(!parts[1].equalsIgnoreCase("none") && vortex.getDatabase().actions.getAllPunishments(event.getGuild()).size()>=PunishmentManager.MAX_SET)
            throw new CommandExceptionListener.CommandErrorException("This server already has "+PunishmentManager.MAX_SET+" punishments set up; please remove some before adding more.");
        
        String successMessage;
        switch(parts[1].toLowerCase())
        {
            case "none":
            {
                vortex.getDatabase().actions.removeAction(event.getGuild(), numstrikes);
                successMessage = "No action will be taken on `"+numstrikes+"` strikes.";
                break;
            }
            case "tempmute":
            case "temp-mute":
            case "mute":
            {
                int minutes = parts.length>2 ? OtherUtil.parseTime(parts[2])/60 : 0;
                if(minutes<0)
                {
                    event.replyError("Temp-Mute time cannot be negative!");
                    return;
                }
                vortex.getDatabase().actions.setAction(event.getGuild(), numstrikes, Action.MUTE, minutes);
                successMessage = "Users will now be `muted` "+(minutes>0 ? "for "+FormatUtil.secondsToTime(minutes*60)+" " : "")+"upon reaching `"+numstrikes+"` strikes.";
                if(vortex.getDatabase().settings.getSettings(event.getGuild()).getMutedRole(event.getGuild())==null)
                    successMessage += event.getClient().getWarning()+" No muted role currently exists!";
                break;
            }
            case "kick":
            {
                vortex.getDatabase().actions.setAction(event.getGuild(), numstrikes, Action.KICK);
                successMessage = "Users will now be `kicked` upon reaching `"+numstrikes+"` strikes.";
                break;
            }
            case "softban":
            {
                vortex.getDatabase().actions.setAction(event.getGuild(), numstrikes, Action.SOFTBAN);
                successMessage = "Users will now be `softbanned` upon reaching `"+numstrikes+"` strikes.";
                break;
            }
            case "tempban":
            case "temp-ban":
            case "ban":
            {
                int minutes = parts.length>2 ? OtherUtil.parseTime(parts[2])/60 : 0;
                if(minutes<0)
                {
                    event.replyError("Temp-Ban time cannot be negative!");
                    return;
                }
                vortex.getDatabase().actions.setAction(event.getGuild(), numstrikes, Action.BAN, minutes);
                successMessage = "Users will now be `banned` "+(minutes>0 ? "for "+FormatUtil.secondsToTime(minutes*60)+" " : "")+"upon reaching `"+numstrikes+"` strikes.";
                break;
            }
            default:
            {
                event.replyError("`"+parts[1]+"` is not a valid action!"+SETTING_STRIKES);
                return;
            }
        }
        event.reply(new MessageBuilder()
                .append(event.getClient().getSuccess())
                .append(" ").append(FormatUtil.filterEveryone(successMessage))
                .setEmbed(new EmbedBuilder().setColor(event.getSelfMember().getColor())
                        .addField(vortex.getDatabase().actions.getAllPunishmentsDisplay(event.getGuild()))
                        .build())
                .build());
    }
}
