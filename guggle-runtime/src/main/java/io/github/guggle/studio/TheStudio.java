package io.github.guggle.studio;

import java.util.ArrayDeque;
import java.util.NavigableSet;
import java.util.Queue;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import io.github.guggle.api.ActorId;
import io.github.guggle.api.Message;
import io.github.guggle.utils.SingletonQueue;
import io.github.guggle.utils.MaxSizeQueue;
import io.github.guggle.utils.ResourceQueue;
import java.util.function.Supplier;
import java.util.function.Consumer;

public class TheStudio {
    
    private class QueuePair {

        private ExecutorService _executor;
        private Consumer<Message> _addMessageFailure;
        private int maxWaitingMessages = Integer.MAX_VALUE;
        private final Queue<Message> messages = new ArrayDeque<>();
        private final Queue<ActorId> resources;
        
        public void setAddMessageFailure(final Consumer<Message> val) {
            if(val == null) {
                throw new IllegalArgumentException("Message failure handler cannot be null");
            }
            
            _addMessageFailure = val;
        }

        public Consumer<Message> getAddMessageFailure() {
            return _addMessageFailure == null ? defaultAddMessageFailure : _addMessageFailure;
        }

        public void setMaxWaitingMessages(final int val) {
            if(val < 0) {
                final String m = String.format("maxWaitingMessages must be positive, %d was passed", val);
                throw new IllegalArgumentException(m);
            }

            maxWaitingMessages = val;
        }

        public int getMaxWaitingMessages() {
            return maxWaitingMessages;
        }

        public void setExecutor(final ExecutorService executor) {
            _executor = executor;
        }

        public ExecutorService getExecutor() {
            return _executor == null ? defaultIoExecutor : _executor;
        }

        private Consumer<ActorId> addActorFailure = (a) -> {
            final String m = String.format("Failed to add actor %s to queue. This is a configuration or library error.", a);
            throw new IllegalStateException(m);
        };

        private void runAll() {
            while(resources.peek() != null && messages.peek() != null) {
                final Message message = messages.poll();
                message.resource(resources.poll());
                getExecutor().submit(message);
            }
        }

        public QueuePair(final Queue<ActorId> resources) {
            this(resources, null);
        }
        
        public QueuePair(final Queue<ActorId> resources, final ExecutorService executor) {
            this.resources = resources;
            _executor = executor;
        }
        
        public void addMessage(final Message message) {
            if(messages.size() >= maxWaitingMessages || !messages.offer(message)) {
                getAddMessageFailure().accept(message);
            }
            else {
                runAll();
            }
        }

        public void addResource(final ActorId actor) {
            if(!resources.offer(actor)) {
                addActorFailure.accept(actor);
            }
            else {
                runAll();
            }
        }
    }

    private volatile Consumer<Message> defaultAddMessageFailure = (m) -> {};
    private volatile ExecutorService defaultIoExecutor = Executors.newCachedThreadPool();
    private volatile ExecutorService defaultComputeExecutor = ForkJoinPool.commonPool();
    private final ConcurrentHashMap<Object, QueuePair> messagesResources = new ConcurrentHashMap<>();
    
    public void message(final Message message) {
        messagesResources.compute(message.actorId(), (k, pair) -> {
                pair.addMessage(message);
                return pair;
            });
    }

    public void resource(final ActorId o) {
        messagesResources.compute(o.actorId(), (k, pair) -> {
                pair.addResource(o);
                return pair;
            });
    }

    public void singleton(final ActorId actor) {
        singleton(actor, null);
    }

    public void singleton(final ActorId actor, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.actorId(), (k) -> {
                SingletonQueue<ActorId> q = new SingletonQueue<>();
                q.offer(actor);
                return new QueuePair(q, executor);
            });
    }

    public void fixed(final ActorId actor, final int num, final Supplier<ActorId> supplier) {
        fixed(actor, num, supplier, null);
    }

    public void fixed(final ActorId actor, final int num, final Supplier<ActorId> supplier, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.actorId(), (k) -> {
                return new QueuePair(new MaxSizeQueue<>(num, supplier), executor);
            });
    }

    private final Consumer<ActorId> growableConsumer = (newActor) -> {
            messagesResources.computeIfPresent(newActor.actorId(), (id, pair) -> {
                    pair.addResource(newActor);
                    return pair;
                });
        };

    public void growable(final ActorId actor, final int max, final int maxWaiters,
                         final Supplier<ActorId> supplier) {
        growable(actor, max, maxWaiters, supplier, null);
    }
    
    public void growable(final ActorId actor, final int max, final int maxWaiters,
                         final Supplier<ActorId> supplier, final ExecutorService executor) {
        messagesResources.computeIfAbsent(actor.actorId(), (k) -> {
                return new QueuePair(new ResourceQueue<>(max, maxWaiters, supplier, executor, growableConsumer), executor);
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
}
