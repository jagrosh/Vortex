/*
 * Copyright 2019 John Grosh (john.a.grosh@gmail.com).
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
package com.jagrosh.vortex;

import com.jagrosh.vortex.automod.Filter.Glob;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class FilterTest
{
    @Test
    public void globTest()
    {
        Glob glob;
        
        assertTrue('0' < '9');
        
        glob = new Glob("get");
        assertFalse(glob.test("go together now"));
        assertFalse(glob.test("go to gether now"));
        assertFalse(glob.test("go toget her now"));
        assertTrue(glob.test("go to get her now"));
        
        glob = new Glob("*get");
        assertFalse(glob.test("go together now"));
        assertFalse(glob.test("go to gether now"));
        assertTrue(glob.test("go toget her now"));
        assertTrue(glob.test("go to get her now"));
        
        glob = new Glob("get*");
        assertFalse(glob.test("go together now"));
        assertTrue(glob.test("go to gether now"));
        assertFalse(glob.test("go toget her now"));
        assertTrue(glob.test("go to get her now"));
        
        glob = new Glob("*get*");
        assertTrue(glob.test("go together now"));
        assertTrue(glob.test("go to gether now"));
        assertTrue(glob.test("go toget her now"));
        assertTrue(glob.test("go to get her now"));
        
        glob = new Glob("get");
        assertTrue(glob.test("I for get"));
        assertFalse(glob.test("I forget"));
        
        glob = new Glob("get");
        assertTrue(glob.test("get the food"));
        assertFalse(glob.test("getthe food"));
    }
}
