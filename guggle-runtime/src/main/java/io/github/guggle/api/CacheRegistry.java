package io.github.guggle.api;

import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.function.Function;
import java.util.concurrent.ConcurrentMap;
import io.github.guggle.cache.AllCaches;

public interface CacheRegistry {

    Lifetime lifetime(MethodId methodId);

    void lifetime(MethodId methodId, Lifetime val);

    ConcurrentMap<Object,Object> backing(MethodId methodId);

    void backing(MethodId methodId, ConcurrentMap<Object,Object> val);

    void backing(ConcurrentMap<Object,Object> val);
    
    public <K extends Permanent<K>> DoubleCacheView<K> doubleView(Class<K> keyType, MethodId methodId, ToDoubleFunction<K> func, Lifetime lifetime);

    public <K extends Permanent<K>> IntCacheView<K> intView(Class<K> keyType, MethodId methodId, ToIntFunction<K> func, Lifetime lifetime);

    public <K extends Permanent<K>> LongCacheView<K> longView(Class<K> keyType, MethodId methodId, ToLongFunction<K> func, Lifetime lifetime);

    public <K extends Permanent<K>, V> ObjectCacheView<K,V> objectView(Class<K> keyType, Class<V> valueType, MethodId methodId, Function<K,V> func, Lifetime lifetime);

    public static CacheRegistry instance() {
        return AllCaches.instance();
    }
}
