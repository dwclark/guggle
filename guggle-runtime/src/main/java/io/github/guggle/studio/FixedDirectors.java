package io.github.guggle.studio;

import java.util.concurrent.atomic.AtomicReferenceArray;
import io.github.guggle.utils.AtomicBitSet;
import io.github.guggle.api.StudioContract;
import io.github.guggle.api.StudioId;
import java.util.function.Supplier;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Map;

class FixedDirectors extends Director {

    private final AtomicReferenceArray<StudioId> refs;
    private final AtomicBitSet bitSet;
    private final Map<StudioId,Integer> indexes;
    
    public StudioId exemplar() {
        return refs.get(0);
    }

    protected StudioId checkoutResource() {
        int index = 0;
        while(index != bitSet.nextClearBit(index)) {
            if(bitSet.setIfUnset(index)) {
                return refs.get(index);
            }
        }

        return null;
    }

    protected void putBackResource(final StudioId val) {
        final int index = indexes.get(val);
        if(!bitSet.clearIfSet(index)) {
            throw new IllegalStateException("Trying to put back a resource that is already back");
        }
    }

    public FixedDirectors(final TheStudio theStudio, final StudioContract contract, final int max, final Supplier<StudioId> supplier) {
        super(theStudio, contract);

        this.refs = new AtomicReferenceArray<>(max);
        this.bitSet = new AtomicBitSet(max);

        final IdentityHashMap<StudioId,Integer> map = new IdentityHashMap<>(max);
        for(int i = 0; i < max; ++i) {
            final StudioId s = supplier.get();
            refs.set(i, s);
            map.put(s, i);
        }

        this.indexes = Collections.unmodifiableMap(map);
    }
}
