package io.github.guggle.cache;

import io.github.guggle.api.*;
import io.github.guggle.utils.TimeUnits;
import io.github.guggle.utils.NamedThread;
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

    public static final Instant EPOCH = Instant.now();
    public static final long EPOCH_MILLIS = EPOCH.toEpochMilli();
    
    public static int sinceEpoch() {
        return (int) ((Instant.now().toEpochMilli() - EPOCH_MILLIS) / 1_000L);
    }

    public static Expired expired(final int since, final TimeUnits timeUnits, final Instant asOf) {
        final Instant toTest = Instant.ofEpochMilli(EPOCH_MILLIS + (1_000L * (long) since) + timeUnits.toMillis());
        return toTest.isBefore(asOf) ? Expired.TRUE : Expired.FALSE;
    }
    
    private static final AllCaches instance = new AllCaches();

    public static CacheRegistry instance() {
        return instance;
    }
    
    private final Map<MethodId, View<?,?>> _all = new HashMap<>();
    private final Map<MethodId, View<?,?>> _configs = new HashMap<>();
    private final ConcurrentMap<Object,CompletableFuture<Expiration>> inFlight = new ConcurrentHashMap<>(64, 0.65f, 4);
    private final Lock configLock = new ReentrantLock();
    
    private volatile ConcurrentMap<Object,Object> defaultBacking = new ConcurrentHashMap<>(4_096, 0.65f, 4);
    private volatile ExecutorService workerPool;
    private volatile ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new NamedThread("Cache Scheduler", true));
    private volatile TimeUnits expirationInterval = TimeUnits.minutes(1L);
    
    private AllCaches() {
        final int cores = Runtime.getRuntime().availableProcessors();
        ThreadPoolExecutor tpe = new ThreadPoolExecutor(Math.max(cores / 4, 1), cores,
                                                        30L, TimeUnit.SECONDS,
                                                        new ArrayBlockingQueue<>(cores * 4),
                                                        new NamedThread("Cache Worker", true),
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
            ret.put(view.keyType, view);
        }
    }

    public TimeUnits getExpirationInterval() {
        return expirationInterval;
    }

    public void setExpirationInterval(final TimeUnits val) {
        this.expirationInterval = val;
        scheduler.shutdownNow();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduleNextExpiration();
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
                        backing.entrySet().stream().forEach((e) -> {
                                final Object key = e.getKey();
                                Expiration exp = (Expiration) e.getValue();
                                View<?,?> view = mapClassViews.get(key.getClass());
                                Expired expired = exp.expired(view.lifetime.getExpires(), view.lifetime.getUnits(), now);
                                
                                if(expired == Expired.TRUE) {
                                    if(view.lifetime.getRefresh() == Refresh.ON_DEMAND ||
                                       view.lifetime.getRefresh() == Refresh.NONE) {
                                        backing.remove(key);
                                    }
                                    
                                    if(view.lifetime.getRefresh() == Refresh.EAGER) {
                                        workerPool.submit(() -> {
                                                final Object myKey = key;
                                                view.untypedGenerate(myKey);
                                            });
                                    }
                                }
                                else if(expired == Expired.UNKNOWN) {
                                    backing.put(key, Expiration.convert(view.lifetime.getExpires(), exp));
                                } });
                    }
                }
                finally {
                    scheduleNextExpiration();
                }
            });
    }

    private class View<K extends Permanent<K>, V extends Expiration> implements KeyView<K> {
        protected final Class<K> keyType;
        protected volatile ConcurrentMap<Object,Object> backing;
        protected volatile Lifetime lifetime;

        public View(final Class<K> keyType, final ConcurrentMap<Object,Object> backing, final Lifetime lifetime) {
            this.keyType = keyType;
            this.backing = backing;
            this.lifetime = lifetime;
        }

        public boolean contains(final K key) {
            return backing.containsKey(key);
        }

        public void remove(final K key) {
            backing.remove(key);
        }

        public void clear() {
            final Set<K> toRemove = keys().collect(Collectors.toSet());
            for(K k : toRemove) {
                backing.remove(k);
            }
        }

        protected View<K,V> copy() {
            return new View<>(keyType, backing, lifetime);
        }

        public void dirty(final K key) {
            final Expiration val = (Expiration) backing.get(key);
            if(val != null) {
                backing.remove(key);
                if(lifetime.getRefresh() == Refresh.EAGER) {
                    generate(key);
                }
            }
        }

        public Stream<K> keys() {
            return backing.keySet().stream().filter(keyType::isInstance).map(keyType::cast);
        }

        public long size() {
            return backing.keySet().stream().filter(keyType::isInstance).count();
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

    private class IntView<K extends Permanent<K>> extends View<K,Expiration> implements IntCacheView<K> {
        private final ToIntFunction<K> function;
        
        public IntView(final Class<K> keyType, final ConcurrentMap<Object,Object> backing,
                       final Lifetime lifetime, final ToIntFunction<K> function) {
            super(keyType, backing, lifetime);
            this.function = function;
        }

        public int value(final K key) {
            final IntHolder holder = (IntHolder) backing.get(key);
            if(holder != null) {
                return holder.value();
            }
            else {
                return ((IntHolder) generate(key)).value();
            }
        }
        
        public int get(final K key) {
            final IntHolder holder = (IntHolder) backing.get(key);
            return (holder == null) ? 0 : holder.value();
        }
        
        public void put(final K key, final int val) {
            backing.put(key.permanent(), Expiration.forInt(lifetime.getExpires(), val));
        }
        
        public IntStream values() {
            return backing.entrySet().stream()
                .filter(keyType::isInstance)
                .mapToInt((e) -> ((IntHolder) e.getValue()).value());
        }

        @Override
        protected Expiration untypedGenerate(final Object o) {
            return generate(keyType.cast(o));
        }

        @Override
        protected Expiration generate(final K key) {
            final K permanent = key.permanent();
            final CompletableFuture<Expiration> attempt = new CompletableFuture<>();
            final CompletableFuture<Expiration> future = inFlight.computeIfAbsent(permanent, (tmp) -> attempt);

            return (Expiration) ((future == attempt) ?
                                 doInFlight(permanent, future, () -> Expiration.forInt(lifetime.getExpires(), function.applyAsInt(key))) :
                                 extract(future));
        }
    }

    private class LongView<K extends Permanent<K>> extends View<K,Expiration> implements LongCacheView<K> {
        private final ToLongFunction<K> function;
        
        public LongView(final Class<K> keyType, final ConcurrentMap<Object,Object> backing,
                        final Lifetime lifetime, final ToLongFunction<K> function) {
            super(keyType, backing, lifetime);
            this.function = function;
        }

        public long value(final K key) {
            final LongHolder holder = (LongHolder) backing.get(key);
            if(holder != null) {
                return holder.value();
            }
            else {
                return ((LongHolder) generate(key)).value();
            }
        }
        
        public long get(final K key) {
            final LongHolder holder = (LongHolder) backing.get(key);
            return (holder == null) ? 0L : holder.value();
        }
        
        public void put(final K key, final long val) {
            backing.put(key.permanent(), Expiration.forLong(lifetime.getExpires(), val));
        }
        
        public LongStream values() {
            return backing.entrySet().stream()
                .filter(keyType::isInstance)
                .mapToLong((e) -> ((LongHolder) e.getValue()).value());
        }

        @Override
        protected Expiration untypedGenerate(final Object o) {
            return generate(keyType.cast(o));
        }

        @Override
        protected Expiration generate(final K key) {
            final K permanent = key.permanent();
            final CompletableFuture<Expiration> attempt = new CompletableFuture<>();
            final CompletableFuture<Expiration> future = inFlight.computeIfAbsent(permanent, (tmp) -> attempt);

            return (Expiration) ((future == attempt) ?
                                 doInFlight(permanent, future, () -> Expiration.forLong(lifetime.getExpires(), function.applyAsLong(key))) :
                                 extract(future));
        }
    }
    
    private class DoubleView<K extends Permanent<K>> extends View<K,Expiration> implements DoubleCacheView<K> {
        private final ToDoubleFunction<K> function;
        
        public DoubleView(final Class<K> keyType, final ConcurrentMap<Object,Object> backing,
                          final Lifetime lifetime, final ToDoubleFunction<K> function) {
            super(keyType, backing, lifetime);
            this.function = function;
        }

        public double value(final K key) {
            final DoubleHolder holder = (DoubleHolder) backing.get(key);
            if(holder != null) {
                return holder.value();
            }
            else {
                return ((DoubleHolder) generate(key)).value();
            }
        }
        
        public double get(final K key) {
            final DoubleHolder holder = (DoubleHolder) backing.get(key);
            return (holder == null) ? 0.0d : holder.value();
        }
        
        public void put(final K key, final double val) {
            backing.put(key.permanent(), Expiration.forDouble(lifetime.getExpires(), val));
        }
        
        public DoubleStream values() {
            return backing.entrySet().stream()
                .filter(keyType::isInstance)
                .mapToDouble((e) -> ((DoubleHolder) e.getValue()).value());
        }

        @Override
        protected Expiration untypedGenerate(final Object o) {
            return generate(keyType.cast(o));
        }

        @Override
        protected Expiration generate(final K key) {
            final K permanent = key.permanent();
            final CompletableFuture<Expiration> attempt = new CompletableFuture<>();
            final CompletableFuture<Expiration> future = inFlight.computeIfAbsent(permanent, (tmp) -> attempt);
            
            return (Expiration) ((future == attempt) ?
                                 doInFlight(permanent, future, () -> Expiration.forDouble(lifetime.getExpires(), function.applyAsDouble(key))) :
                                 extract(future));
        }
    }

    private class ObjectView<K extends Permanent<K>,V> extends View<K,Expiration> implements ObjectCacheView<K,V> {
        private final Function<K,V> function;
        private final Class<V> valueType;
        
        public ObjectView(final Class<K> keyType, final Class<V> valueType,
                          final ConcurrentMap<Object,Object> backing,
                          final Lifetime lifetime, final Function<K,V> function) {
            super(keyType, backing, lifetime);
            this.function = function;
            this.valueType = valueType;
        }

        public V value(final K key) {
            final ObjectHolder holder = (ObjectHolder) backing.get(key);
            if(holder != null) {
                return valueType.cast(holder.value());
            }
            else {
                return valueType.cast(((ObjectHolder) generate(key)).value());
            }
        }
        
        public V get(final K key) {
            final ObjectHolder holder = (ObjectHolder) backing.get(key);
            return valueType.cast((holder == null) ? null : holder.value());
        }
        
        public void put(final K key, final V val) {
            backing.put(key.permanent(), Expiration.forObject(lifetime.getExpires(), val));
        }
        
        public Stream<V> values() {
            return backing.entrySet().stream()
                .filter(keyType::isInstance)
                .map((e) -> valueType.cast(((ObjectHolder) e.getValue()).value()));
        }

        @Override
        protected Expiration untypedGenerate(final Object o) {
            return generate(keyType.cast(o));
        }

        @Override
        protected Expiration generate(final K key) {
            final K permanent = key.permanent();
            final CompletableFuture<Expiration> attempt = new CompletableFuture<>();
            final CompletableFuture<Expiration> future = inFlight.computeIfAbsent(permanent, (tmp) -> attempt);

            return (Expiration) ((future == attempt) ?
                                 doInFlight(permanent, future, () -> Expiration.forObject(lifetime.getExpires(), function.apply(key))) :
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
                    configs.put(methodId, new View<>(null, defaultBacking, val));
                }
            });
                
    }

    public ConcurrentMap<Object,Object> backing(final MethodId methodId) {
        return withConfigLock((all, configs) -> {
                final View<?,?> v = all.get(methodId);
                return v == null ? null : v.backing;
            });
    }

    public void backing(final MethodId methodId, final ConcurrentMap<Object,Object> val) {
        withConfigLock((all, configs) -> {
                if(all.containsKey(methodId)) {
                    final View<?,?> v = all.get(methodId);
                    final View<?,?> previous = v.copy();
                    v.backing = val;
                    previous.clear();
                }
                else if(configs.containsKey(methodId)) {
                    final View<?,?> v = configs.get(methodId);
                    v.backing = val;
                }
                else {
                    configs.put(methodId, new View<>(null, val, Lifetime.builder().build()));
                }
            });
    }

    public void backing(final ConcurrentMap<Object,Object> val) {
        withConfigLock((all, configs) -> {
                for(View<?,?> v : all.values()) {
                    if(v.backing == defaultBacking) {
                        final View<?,?> previous = v.copy();
                        v.backing = val;
                        previous.clear();
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

    private Lifetime resolveLifetime(final Map<MethodId, View<?,?>> configs, final MethodId methodId, final Lifetime lifetime) {
        Lifetime ret = lifetime;
        if(configs.containsKey(methodId)) {
            View<?,?> v = configs.get(methodId);
            ret = ret.toBuilder().merge(v.lifetime).resolveNulls().build();
        }
        
        return ret;
    }

    private ConcurrentMap<Object,Object> resolveBacking(final Map<MethodId, View<?,?>> configs, final MethodId methodId) {
        ConcurrentMap<Object,Object> backing = null;
        
        if(configs.containsKey(methodId)) {
            backing = configs.get(methodId).backing;
        }

        return backing == null ? defaultBacking : backing;
    }

    public <K extends Permanent<K>> DoubleCacheView<K> doubleView(Class<K> keyType, MethodId methodId, ToDoubleFunction<K> func, Lifetime lifetime) {
        return withConfigLock((all,configs) -> {
                if(all.containsKey(methodId)) {
                    @SuppressWarnings("unchecked") DoubleView<K> existing = (DoubleView<K>) all.get(methodId);
                    return existing;
                }

                DoubleView<K> ret = new DoubleView<>(keyType, resolveBacking(configs, methodId),
                                                     resolveLifetime(configs, methodId, lifetime), func);
                all.put(methodId, ret);
                return ret;
            });
    }

    public <K extends Permanent<K>> IntCacheView<K> intView(final Class<K> keyType, final MethodId methodId,
                                                            final ToIntFunction<K> func, final Lifetime lifetime) {
        return withConfigLock((all,configs) -> {
                if(all.containsKey(methodId)) {
                    @SuppressWarnings("unchecked") IntView<K> existing = (IntView<K>) all.get(methodId);
                    return existing;
                }

                IntView<K> ret = new IntView<>(keyType, resolveBacking(configs, methodId),
                                               resolveLifetime(configs, methodId, lifetime), func);
                all.put(methodId, ret);
                return ret;
            });
    }

    public <K extends Permanent<K>> LongCacheView<K> longView(Class<K> keyType, MethodId methodId, ToLongFunction<K> func, Lifetime lifetime) {
        return withConfigLock((all,configs) -> {
                if(all.containsKey(methodId)) {
                    @SuppressWarnings("unchecked") LongView<K> existing = (LongView<K>) all.get(methodId);
                    return existing;
                }

                LongView<K> ret = new LongView<>(keyType, resolveBacking(configs, methodId),
                                                 resolveLifetime(configs, methodId, lifetime), func);
                all.put(methodId, ret);
                return ret;
            });
    }

    public <K extends Permanent<K>, V> ObjectCacheView<K,V> objectView(final Class<K> keyType, final MethodId methodId,
                                                                       final Function<K,V> func, final Lifetime lifetime, final Class<V> valueType) {
        return withConfigLock((all,configs) -> {
                if(all.containsKey(methodId)) {
                    @SuppressWarnings("unchecked") ObjectView<K,V> existing = (ObjectView<K,V>) all.get(methodId);
                    return existing;
                }

                ObjectView<K,V> ret = new ObjectView<>(keyType, valueType, resolveBacking(configs, methodId),
                                                       resolveLifetime(configs, methodId, lifetime), func);
                all.put(methodId, ret);
                return ret;
            });
    }
}
