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
package com.jagrosh.vortex.pro;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Michael Ritter (Kantenkugel)
 */
public class VortexPro
{
    private static final Map<Class<?>, Object> IMPLS = new HashMap<>();

    @SuppressWarnings("unchecked")
    public static <T> T from(Class<T> apiClass, Object... args)
    {
        if(IMPLS.containsKey(apiClass))
        {
            return (T) IMPLS.get(apiClass);
        }
        ProFeature anno = apiClass.getAnnotation(ProFeature.class);
        if(anno == null)
        {
            throw new IllegalArgumentException("Class " + apiClass.getName() + " is not annotated with the " + ProFeature.class.getName() + " annotation!");
        }
        Class<? extends T> implClass = null;
        try
        {
            Class<?> tmpClass = Class.forName(anno.value());
            if(!apiClass.isAssignableFrom(tmpClass) || tmpClass.isInterface())
            {
                throw new IllegalStateException("Class " + tmpClass.getName() + " can't be converted to a instance of " + apiClass.getName());
            }
            implClass = (Class<? extends T>) tmpClass;
        }
        catch(ClassNotFoundException ignored) {}
        if(implClass == null && anno.mock() != Object.class)
        {
            if(!apiClass.isAssignableFrom(anno.mock()) || anno.mock().isInterface())
            {
                throw new IllegalStateException("Class " + anno.mock().getName() + " can't be converted to a instance of " + apiClass.getName());
            }
            implClass = (Class<? extends T>) anno.mock();
        }
        T instance = null;
        if(implClass != null)
        {
            instance = createInstance(implClass, args);
        }
        IMPLS.put(apiClass, instance);
        return instance;
    }

    private static <T> T createInstance(Class<? extends T> clazz, Object[] args)
    {
        @SuppressWarnings("unchecked")
        Set<Constructor<? extends T>> constructors = Arrays.stream(clazz.getConstructors())
                .filter(c -> c.getParameterCount() == 0 || c.getParameterCount() == args.length)
                .map(c -> (Constructor<? extends T>) c)
                .collect(Collectors.toSet());
        if(constructors.size() != 1)
        {
            throw new IllegalArgumentException("Could not find a (single) Constructor with 0 or matching amount of arguments. Arguments provided: " + Arrays.toString(args));
        }
        Constructor<? extends T> constructor = constructors.iterator().next();
        try
        {
            if(constructor.getParameterCount() == 0)
                return constructor.newInstance();
            else
                return constructor.newInstance(args);
        }
        catch(InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            throw new IllegalStateException("Can not instantiate class "+clazz.getName()+". Does it have a public no-arg constructor?", e);
        }
    }

    private VortexPro() {}
}
