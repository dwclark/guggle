package io.github.guggle.studio;

import io.github.guggle.api.Message;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;
import io.github.guggle.utils.ExemplarQueue;
import io.github.guggle.utils.MaxSizeQueue;
import io.github.guggle.utils.NamedThread;
import io.github.guggle.utils.ResourceQueue;
import io.github.guggle.utils.SingletonQueue;
import java.util.ArrayDeque;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TheStudio {

    volatile Consumer<Message> defaultMessageFailure = (m) -> {};

    volatile ExecutorService defaultIoExecutor = Executors.newCachedThreadPool(StudioThreads.factory(true));
    
    volatile ExecutorService defaultComputeExecutor = new ForkJoinPool(Runtime.getRuntime().availableProcessors(),
                                                                       StudioThreads.factory(),
                                                                       null, true);
    
    private final ConcurrentHashMap<Object, Director> directors = new ConcurrentHashMap<>();

    private Director find(final Object key) {
        final Director director = directors.get(key);
        if(director == null) {
            throw new IllegalStateException("Director has not been configured to handle %s", message.studioId());
        }

        return director;
    }
    
    public void message(final Message message) {
        find(message.studioId()).messageReady(message);
    }

    public void resource(final StudioId o) {
        find(message.studioId()).messageReady(message);
    }

    public void singleton(final StudioId actor) {
        singleton(actor, StudioContract.UNKNOWN);
    }

    public void singleton(final StudioId actor, final StudioContract contract) {
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                return new SingleDirector(this, contract, actor);
            });
    }

    public void singleton(final StudioId actor, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                final Director d = new SingleDirector(this, StudioContract.UNKNOWN);
                d.setExecutor(executor);
                return d;
            });
    }

    public void fixed(final StudioId actor, final int num, final Supplier<StudioId> supplier) {
        fixed(actor, num, supplier, StudioContract.UNKNOWN);
    }

    public void fixed(final StudioId actor, final int num,
                      final Supplier<StudioId> supplier, final StudioContract studioExecution) {
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                return new QueuePair(new MaxSizeQueue<>(num, supplier), studioExecution);
            });
    }

    public void fixed(final StudioId actor, final int num, final Supplier<StudioId> supplier, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                return new QueuePair(new MaxSizeQueue<>(num, supplier), executor, StudioContract.UNKNOWN);
            });
    }

    private final Consumer<StudioId> growableConsumer = (newActor) -> {
            messagesResources.computeIfPresent(newActor.studioId(), (id, pair) -> {
                    pair.addResource(newActor);
                    return pair;
                });
        };

    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier) {
        growable(actor, max, maxWaiters, supplier, StudioContract.UNKNOWN);
    }

    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier, final StudioContract studioExecution) {
        final ExecutorService executor = (studioExecution == StudioContract.COMPUTE ?
                                          defaultComputeExecutor :
                                          defaultIoExecutor);
        
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                return new QueuePair(new ResourceQueue<>(max, maxWaiters, supplier, executor, growableConsumer),
                                     studioExecution);
            });
    }

    public void growable(final StudioId actor, final int max, final int maxWaiters,
                         final Supplier<StudioId> supplier, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.studioId(), (k) -> {
                return new QueuePair(new ResourceQueue<>(max, maxWaiters, supplier, executor, growableConsumer),
                                     executor, StudioContract.UNKNOWN);
            });
    }

    public void onAddMessageFailure(final Object id, final Consumer<Message> consumer) {
        messagesResources.computeIfPresent(id, (k,pair) -> {
                pair.setAddMessageFailure(consumer);
                return pair;
            });
    }

    public Consumer<Message> onAddMessageFailure(final Object id) {
        final QueuePair pair = messagesResources.get(id);
        return pair == null ? null : pair.getAddMessageFailure();
    }

    public void maxWaitingMessages(final Object id, final int max) {
        messagesResources.computeIfPresent(id, (k, pair) -> {
                pair.setMaxWaitingMessages(max);
                return pair;
            });
    }

    public int maxWaitingMessages(final Object id) {
        final QueuePair pair = messagesResources.get(id);
        return pair == null ? null : pair.getMaxWaitingMessages();
    }

    public void executor(final Object id, final ExecutorService executor) {
        messagesResources.computeIfPresent(id, (k, pair) -> {
                pair.setExecutor(executor);
                return pair;
            });
    }

    public ExecutorService executor(final Object id) {
        final QueuePair pair = messagesResources.get(id);
        return pair == null ? null : pair.getExecutor();
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

    public <T> T find(final Object id, final Class<T> type) {
        final QueuePair pair = messagesResources.get(id);
        return pair != null ? type.cast(pair.resources.exemplar()) : null;
    }
}
