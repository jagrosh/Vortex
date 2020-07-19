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
package com.jagrosh.vortex.database.managers;

import com.jagrosh.easysql.DataManager;
import com.jagrosh.easysql.DatabaseConnector;
import com.jagrosh.easysql.SQLColumn;
import com.jagrosh.easysql.columns.*;
import com.jagrosh.vortex.Action;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.utils.FormatUtil;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageEmbed.Field;
import org.json.JSONObject;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class PunishmentManager extends DataManager
{
    public static final int MAX_STRIKES = 100;
    public static final int MAX_SET = 20;
    private static final String STRIKES_TITLE = Action.STRIKE.getEmoji()+" Punishments";
    
    
    public static final String DEFAULT_SETUP_MESSAGE = "\n" + Constants.WARNING + " It looks like you've set up some automoderation without assigning any punishments! "
                                                     + "I've gone ahead and set up some default punishments; you can see the settings with `" + Constants.PREFIX 
                                                     + "settings` and set or change any punishments with the `" + Constants.PREFIX+"punishment` command!";
    private static final int[] DEFAULT_STRIKE_COUNTS = {2,               3,               4,           5,              6};
    private static final Action[] DEFAULT_ACTIONS =    {Action.TEMPMUTE, Action.TEMPMUTE, Action.KICK, Action.TEMPBAN, Action.BAN};
    private static final int[] DEFAULT_TIMES =         {10,              120,             0,           1440,           0};
    
    public static final SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID", false, 0L);
    public static final SQLColumn<Integer> NUM_STRIKES = new IntegerColumn("NUM_STRIKES", false, 0);
    public static final SQLColumn<Integer> ACTION = new IntegerColumn("ACTION", false, 0);
    public static final SQLColumn<Integer> TIME = new IntegerColumn("TIME", false, 0);
    
    public PunishmentManager(DatabaseConnector connector)
    {
        super(connector, "ACTIONS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+NUM_STRIKES;
    }
    
    public boolean useDefaultSettings(Guild guild) // only activates if none set
    {
        return readWrite(selectAll(GUILD_ID.is(guild.getId())), rs -> 
        {
            if(rs.next())
                return false;
            for(int i=0; i<DEFAULT_STRIKE_COUNTS.length; i++)
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                NUM_STRIKES.updateValue(rs, DEFAULT_STRIKE_COUNTS[i]);
                ACTION.updateValue(rs, DEFAULT_ACTIONS[i].getBit());
                TIME.updateValue(rs, DEFAULT_TIMES[i]);
                rs.insertRow();
            }
            return true;
        });
    }
    
    public void removeAction(Guild guild, int numStrikes)
    {
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+NUM_STRIKES.is(numStrikes)), rs -> 
        {
            if(rs.next())
                rs.deleteRow();
        });
    }
    
    public void setAction(Guild guild, int numStrikes, Action action)
    {
        setAction(guild, numStrikes, action, 0);
    }
    
    public void setAction(Guild guild, int numStrikes, Action action, int time)
    {
        Action act;
        if(action==Action.MUTE && time>0)
            act = Action.TEMPMUTE;
        else if(action==Action.TEMPMUTE && time==0)
            act = Action.MUTE;
        else if(action==Action.BAN && time>0)
            act = Action.TEMPBAN;
        else if(action==Action.TEMPBAN && time==0)
            act = Action.BAN;
        else
            act = action;
        readWrite(selectAll(GUILD_ID.is(guild.getId())+" AND "+NUM_STRIKES.is(numStrikes)), rs -> 
        {
            if(rs.next())
            {
                ACTION.updateValue(rs, act.getBit());
                TIME.updateValue(rs, time);
                rs.updateRow();
            }
            else
            {
                rs.moveToInsertRow();
                GUILD_ID.updateValue(rs, guild.getIdLong());
                NUM_STRIKES.updateValue(rs, numStrikes);
                ACTION.updateValue(rs, act.getBit());
                TIME.updateValue(rs, time);
                rs.insertRow();
            }
        });
    }
    
    public List<Punishment> getAllPunishments(Guild guild)
    {
        return read(selectAll(GUILD_ID.is(guild.getId())), rs -> 
        {
            List<Punishment> list = new LinkedList<>();
            while(rs.next())
                list.add(new Punishment(rs));
            return list;
        });
    }
    
    public List<Punishment> getPunishments(Guild guild, int from, int to)
    {
        List<Punishment> punishments = getAllPunishments(guild);
        if(punishments.isEmpty())
            return punishments;
        List<Punishment> filtered = punishments.stream().filter(p -> p.numStrikes>from && p.numStrikes<=to).collect(Collectors.toList());
        if(!filtered.isEmpty())
            return filtered;
        Punishment max = punishments.get(0);
        for(Punishment p: punishments)
            if(p.numStrikes>max.numStrikes)
                max = p;
        if(from>=max.numStrikes)
            return Collections.singletonList(max);
        return Collections.EMPTY_LIST;
    }
    
    public Field getAllPunishmentsDisplay(Guild guild)
    {
        List<Punishment> all = getAllPunishments(guild);
        if(all.isEmpty())
            return new Field(STRIKES_TITLE, "No strikes set!", true);
        all.sort((a,b) -> a.numStrikes-b.numStrikes);
        StringBuilder sb = new StringBuilder();
        all.forEach(p -> sb.append("\n`").append(p.numStrikes).append(" ").append(Action.STRIKE.getEmoji()).append("`: **")
                .append(FormatUtil.capitalize(p.action.name())).append("** ").append(p.action.getEmoji())
                .append(p.time>0 ? " "+FormatUtil.secondsToTimeCompact(p.time*60) : ""));
        return new Field(STRIKES_TITLE, sb.toString().trim(), true);
    }
    
    public JSONObject getAllPunishmentsJson(Guild guild)
    {
        JSONObject obj = new JSONObject();
        getAllPunishments(guild).forEach(p -> obj.put(Integer.toString(p.numStrikes), 
                new JSONObject().put("action", p.action.toString()).put("time", p.time)));
        return obj;
    }
    
    public class Punishment
    {
        public final Action action;
        public final int numStrikes;
        public final int time;
        
        private Punishment(ResultSet rs) throws SQLException
        {
            this.action = Action.fromBit(ACTION.getValue(rs));
            this.numStrikes = NUM_STRIKES.getValue(rs);
            this.time = TIME.getValue(rs);
        }
    }
}
