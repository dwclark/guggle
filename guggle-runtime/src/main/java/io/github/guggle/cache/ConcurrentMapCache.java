package io.github.guggle.cache;

import io.github.guggle.api.*;
import java.util.concurrent.ConcurrentMap;
import java.time.Instant;

public class ConcurrentMapCache {

    final ConcurrentMap<Permanent<?>, Expiration> map;
    final Instant start;
    
    public ConcurrentMapCache(final ConcurrentMap<Permanent<?>, Expiration> map) {
        this.map = map;
        this.start = Instant.now();
    }

    
}
