/*
 * Copyright 2021 John Grosh (john.a.grosh@gmail.com).
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

import com.jagrosh.vortex.automod.InviteResolver;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class InviteTest
{
    @Test
    public void inviteTest() 
    {
        InviteResolver ir = new InviteResolver();
        
        long validId = ir.resolve("0p9LSGoRLu6Pet0k", "");
        assertEquals(validId, 147698382092238848L);
        
        long invalidId = ir.resolve("discord-not-a-real-code", "");
        assertEquals(invalidId, 0L);
    }
}
