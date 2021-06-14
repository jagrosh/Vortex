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

import com.jagrosh.vortex.automod.URLResolver;
import com.typesafe.config.ConfigFactory;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class RedirectTest
{
    @Test
    public void redirectTest()
    {
        URLResolver resolver = new URLResolver.ActiveURLResolver(ConfigFactory.load());
        List<String> redir1 = resolver.findRedirects("https://tinyurl.com/yggtreehouse");
        System.out.println(redir1);
        assertFalse(redir1.isEmpty());
        List<String> redir2 = resolver.findRedirects("https://bit.ly/3xmJMoU");
        System.out.println(redir2);
        assertFalse(redir2.isEmpty());
        List<String> redir3 = resolver.findRedirects("https://shorturl.at/gyG16");
        System.out.println(redir3);
        assertFalse(redir3.isEmpty());
    }
}
