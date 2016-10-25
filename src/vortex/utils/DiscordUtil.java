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
package vortex.utils;

import java.util.function.Consumer;
import net.dv8tion.jda.core.requests.RestAction;
import net.dv8tion.jda.core.utils.SimpleLog;

/**
 *
 * @author John Grosh (jagrosh)
 */
public class DiscordUtil {
    private static final Consumer<Throwable> FAILURE = (t) -> SimpleLog.getLog("Queue").fatal(t);
    
    
    public static <T> void queueAndBlock(RestAction<T> action)
    {
        queueAndBlock(action,null,FAILURE);
    }
    
    public static <T> void queueAndBlock(RestAction<T> action, Consumer<T> success)
    {
        queueAndBlock(action,success,FAILURE);
    }
    
    public static <T> void queueAndBlock(RestAction<T> action, Consumer<T> success, Consumer<Throwable> failure)
    {
        System.out.println("flag0");
        Object lock = new Object();
        synchronized(lock)
        {
            action.queue((o) -> {
                synchronized(lock)
                {
                    System.out.println("flag4");
                    if(success!=null)
                        success.accept(o);
                    lock.notifyAll();
                }
            }, (t)->{
                synchronized(lock)
                {
                    System.out.println("flag3");
                    failure.accept(t);
                    lock.notifyAll();
                }
            });
            System.out.println("flag2");
            try{
                lock.wait();
            }catch(InterruptedException e){
                SimpleLog.getLog("Queue").fatal(e);
            }
        }
        System.out.println("flag1");
    }
}
