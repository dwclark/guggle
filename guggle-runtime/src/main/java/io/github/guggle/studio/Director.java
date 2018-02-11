package io.github.guggle.studio;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;
import io.github.guggle.studio.StudioThreads.StudioThread;

abstract class Director {
    
    private volatile int maxWaitingMessages = -1;
    private volatile ExecutorService executor = null;
    private volatile Consumer<Object> messageFailure;
    
    private final AtomicInteger waitingMessages = new AtomicInteger();
    private final ConcurrentLinkedQueue<Consumer<Object>> queue = new ConcurrentLinkedQueue<>();
    private final TheStudio theStudio;
    private final StudioContract contract;

    public Director(final TheStudio theStudio, final StudioContract contract) {
        this.theStudio = theStudio;
        this.contract = contract;
    }

    public Consumer<Object> getMessageFailure() {
        return messageFailure != null ? messageFailure : theStudio.getDefaultMessageFailure();
    }

    public Director setMessageFailure(final Consumer<Object> val) {
        this.messageFailure = val;
        return this;
    }

    public int getMaxWaitingMessages() {
        return maxWaitingMessages;
    }

    public Director setMaxWaitingMessages(final int val) {
        this.maxWaitingMessages = val;
        return this;
    }

    public Director setExecutor(final ExecutorService val) {
        this.executor = val;
        return this;
    }

    protected ExecutorService getExecutor() {
        if(executor != null) {
            return executor;
        }
        else if(contract == StudioContract.IO) {
            return theStudio.getDefaultIoExecutor();
        }
        else {
            return theStudio.getDefaultComputeExecutor();
        }
    }

    private void sendAll() {
        while(true) {
            final Consumer<Object> found = queue.peek();
            if(found == null) {
                return;
            }
            
            final StudioId actor = checkoutResource();
            if(actor == null) {
                return;
            }
            
            final Consumer<Object> polled = queue.poll();
            if(found == polled) {
                getExecutor().submit(() -> {
                        found.accept(actor);
                        resourceReady(actor);
                    });
                    
                waitingMessages.decrementAndGet();
            }
            else {
                putBackResource(actor);
            }
        }
    }
    
    public void messageReady(final Consumer<Object> message) {
        if(!canAddMessage()) {
            getMessageFailure().accept(message);
        }
        else {
            queue.offer(message);
            sendAll();
        }
    }

    private boolean canAddMessage() {
        final int max = maxWaitingMessages;
        if(max < 0) {
            return true;
        }

        int current = waitingMessages.get();
        if(current == max) {
            return false;
        }

        while(!waitingMessages.compareAndSet(current, current + 1)) {
            current = waitingMessages.get();
            if(current == max) {
                return false;
            }
        }

        return true;
    }
    
    public void resourceReady(final StudioId studioId) {
        putBackResource(studioId);
        sendAll();
    }

    public int waitingSize() {
        return waitingMessages.get();
    }

    protected abstract StudioId checkoutResource();
    protected abstract void putBackResource(StudioId studioId);
    public abstract StudioId exemplar();
}
