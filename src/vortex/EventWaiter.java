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
package vortex;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import net.dv8tion.jda.core.events.Event;
import net.dv8tion.jda.core.hooks.EventListener;
import vortex.entities.WaitingEvent;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class EventWaiter implements EventListener {
    
    private final HashMap<Class, List<WaitingEvent>> waitingEvents;
    
    public EventWaiter()
    {
        waitingEvents = new HashMap<>();
    }
    
    public <T extends Event> void waitForEvent(Class<T> classType, Predicate<T> condition, Consumer<T> action)
    {
        List<WaitingEvent> list;
        if(waitingEvents.containsKey(classType))
            list = waitingEvents.get(classType);
        else
        {
            list = new ArrayList<>();
            waitingEvents.put(classType, list);
        }
        list.add(new WaitingEvent<>(condition, action));
    }
    
    @Override
    public final void onEvent(Event event)
    {
        if(waitingEvents.containsKey(event.getClass()))
        {
            List<WaitingEvent> list = waitingEvents.get(event.getClass());
            list.removeAll(list.stream().filter(i -> i.attempt(event)).collect(Collectors.toList()));
        }
    }
}
