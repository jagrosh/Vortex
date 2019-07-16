package com.jagrosh.vortex.pro;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        if(implClass == null)
        {
            if(anno.mock() != Object.class)
            {
                if(!apiClass.isAssignableFrom(anno.mock()) || anno.mock().isInterface())
                {
                    throw new IllegalStateException("Class " + anno.mock().getName() + " can't be converted to a instance of " + apiClass.getName());
                }
                implClass = (Class<? extends T>) anno.mock();
            }
        }
        try
        {
            T instance = null;
            if(implClass != null)
            {
                Set<Constructor<? extends T>> constructors = Arrays.stream(implClass.getConstructors())
                        .filter(c -> c.getParameterCount() == args.length)
                        .map(c -> (Constructor<? extends T>) c)
                        .collect(Collectors.toSet());
                if(constructors.size() != 1)
                {
                    throw new IllegalArgumentException("Could not find a Constructor with matching amount of arguments. Arguments provided: " + Arrays.toString(args));
                }
                instance = constructors.iterator().next().newInstance(args);
            }
            IMPLS.put(apiClass, instance);
            return instance;
        }
        catch(InvocationTargetException | InstantiationException | IllegalAccessException e)
        {
            throw new IllegalStateException("Can not instanciate class "+implClass.getName()+". Does it have a public no-arg constructor?", e);
        }
    }

    private VortexPro() {}
}
