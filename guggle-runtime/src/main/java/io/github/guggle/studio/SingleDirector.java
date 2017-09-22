package io.github.guggle.studio;

import java.util.concurrent.atomic.AtomicReference;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;

class SingleDirector extends Director {

    private final AtomicReference<StudioId> ref = new AtomicReference<>();
    
    private final StudioId exemplar;

    public StudioId exemplar() {
        return exemplar;
    }

    protected StudioId checkoutResource() {
        final StudioId val = ref.get();
        if(val == null) {
            return null;
        }

        return ref.compareAndSet(val, null) ? val : null;
    }

    protected void putBackResource(final StudioId val) {
        if(!ref.compareAndSet(null, val)) {
            throw new IllegalStateException("Trying to put back a resource that is already back");
        }
    }

    public SingleDirector(final TheStudio theStudio, final StudioContract contract, final StudioId val) {
        super(theStudio, contract);
        ref.set(val);
        this.exemplar = val;
    }
}
