package io.github.guggle.studio;

import io.github.guggle.utils.*;
import io.github.guggle.api.MaximumMessagesException;
import io.github.guggle.api.NotFoundInStudioException;
import io.github.guggle.api.Permanent;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;
import io.github.guggle.api.Studio;
import java.util.ArrayDeque;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.ToIntBiFunction;
import java.util.function.BiConsumer;

public class TheStudio implements Studio {

    private volatile Consumer<Object> defaultMessageFailure = (m) -> {
        throw new MaximumMessagesException(String.format("Too many messages; message was: %s", m));
    };

    private volatile Consumer<Exception> onEventFail = (e) -> e.printStackTrace();

    private volatile ExecutorService defaultIoExecutor = Executors.newCachedThreadPool(StudioThreads.factory(true));
    
    private volatile ExecutorService defaultComputeExecutor = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                                                                               StudioThreads.factory(),
                                                                               null, true);
    
    private final ConcurrentHashMap<Object, Director> directors = new ConcurrentHashMap<>();

    private Director find(final Object key) {
        final Director director = directors.get(key);
        if(director == null) {
            throw new NotFoundInStudioException(String.format("Director has not been configured to handle %s", key));
        }

        return director;
    }
    
    public <T> T find(final Object id, final Class<T> type) {
        final Director d = directors.get(id);
        if(d != null) {
            return type.cast(d.exemplar());
        }
        else {
            final String message = String.format("Did not find actor/agent with id %s and type %s", id, type);
            throw new NotFoundInStudioException(message);
        }
    }

    //TODO: Make more like this
    public <T, M extends Permanent<M>> void event(final Object id, final Class<T> targetType,
                                                  final BiConsumer<T,M> eventConsumer, final M message) {
        final M permanent = message.permanent();
        find(id).messageReady((o) -> {
                try {
                    eventConsumer.accept(targetType.cast(o), permanent);
                }
                catch(Exception e) {
                    onEventFail.accept(e);
                }
            });
    }

    public <T, M extends Permanent<M>> CompletableFuture<Void> eventMessage(final Object id, final Class<T> targetType,
                                                                            final BiConsumer<T,M> eventConsumer,
                                                                            final M message) {
        final M permanent = message.permanent();
        final CompletableFuture<Void> future = new CompletableFuture<>();
        
        find(id).messageReady((o) -> {
                try {
                    eventConsumer.accept(targetType.cast(o), permanent);
                    future.complete(null);
                }
                catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });

        return future;
    }

    public <T, M extends Permanent<M>> CompletableFuture<Boolean> message(final Object id, final Class<T> targetType,
                                                                          final ToBooleanBiFunction<T,M> function,
                                                                          final M message) {
        final M permanent = message.permanent();
        final CompletableFuture<Boolean> future = new CompletableFuture<>();

        find(id).messageReady((o) -> {
                try {
                    future.complete(function.applyAsBoolean(targetType.cast(o), permanent));
                }
                catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });

        return future;
    }
    
    public <T, M extends Permanent<M>> CompletableFuture<Byte> message(final Object id, final Class<T> targetType,
                                                                       final ToByteBiFunction<T,M> function,
                                                                       final M message) {
        final M permanent = message.permanent();
        final CompletableFuture<Byte> future = new CompletableFuture<>();

        find(id).messageReady((o) -> {
                try {
                    future.complete(function.applyAsByte(targetType.cast(o), permanent));
                }
                catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });

        return future;
    }
    
    public <T, M extends Permanent<M>> CompletableFuture<Short> message(final Object id, final Class<T> targetType,
                                                                        final ToShortBiFunction<T,M> function,
                                                                        final M message) {
        final M permanent = message.permanent();
        final CompletableFuture<Short> future = new CompletableFuture<>();

        find(id).messageReady((o) -> {
                try {
                    future.complete(function.applyAsShort(targetType.cast(o), permanent));
                }
                catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });

        return future;
    }

    public <T, M extends Permanent<M>> CompletableFuture<Integer> message(final Object id, final Class<T> targetType,
                                                                          final ToIntBiFunction<T,M> function,
                                                                          final M message) {
        final M permanent = message.permanent();
        final CompletableFuture<Integer> future = new CompletableFuture<>();

        find(id).messageReady((o) -> {
                try {
                    future.complete(function.applyAsInt(targetType.cast(o), permanent));
                }
                catch(Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        
        return future;
    }

    public void singleton(final StudioId actor) {
        singleton(actor, StudioContract.UNKNOWN);
    }

    public void singleton(final StudioId actor, final StudioContract contract) {
        directors.computeIfAbsent(actor.studioId(), (k) -> new SingleDirector(this, contract, actor));
    }

    public void singleton(final StudioId actor, final ExecutorService executor) {
        directors.computeIfAbsent(actor.studioId(),
                                  (k) -> new SingleDirector(this, StudioContract.UNKNOWN, actor).setExecutor(executor));
    }

    public void fixed(final StudioId actor, final int num, final Supplier<StudioId> supplier) {
        fixed(actor, num, supplier, StudioContract.UNKNOWN);
    }

    public void fixed(final StudioId actor, final int num,
                      final Supplier<StudioId> supplier, final StudioContract contract) {
        directors.computeIfAbsent(actor.studioId(), (k) -> new FixedDirectors(this, contract, num, supplier));
    }

    public void fixed(final StudioId actor, final int num, final Supplier<StudioId> supplier, final ExecutorService executor) {
        directors.computeIfAbsent(actor.studioId(),
                                  (k) -> new FixedDirectors(this, StudioContract.UNKNOWN, num, supplier).setExecutor(executor));
    }

    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier) {
        growable(actor, max, maxWaiters, supplier, StudioContract.UNKNOWN);
    }

    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier, final StudioContract contract) {
        directors.computeIfAbsent(actor.studioId(), (k) -> new GrowableDirectors(this, contract, max, supplier, maxWaiters));
    }
    
    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier, final ExecutorService executor) {
        directors.computeIfAbsent(actor.studioId(),
                                  (k) -> new GrowableDirectors(this, StudioContract.UNKNOWN, max, supplier, maxWaiters).setExecutor(executor));
    }

    public void messageFailure(final Object id, final Consumer<Object> consumer) {
        directors.computeIfPresent(id, (k,d) -> d.setMessageFailure(consumer));
    }

    public Consumer<Object> messageFailure(final Object id) {
        final Director d = directors.get(id);
        return d == null ? null : d.getMessageFailure();
    }

    public void maxWaitingMessages(final Object id, final int max) {
        directors.computeIfPresent(id, (k, d) -> d.setMaxWaitingMessages(max));
    }

    public int maxWaitingMessages(final Object id) {
        final Director d = directors.get(id);
        return d == null ? null : d.getMaxWaitingMessages();
    }

    public void executor(final Object id, final ExecutorService executor) {
        directors.computeIfPresent(id, (k, d) -> d.setExecutor(executor));
    }

    public ExecutorService executor(final Object id) {
        final Director d = directors.get(id);
        return d == null ? null : d.getExecutor();
    }

    public ExecutorService getDefaultIoExecutor() {
        return defaultIoExecutor;
    }

    public void setDefaultIoExecutor(final ExecutorService val) {
        this.defaultIoExecutor = val;
    }

    public ExecutorService getDefaultComputeExecutor() {
        return defaultComputeExecutor;
    }

    public void setDefaultComputeExecutor(final ExecutorService val) {
        this.defaultComputeExecutor = val;
    }

    public Consumer<Object> getDefaultMessageFailure() {
        return defaultMessageFailure;
    }

    public void setDefaultMessageFailure(final Consumer<Object> val) {
        defaultMessageFailure = val;
    }
}
