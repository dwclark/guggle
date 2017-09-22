package io.github.guggle.utils;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class NamedThread implements ThreadFactory {

    private final AtomicInteger counter;
    private final String prefix;
    private final boolean daemon;

    private String next() {
        return String.format("%s - %d", prefix, counter.incrementAndGet());
    }
    
    public NamedThread(final String prefix, final boolean daemon) {
        this.prefix = prefix;
        this.daemon = daemon;
        this.counter = new AtomicInteger();
    }
    
    public Thread newThread(final Runnable r) {
        final Thread ret = new Thread(r, next());
        ret.setDaemon(daemon);
        return ret;
    }
}
