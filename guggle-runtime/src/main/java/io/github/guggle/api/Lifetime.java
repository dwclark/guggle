package io.github.guggle.api;

import java.time.temporal.ChronoUnit;

public class Lifetime {

    private final Expires expires;
    private final Refresh refresh;
    private final long interval;
    private final ChronoUnit units;
    private final int maxSize;

    public Lifetime(final Expires expires,
                    final Refresh refresh,
                    final long interval,
                    final ChronoUnit units,
                    final int maxSize) {
        this.expires = expires;
        this.refresh = refresh;
        this.interval = interval;
        this.units = units;
        this.maxSize = maxSize;
    }

    public Expires getExpires() {
        return expires;
    }

    public Refresh getRefresh() {
        return refresh;
    }

    public long getInterval() {
        return interval;
    }

    public ChronoUnit getUnits() {
        return units;
    }

    public int getMaxSize() {
        return maxSize;
    }
}
