package io.github.guggle.utils;

import java.util.Map;
import java.util.HashMap;

public class TypeInstance {

    private static final ThreadLocal<Map<Class<?>, Object>> _tl = ThreadLocal.withInitial(() -> new HashMap<>());

    public static <T> T instance(final Class<T> type) {
        final Map<Class<?>,Object> map = _tl.get();
        Object found = map.get(type);
        if(found != null) {
            return type.cast(found);
        }
        else {
            try {
                T instance = type.newInstance();
                map.put(type, instance);
                return instance;
            }
            catch(InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
