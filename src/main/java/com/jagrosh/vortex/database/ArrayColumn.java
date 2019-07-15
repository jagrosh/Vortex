/*
 * Copyright 2017 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex.database;

import com.jagrosh.easysql.SQLColumn;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class ArrayColumn<T> extends SQLColumn<List<T>>
{
    public ArrayColumn(String name)
    {
        this(name, false);
    }

    public ArrayColumn(String name, boolean publicKey)
    {
        super(name, false, null, publicKey);
    }

    @Override
    public String getDataDescription()
    {
        return "ARRAY DEFAULT ()" + nullable() + (primaryKey ? " PRIMARY KEY" : "");
    }

    @Override
    public List<T> getValue(ResultSet results) throws SQLException
    {
        return new ArrayList<>(Arrays.asList((T[]) results.getArray(name).getArray()));
    }

    @Override
    public void updateValue(ResultSet results, List<T> newValue) throws SQLException
    {
        results.updateObject(name, newValue.toArray());
    }
}
