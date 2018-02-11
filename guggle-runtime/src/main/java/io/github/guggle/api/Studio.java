package io.github.guggle.api;

import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.ExecutorService;
import io.github.guggle.studio.TheStudio;
import java.util.function.*;
import java.util.concurrent.CompletableFuture;
import io.github.guggle.utils.*;

public interface Studio {
    <T> T find(Object id, Class<T> type);
    
    <T, M extends Permanent<M>> void event(final Object id, final Class<T> targetType,
                                           final BiConsumer<T,M> eventConsumer, final M message);

    <T, M extends Permanent<M>> CompletableFuture<Void> eventMessage(final Object id, final Class<T> targetType,
                                                                     final BiConsumer<T,M> eventConsumer,
                                                                     final M message);
    
    <T, M extends Permanent<M>> CompletableFuture<Boolean> message(final Object id, final Class<T> targetType,
                                                                   final ToBooleanBiFunction<T,M> function,
                                                                   final M message);
    
    <T, M extends Permanent<M>> CompletableFuture<Byte> message(final Object id, final Class<T> targetType,
                                                                final ToByteBiFunction<T,M> function,
                                                                final M message);

    <T, M extends Permanent<M>> CompletableFuture<Short> message(final Object id, final Class<T> targetType,
                                                                 final ToShortBiFunction<T,M> function,
                                                                 final M message);
    
    <T, M extends Permanent<M>> CompletableFuture<Integer> message(final Object id, final Class<T> targetType,
                                                                   final ToIntBiFunction<T,M> function,
                                                                   final M message);

    void singleton(StudioId actor);
    void singleton(StudioId actor, StudioContract contract);
    void singleton(StudioId actor, ExecutorService executor);

    void fixed(StudioId actor, int num, Supplier<StudioId> supplier);
    void fixed(StudioId actor, int num, Supplier<StudioId> supplier, StudioContract contract);
    void fixed(StudioId actor, int num, Supplier<StudioId> supplier, ExecutorService executor);

    void growable(StudioId actor, int max, int maxWaiters, Supplier<StudioId> supplier);
    void growable(StudioId actor, int max, int maxWaiters, Supplier<StudioId> supplier, StudioContract contract);    
    void growable(StudioId actor, int max, int maxWaiters, Supplier<StudioId> supplier, ExecutorService executor);

    void messageFailure(Object id, Consumer<Object> consumer);
    Consumer<Object> messageFailure(Object id);

    void maxWaitingMessages(Object id, int max);
    int maxWaitingMessages(Object id);

    void executor(Object id, ExecutorService executor);
    ExecutorService executor(Object id);

    ExecutorService getDefaultIoExecutor();
    void setDefaultIoExecutor(ExecutorService val);

    ExecutorService getDefaultComputeExecutor();
    void setDefaultComputeExecutor(ExecutorService val);

    Consumer<Object> getDefaultMessageFailure();
    void setDefaultMessageFailure(Consumer<Object> val);

    public static Studio instance() {
        return new TheStudio();
    }
}
