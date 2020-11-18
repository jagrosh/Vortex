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
import com.jagrosh.jdautilities.menu.ButtonMenu;
import com.jagrosh.vortex.Vortex;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.exceptions.PermissionException;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Michaili K (mysteriouscursor+git@protonmail.com)
 */
public class ResetCmd extends Command
{
    private final Vortex vortex;

    private static final String CONFIRM_RESET_EMOJI = "\u2705"; // ✅
    private static final String CANCEL_RESET_EMOJI = "\u274c";  // ❌

    public ResetCmd(Vortex vortex)
    {
        this.vortex = vortex;
        this.name = "reset";
        this.help = "reset vortex settings/data to default";
        this.arguments = "<automod|filters|settings|ignores|punishments|strikes|all>";
        this.category = new Category("Settings");
        this.guildOnly = true;
        this.userPermissions = new Permission[]{Permission.MANAGE_SERVER};
        this.cooldown = 60;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        if(event.getArgs().isEmpty())
        {
            event.replyError("Please include the section to reset");
            return;
        }

        ButtonMenu.Builder menuBuilder = new ButtonMenu.Builder()
                .addChoice(CONFIRM_RESET_EMOJI)
                .addChoice(CANCEL_RESET_EMOJI)
                .setUsers(event.getAuthor())
                .setTimeout(30, TimeUnit.SECONDS)
                .setEventWaiter(vortex.getEventWaiter())
                .setFinalAction(m -> {
                    try
                    {
                        m.clearReactions().queue();
                    }
                    catch (PermissionException ignored)
                    {
                    }
                });

        ThrowableConsumer<Guild> consumer;
        String description;

        switch (event.getArgs().toLowerCase())
        {
            case "automod":
                description = "**Reset Automod**\n" +
                        "You are about to reset Automod.\n" +
                        "This will result in all Automod settings to be disabled.\n" +
                        "Filters aren't affected by this reset.\n\n" +
                        "Are you sure you want to reset all Automod settings?";

                consumer = (guild) -> vortex.getDatabase().automod.reset(guild);
                break;


            case "filters":
                description = "**Reset Filters**\n" +
                        "You are about to remove all filters.\n" +
                        "Are you sure you want to remove all Filters?";
                consumer = (guild) -> vortex.getDatabase().filters.deleteAllFilters(guild.getIdLong());
                break;


            case "settings":
                description = "**Reset Settings**\n" +
                        "You are about to reset all Server Settings.\n" +
                        "This will result in raid mode being disabled & Server Settings like mod role & logs to be reset.\n" +
                        "Punishments, Automod, Ignored roles/channels & Filters aren't affected by this reset.\n" +
                        "Are you sure you want to reset all Server Settings?";
                consumer = (guild) -> vortex.getDatabase().settings.reset(guild);
                break;


            case "ignores":
                description = "**Reset Ignores**\n" +
                        "You are about to reset ignored channels & roles.\n" +
                        "This will result in AutoMod no longer ignoring any channels & roles added to the ignore list.\n" +
                        "Are you sure you want to reset all ignored channels & roles?";
                consumer = (guild) -> vortex.getDatabase().ignores.unignoreAll(guild);
                break;


            case "punishments":
                description = "**Reset Punishments**\n" +
                        "You are about to reset all punishments.\n" +
                        "This will result in all punishments being removed & as such, strikes no longer mute, kick or ban members.\n" +
                        "Are you sure you want to reset all punishments?";
                consumer = (guild) -> vortex.getDatabase().actions.removeAllActions(guild);
                break;

            case "strikes":
                description = "**Reset Strikes**\n" +
                        "You are about to reset **__all__** strikes.\n" +
                        "This will result in all strikes being pardoned from all members.\n" +
                        "No bans or mutes will be lifted.\n" +
                        "Are you sure you want to reset all strikes?";
                consumer = (guild) -> vortex.getDatabase().strikes.resetAllStrikes(guild);
                break;

            case "all":
                description = "**Reset All**\n" +
                        "You are about to reset **__all__** settings & strikes.\n" +
                        "This will result in Automod, settings, filters, punishments & strikes to be reset.\n" +
                        "No bans or mutes will be lifted.\n" +
                        "Are you sure you want to reset all settings & strikes?";
                consumer = (guild) ->
                {
                    vortex.getDatabase().automod.reset(guild);
                    vortex.getDatabase().filters.deleteAllFilters(guild.getIdLong());
                    vortex.getDatabase().settings.reset(guild);
                    vortex.getDatabase().ignores.unignoreAll(guild);
                    vortex.getDatabase().actions.removeAllActions(guild);
                    vortex.getDatabase().strikes.resetAllStrikes(guild);
                };
                break;

            default:
                event.replyError("Unknown section");
                return;
        }

        menuBuilder
                .setDescription(description)
                .setAction(reactionEmote ->
                {
                    if (reactionEmote.getName().equals(CONFIRM_RESET_EMOJI))
                    {
                        try
                        {
                            consumer.call(event.getGuild());
                            event.replySuccess("Reset successful");
                        }
                        catch (Exception e)
                        {
                            event.replyError("An error occurred while resetting");
                        }
                    }
                })
                .build()
                .display(event.getTextChannel());
    }

    private interface ThrowableConsumer<T>
    {
        void call(T t) throws Exception;
    }
}
