package io.github.guggle.studio;

import io.github.guggle.api.StudioContract;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ForkJoinPool;

final class StudioThreads {

    public interface StudioThread {}

    private static AtomicInteger counter = new AtomicInteger();
    
    private static String nextName(final StudioContract contract) {
        return String.format("StudioThread-%s-%d", contract.name(), counter.incrementAndGet());
    }
    
    public static class RegularThread extends Thread implements StudioThread {
        
        public RegularThread(final Runnable runnable, final StudioContract contract, final boolean daemon) {
            super(runnable, nextName(contract));
            setDaemon(daemon);
        }
    }

    public static class FJThread extends ForkJoinWorkerThread implements StudioThread {

        public FJThread(final ForkJoinPool pool, final StudioContract contract) {
            super(pool);
            setName(nextName(contract));
        }
    }

    private static class StudioThreadFactory implements ThreadFactory, ForkJoinWorkerThreadFactory {
        
        public StudioThreadFactory() {
            this(false);
        }
        
        public StudioThreadFactory(final boolean makeDaemon) {
            this.makeDaemon = makeDaemon;
        }
        
        private final boolean makeDaemon;
        
        public Thread newThread(final Runnable r) {
            return new RegularThread(r, StudioContract.IO, makeDaemon);
        }

        public ForkJoinWorkerThread newThread(final ForkJoinPool pool) {
            return new FJThread(pool, StudioContract.COMPUTE);
        }
    }

    public static StudioThreadFactory factory() {
        return new StudioThreadFactory();
    }

    public static StudioThreadFactory factory(final boolean makeDaemon) {
        return new StudioThreadFactory(makeDaemon);
    }
}

