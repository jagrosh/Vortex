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
package com.jagrosh.vortex.managers;

import net.dv8tion.jda.api.utils.ConcurrentSessionController;

/**
 *
 * @author John Grosh (john.a.grosh@gmail.com)
 */
public class SlowerConcurrentSessionController extends ConcurrentSessionController
{
    private final int delay;
    
    public SlowerConcurrentSessionController(int delay)
    {
        super();
        this.delay = delay;
    }
    
    @Override
    protected void runWorker()
    {
        synchronized (lock)
        {
            if (workerHandle == null)
            {
                workerHandle = new QueueWorker(delay);
                workerHandle.start();
            }
        }
    }
}
