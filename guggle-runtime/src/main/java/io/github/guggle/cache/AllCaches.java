package io.github.guggle.cache;

import io.github.guggle.api.*;
import io.github.guggle.utils.TimeUnits;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.*;
import java.util.stream.*;

public class AllCaches implements CacheRegistry {

    private static final AllCaches instance = new AllCaches();
    public static CacheRegistry instance() {
        return instance;
    }

    private static final int CORES = Runtime.getRuntime().availableProcessors();

    private final Instant start = Instant.now();
    private final Map<MethodId, View<?,?>> _all = new HashMap<>();
    private final Map<MethodId, View<?,?>> _configs = new HashMap<>();
    private final ConcurrentMap<Object,CompletableFuture<Expiration>> inFlight = new ConcurrentHashMap<>(64, 0.65f, 4);
    private final Lock configLock = new ReentrantLock();

    
    private volatile ConcurrentMap<Object,Object> defaultBacking = new ConcurrentHashMap<>(4_096, 0.65f, 4);
    private volatile ExecutorService workerPool;
    private volatile ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private volatile TimeUnits expirationInterval = TimeUnits.minutes(1L);
    
    private AllCaches() {
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.max(CORES / 4, 1), CORES,
                                                        30L, TimeUnit.SECONDS,
                                                        new ArrayBlockingQueue<>(CORES * 4),
                                                        new ThreadPoolExecutor.CallerRunsPolicy());
        tpe.allowCoreThreadTimeOut(true);
        workerPool = tpe;
    }

    private <T> T withConfigLock(final BiFunction<Map<MethodId, View<?,?>>, Map<MethodId, View<?,?>>, T> function) {
        configLock.lock();
        try {
            return function.apply(_all, _configs);
        }
        finally {
            configLock.unlock();
        }
    }

    private void withConfigLock(final BiConsumer<Map<MethodId, View<?,?>>, Map<MethodId, View<?,?>>> consumer) {
        configLock.lock();
        try {
            consumer.accept(_all, _configs);
        }
        finally {
            configLock.unlock();
        }
    }

    private boolean instanceIn(final List<ConcurrentMap<Object,Object>> soFar, final ConcurrentMap<Object,Object> map) {
        for(int i = 0; i < soFar.size(); ++i) {
            if(soFar.get(i) == map) {
                return true;
            }
        }

        return false;
    }
    
    private void allBacking(final Map<MethodId, View<?,?>> all, final List<ConcurrentMap<Object,Object>> ret) {
        ret.add(defaultBacking);
        
        for(Map.Entry<MethodId, View<?,?>> entry : all.entrySet()) {
            if(!instanceIn(ret, entry.getValue().backing)) {
                ret.add(entry.getValue().backing);
            }
        }
    }
    
    private void classViews(final Map<MethodId, View<?,?>> all, final Map<Class<?>, View<?,?>> ret) {
        for(View<?,?> view : all.values()) {
            ret.put(view.type, view);
        }
    }

    private void scheduleNextExpiration() {
        scheduler.schedule(this::expireNeeded, expirationInterval.getInterval(), expirationInterval.getTimeUnit());
    }
    
    private void expireNeeded() {
        workerPool.submit(() -> {
                try {
                    final Instant now = Instant.now();
                    final List<ConcurrentMap<Object,Object>> listBacking = new ArrayList<>();
                    final Map<Class<?>, View<?,?>> mapClassViews = new HashMap<>();
                    
                    withConfigLock((all, configs) -> {
                            allBacking(all, listBacking);
                            classViews(all, mapClassViews);
                        });

                    for(ConcurrentMap<Object,Object> backing : listBacking) {
                        Set<Object> toRemove = new HashSet<>();
                        backing.entrySet().stream().forEach((e) -> {
                                final Object key = e.getKey();
                                Expiration exp = (Expiration) e.getValue();
                                View<?,?> view = mapClassViews.get(key.getClass());
                                if(exp.expired(view.lifetime.getExpires(), view.lifetime.getUnits(), now, start)) {
                                    if(view.lifetime.getRefresh() == Refresh.ON_DEMAND ||
                                       view.lifetime.getRefresh() == Refresh.NONE) {
                                        toRemove.add(key);
                                    }

                                    if(view.lifetime.getRefresh() == Refresh.EAGER) {
                                        workerPool.submit(() -> {
                                                final Object myKey = key;
                                                view.untypedGenerate(myKey);
                                            });
                                    }
                                }
                            });

                        for(Object key : toRemove) {
                            backing.remove(key);
                        }
                    }
                }
                finally {
                    scheduleNextExpiration();
                }
            });
    }

    private class View<K extends Permanent<K>, V extends Expiration> implements KeyView<K> {
        protected final Class<K> type;
        protected volatile ConcurrentMap<Object,Object> backing;
        protected volatile Lifetime lifetime;

        public View(final Class<K> type, final ConcurrentMap<Object,Object> backing, final Lifetime lifetime) {
            this.type = type;
            this.backing = backing;
            this.lifetime = lifetime;
        }

        public boolean contains(final K key) {
            return backing.containsKey(key);
        }

        public void remove(final K key) {
            backing.remove(key);
        }

        protected void removeAll() {
            Set<K> toRemove = keys().collect(Collectors.toSet());
            for(K k : toRemove) {
                backing.remove(k);
            }
        }

        protected View<K,V> copy() {
            return new View(type, backing, lifetime);
        }

        public void dirty(final K key) {
            final Expiration val = (Expiration) backing.get(key);
            if(val != null) {
                val.dirty();
            }
        }

        public Stream<K> keys() {
            return backing.keySet().stream().filter(type::isInstance).map(type::cast);
        }

        public long size() {
            return backing.keySet().stream().filter(type::isInstance).count();
        }

        protected Expiration untypedGenerate(final Object o) {
            throw new UnsupportedOperationException();
        }

        protected Expiration generate(final K key) {
            throw new UnsupportedOperationException();
        }

        protected Expiration extract(final CompletableFuture<Expiration> future) {
            try {
                return future.get();
            }
            catch(InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        }

        protected Expiration doInFlight(final K permanent,
                                            final CompletableFuture<Expiration> future,
                                            final Supplier<Expiration> supplier) {
            try {
                Expiration holder = supplier.get();
                future.complete(holder);
                backing.put(permanent, holder);
                return holder;
            }
            finally {
                inFlight.remove(permanent);
            }
        }
    }

    private class IntView<K extends Permanent<K>> extends View<K,Expiration.IntHolder> implements IntCacheView<K> {
        final ToIntFunction<K> function;
        
        public IntView(final Class<K> type, final ConcurrentMap<Object,Object> backing,
                       final Lifetime lifetime, final ToIntFunction<K> function) {
            super(type, backing, lifetime);
            this.function = function;
        }

        public Expiration.IntHolder value(final K key) {
            Expiration.IntHolder holder = (Expiration.IntHolder) backing.get(key);
            return (holder != null) ? holder : generate(key);
        }
        
        public Holder get(final K key) {
            return (Expiration.IntHolder) backing.get(key);
        }
        
        public void put(final K key, final int val) {
            backing.put(key.permanent(), Expiration.intHolder(start, val));
        }
        
        public IntStream values() {
            return backing.entrySet().stream()
                .filter(type::isInstance)
                .mapToInt((e) -> ((Expiration.IntHolder) e.getValue()).intValue());
        }

        @Override
        protected Expiration.IntHolder untypedGenerate(final Object o) {
            return generate(type.cast(o));
        }

        @Override
        protected Expiration.IntHolder generate(final K key) {
            final K permanent = key.permanent();
            final CompletableFuture<Expiration> attempt = new CompletableFuture<>();
            final CompletableFuture<Expiration> future = inFlight.computeIfAbsent(permanent, (tmp) -> attempt);

            return (Expiration.IntHolder) ((future == attempt) ?
                                           doInFlight(permanent, future, () -> Expiration.intHolder(start, function.applyAsInt(key))) :
                                           extract(future));
        }
    }
        
    public Lifetime lifetime(final MethodId methodId) {
        return withConfigLock((all, configs) -> {
                if(all.containsKey(methodId)) {
                    return all.get(methodId).lifetime;
                }

                if(configs.containsKey(methodId)) {
                    return all.get(methodId).lifetime;
                }

                return null;
            });
    }
    
    public void lifetime(final MethodId methodId, final Lifetime val) {
        withConfigLock((all, configs) -> {
                if(all.containsKey(methodId)) {
                    View<?,?> v = all.get(methodId);
                    v.lifetime = v.lifetime.toBuilder().merge(val).resolveNulls().build();
                }
                else if(configs.containsKey(methodId)) {
                    View<?,?> v = configs.get(methodId);
                    v.lifetime = v.lifetime.toBuilder().merge(val).build();
                }
                else {
                    configs.put(methodId, new View(null, defaultBacking, val));
                }
            });
                
    }

    public ConcurrentMap<Object,Object> backing(final MethodId methodId) {
        return withConfigLock((all, configs) -> {
                final View v = all.get(methodId);
                return v == null ? null : v.backing;
            });
    }

    public void backing(final MethodId methodId, final ConcurrentMap<Object,Object> val) {
        withConfigLock((all, configs) -> {
                if(all.containsKey(methodId)) {
                    final View<?,?> v = all.get(methodId);
                    final View<?,?> previous = v.copy();
                    v.backing = val;
                    previous.removeAll();
                }
                else if(configs.containsKey(methodId)) {
                    final View<?,?> v = configs.get(methodId);
                    v.backing = val;
                }
                else {
                    configs.put(methodId, new View(null, val, Lifetime.builder().build()));
                }
            });
    }

    public void backing(final ConcurrentMap<Object,Object> val) {
        withConfigLock((all, configs) -> {
                for(View<?,?> v : all.values()) {
                    if(v.backing == defaultBacking) {
                        final View<?,?> previous = v.copy();
                        v.backing = val;
                        previous.removeAll();
                    }
                }

                for(View<?,?> v : configs.values()) {
                    if(v.backing == defaultBacking) {
                        v.backing = val;
                    }
                }

                defaultBacking = val;
            });
    }
    
    public <K extends Permanent<K>> DoubleCacheView<K> doubleView(Class<K> keyType, MethodId methodId, ToDoubleFunction<K> func, Lifetime lifetime) {
        throw new UnsupportedOperationException();
    }

    public <K extends Permanent<K>> IntCacheView<K> intView(Class<K> keyType, MethodId methodId, ToIntFunction<K> func, Lifetime lifetime) {
        return withConfigLock((all,configs) -> {
                if(all.containsKey(methodId)) {
                    return (IntView<K>) all.get(methodId);
                }

                Lifetime actual = lifetime;
                ConcurrentMap<Object,Object> backing = null;
                if(configs.containsKey(methodId)) {
                    View<?,?> v = configs.get(methodId);
                    actual = actual.toBuilder().merge(v.lifetime).resolveNulls().build();
                    backing = v.backing;
                }

                IntView ret = new IntView(keyType, backing == null ? defaultBacking : backing, actual, func);
                all.put(methodId, ret);
                return ret;
            });
    }

    public <K extends Permanent<K>> LongCacheView<K> longView(Class<K> keyType, MethodId methodId, ToLongFunction<K> func, Lifetime lifetime) {
        throw new UnsupportedOperationException();
    }

    public <K extends Permanent<K>, V> ObjectCacheView<K,V> objectView(Class<K> keyType, MethodId methodId, Function<K,V> func, Lifetime lifetime) {
        throw new UnsupportedOperationException();
    }
    
}
