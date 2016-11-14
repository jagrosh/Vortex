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

import java.util.function.Consumer;
import java.util.function.Predicate;
import net.dv8tion.jda.core.events.Event;

/**
 *
 * @author John Grosh (jagrosh)
 * @param <T> the type of event
 */
public class WaitingEvent<T extends Event> {
    final private Predicate<T> condition;
    final private Consumer<T> action;
    
    public WaitingEvent(Predicate<T> condition, Consumer<T> action)
    {
        this.condition = condition;
        this.action = action;
    }
    
    public boolean attempt(T event)
    {
        if(condition.test(event))
        {
            action.accept(event);
            return true;
        }
        return false;
    }
}
