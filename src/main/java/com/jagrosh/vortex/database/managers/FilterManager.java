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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import net.dv8tion.jda.core.entities.Guild;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FilterManager extends DataManager
{
    public final static SQLColumn<Long> GUILD_ID = new LongColumn("GUILD_ID",false,0L);
    public final static SQLColumn<Integer> NUM = new IntegerColumn("NUM", false, 0);
    public final static SQLColumn<Integer> TYPE = new IntegerColumn("TYPE", false, 0);
    public final static SQLColumn<String> VALUE = new StringColumn("VALUE", false, "", 60);
    
    public FilterManager(DatabaseConnector connector)
    {
        super(connector, "FILTERS");
    }

    @Override
    protected String primaryKey()
    {
        return GUILD_ID+", "+NUM;
    }
    
    public int addFilter(Guild guild, Type type, String value)
    {
        return 0;
    }
    
    public Filter deleteFilter(Guild guild, int index)
    {
        return null;
    }
    
    public List<Filter> getFilters(Guild guild)
    {
        return null;
    }
    
    public class Filter
    {
        public final String value;
        public final Type type;
        
        private Filter(ResultSet rs) throws SQLException
        {
            this.value = VALUE.getValue(rs);
            this.type = Type.values()[TYPE.getValue(rs)];
        }
    }
    
    public enum Type
    {
        WORD, GLOB, REGEX
    }
}
