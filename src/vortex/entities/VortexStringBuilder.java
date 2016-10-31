/*
 * Copyright 2016 John Grosh (jagrosh).
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
package vortex.entities;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class VortexStringBuilder {
    private final StringBuilder builder;
    private final AtomicInteger counter;
    private final int maximum;
    private final Consumer<String> action;
    
    public VortexStringBuilder(int maximum, Consumer<String> action)
    {
        this("",maximum,action);
    }
    
    public VortexStringBuilder(String initial, int maximum, Consumer<String> action)
    {
        this.counter = new AtomicInteger(0);
        this.builder = new StringBuilder(initial);
        this.maximum = maximum;
        this.action = action;
    }
    
    public VortexStringBuilder append(Object o)
    {
        builder.append(o);
        return this;
    }
    
    public void increment()
    {
        if(counter.incrementAndGet()>=maximum)
            action.accept(toString().trim());
    }
    
    @Override
    public String toString()
    {
        return builder.toString();
    }
}
