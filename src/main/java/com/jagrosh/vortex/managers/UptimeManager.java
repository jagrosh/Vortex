/*
 * Copyright 2023 jagrosh.
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
package com.jagrosh.vortex.managers;

import club.minnced.discord.webhook.WebhookClient;
import com.jagrosh.vortex.Constants;
import com.jagrosh.vortex.database.Database;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author jagrosh
 */
public class UptimeManager 
{
    private boolean started = false;
    private int failures = 0;
    private final int minPrem;
    private final ScheduledExecutorService threadpool;
    private final WebhookClient log;
    private final Database database;
    
    public UptimeManager(WebhookClient log, Database database, ScheduledExecutorService threadpool, int minPrem)
    {
        this.log = log;
        this.database = database;
        this.threadpool = threadpool;
        this.minPrem = minPrem;
    }
    
    public void start()
    {
        if(started)
            return;
        started = true;
        
        threadpool.scheduleWithFixedDelay(()->check(), 2, 2, TimeUnit.MINUTES);
    }
    
    private void check()
    {
        boolean valid = false;
        try
        {
            boolean closed = database.getConnection().isClosed();
            int prem = database.premium.getPremiumGuilds().size();
            valid = !closed && prem >= minPrem;
        }
        catch(Exception ignored){}
        
        if(!valid)
            failures++;
        else
            failures = 0;
        
        if(failures == 3)
            shutdownAll();
    }
    
    private void shutdownAll()
    {
        try
        {
            log.send(Constants.ERROR+" Uptime failure, attempting restart...").get();
            Thread.sleep(1000);
        }
        catch(Exception ignored){}
        System.exit(1);
    }
}
