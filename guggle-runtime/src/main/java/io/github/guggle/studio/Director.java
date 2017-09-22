package io.github.guggle.studio;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.concurrent.atomic.AtomicInteger;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.Message;
import io.github.guggle.api.StudioId;

abstract class Director {
    private volatile int maxWaitingMessages = -1;
    private volatile ExecutorService executor = null;
    private volatile Consumer<Message> messageFailure;
    
    private final AtomicInteger waitingMessages = new AtomicInteger();
    private final ConcurrentLinkedQueue<Message> queue = new ConcurrentLinkedQueue<>();
    private final TheStudio theStudio;
    private final StudioContract contract;

    public Director(final TheStudio theStudio, final StudioContract contract) {
        this.theStudio = theStudio;
        this.contract = contract;
    }

    public void setExecutor(final ExecutorService val) {
        this.executor = val;
    }

    protected ExecutorService getExecutor() {
        if(executor != null) {
            return executor;
        }
        else if(contract == StudioContract.IO) {
            return theStudio.defaultIoExecutor;
        }
        else {
            return theStudio.defaultComputeExecutor;
        }
    }

    private void submitMessage(final Message message) {
        getExecutor().submit(message);
    }
    
    public void messageReady(final Message message) {
        if(canAddMessage()) {
            queue.offer(message);
            final StudioId resource = checkoutResource();
            final Message foundMessage = queue.poll();
            if(foundMessage != null) {
                foundMessage.resource(resource);
                submitMessage(foundMessage);
            }
            else {
                putBackResource(resource);
            }
        }
        else if(messageFailure != null) {
            messageFailure.accept(message);
        }
        else {
            theStudio.defaultMessageFailure.accept(message);
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
        if(Thread.currentThread() instanceof StudioThread) {
            final Message message = queue.poll();
            if(message != null) {
                message.resource(studioId);
                submitMessage(message);
                waitingMessages.decrementAndGet();
            }
            else {
                putBackResource(studioId);
            }
        }
        else {
            putBackResource(studioId);
        }
    }

    public int waitingSize() {
        return waitingMessages.get();
    }

    protected abstract StudioId checkoutResource();
    protected abstract void putBackResource(StudioId studioId);
    public abstract StudioId exemplar();
}
