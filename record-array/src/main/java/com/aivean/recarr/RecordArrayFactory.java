package com.aivean.recarr;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author <a href="mailto:aiveeen@gmail.com">Ivan Zaitsev</a>
 * 2021-11-02
 */
final class RecordArrayFactory {

    static final Method factory;

    static {
        Method f;
        try {
            f = Class.forName("com.aivean.recarr.RecordArrayFactoryImpl").getDeclaredMethod("create",
                    Class.class, int[].class);
        } catch (NoSuchMethodException | ClassNotFoundException e) {
            throw new RuntimeException("Cannot initialize RecordArrayFactory; " +
                    "Is annotation processing enabled?", e);
        }
        factory = f;
    }

    @SuppressWarnings("unchecked")
    static <T> RecordArray<T> create(Class<T> clazz, int ... dimensions) {
        try {
            return (RecordArray<T>) factory.invoke(null, clazz, dimensions);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Cannot create RecordArray<" + clazz.getSimpleName() + ">; " +
                    "Is annotation processing enabled?", e);
        }
    }
}
