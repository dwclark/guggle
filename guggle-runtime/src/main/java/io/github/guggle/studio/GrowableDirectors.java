package io.github.guggle.studio;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentLinkedQueue;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;
import java.util.function.Supplier;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;

class GrowableDirectors extends Director {

    private final int maxWaitersBeforeExpansion;
    private final int max;
    private final Supplier<StudioId> supplier;
    private final StudioId exemplar;
    private final ConcurrentLinkedQueue<StudioId> resources = new ConcurrentLinkedQueue<>();
    private final AtomicInteger size = new AtomicInteger();
    private final AtomicBoolean underConstruction = new AtomicBoolean(false);

    public StudioId exemplar() {
        return exemplar;
    }

    protected StudioId checkoutResource() {
        final StudioId ret = resources.poll();
        if(ret == null && size.get() < max && maxWaitersBeforeExpansion < waitingSize()) {
            newResource();
        }

        return ret;
    }

    protected void putBackResource(final StudioId val) {
        resources.offer(val);
    }

    private void newResource() {
        if(underConstruction.compareAndSet(false, true)) {
            getExecutor().submit(() -> {
                    final StudioId val = supplier.get();
                    resourceReady(val);
                    underConstruction.set(false);
                });
        }
    }

    public GrowableDirectors(final TheStudio theStudio, final StudioContract contract,
                             final int max, final Supplier<StudioId> supplier, final int maxWaitersBeforeExpansion) {
        super(theStudio, contract);

        this.maxWaitersBeforeExpansion = maxWaitersBeforeExpansion;
        this.max = max;
        this.supplier = supplier;
        this.exemplar = supplier.get();
        this.resources.offer(exemplar);
    }
}
