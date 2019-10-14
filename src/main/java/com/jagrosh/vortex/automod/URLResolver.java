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
package com.jagrosh.vortex.automod;

import com.jagrosh.vortex.utils.FixedCache;
import com.jagrosh.vortex.utils.OtherUtil;
import com.typesafe.config.Config;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public interface URLResolver
{
    public List<String> findRedirects(String url);
    public void loadSafeDomains();
    
    public static class DummyURLResolver implements URLResolver
    {
        @Override
        public List<String> findRedirects(String url)
        {
            return Collections.EMPTY_LIST;
        }

        @Override
        public void loadSafeDomains() {}
    }
    
    public static class ActiveURLResolver implements URLResolver
    {
        private final String prefix, suffix, form;
        private final OkHttpClient client = new OkHttpClient.Builder().build();
        private final Request.Builder request;

        private final FixedCache<String,List<String>> cache = new FixedCache<>(1000);

        public ActiveURLResolver(Config config)
        {
            prefix = config.getString("url-resolver.prefix");
            suffix = config.getString("url-resolver.suffix");
            form = config.getString("url-resolver.form");
            request = new Request.Builder().url(config.getString("url-resolver.url"));
            config.getConfig("url-resolver.headers").entrySet()
                    .forEach(entry -> request.addHeader(entry.getKey(), (String) entry.getValue().unwrapped()));
        }

        @Override
        public synchronized List<String> findRedirects(String url)
        {
            if(isSafeDomain(url))
                return Collections.EMPTY_LIST;
            if(cache.contains(url))
                return cache.get(url);
            try
            {
                List<String> resolved = resolve(client
                        .newCall(request.post(new FormBody.Builder().add(form, url).build()).build())
                        .execute().body().string());
                cache.put(url, resolved);
                System.out.println("Link Resolving: "+url+" -> "+resolved);
                return resolved;
            }
            catch(Exception ex)
            {
                return Collections.EMPTY_LIST;
            }
        }

        private List<String> resolve(String text)
        {
            List<String> list = new LinkedList<>();
            boolean skip = true; // first link is always the link we gave
            for(int i=0; i<text.length(); )
            {
                i = text.indexOf(prefix, i);
                if(i==-1)
                    break;
                int first = i + prefix.length();
                i = text.indexOf(suffix,first);
                if(skip)
                    skip = false;
                else
                    list.add(text.substring(first,i).trim());
            }
            return list;
        }

        // These are domains frequently sent via Discord that do not redirect to external sites
        private String[] safeDomains = OtherUtil.readLines("safe_domains");

        private boolean isSafeDomain(String url)
        {
            // first, find the root domain
            String root = url.substring(url.indexOf("://")+3);
            int index = root.indexOf("/");
            if(index!=-1)
                root = root.substring(0, index);
            index = root.lastIndexOf(".");
            index = root.lastIndexOf(".", index-1);
            root = root.substring(index+1).toLowerCase();
            for(String str: safeDomains)
                if(str.equals(root))
                    return true;
            return false;
        }

        @Override
        public void loadSafeDomains()
        {
            this.safeDomains = OtherUtil.readLines("safe_domains");
        }
    }
}
