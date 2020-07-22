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
package com.jagrosh.vortex.commands.tools;

import com.jagrosh.jdautilities.command.Command;
import com.jagrosh.jdautilities.command.CommandEvent;
import com.jagrosh.vortex.Vortex;
import com.jagrosh.vortex.database.Database;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
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
        this.help = "exports all server data as json";
        this.category = new Category("Tools");
        this.botPermissions = new Permission[]{Permission.MESSAGE_ATTACH_FILES};
        this.userPermissions = new Permission[]{Permission.ADMINISTRATOR};
        this.cooldown = 60*30;
        this.cooldownScope = CooldownScope.GUILD;
        this.guildOnly = true;
    }

    @Override
    protected void execute(CommandEvent event)
    {
        Guild g = event.getGuild();
        Database db = vortex.getDatabase();
        JSONObject obj = new JSONObject()
                .put("automod", db.automod.getSettingsJson(g))
                .put("settings", db.settings.getSettingsJson(g))
                .put("ignores", db.ignores.getIgnoresJson(g))
                .put("strikes", db.strikes.getAllStrikesJson(g))
                .put("punishments", db.actions.getAllPunishmentsJson(g))
                .put("tempmutes", db.tempmutes.getAllMutesJson(g))
                .put("tempbans", db.tempbans.getAllBansJson(g))
                .put("inviteWhitelist", db.inviteWhitelist.getWhitelistJson(g))
                .put("filters", db.filters.getFiltersJson(g))
                .put("premium", db.premium.getPremiumInfoJson(g));
        event.getChannel().sendFile(obj.toString(1).getBytes(), "vortex_data_" + g.getId() + ".json").queue();
    }
}
