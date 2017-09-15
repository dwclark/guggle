package io.github.guggle.cache;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import io.github.guggle.api.*;
import static io.github.guggle.api.Expires.*;
import io.github.guggle.utils.TimeUnits;

public class Expiration {
    private final static int DIRTY = Integer.MIN_VALUE;
    
    private volatile int lastAccessed;
    private final int created;

    public Expiration(final Instant start) {
        this.created = diff(start);
        this.lastAccessed = created;
    }
    
    public void updateAccessed(final Instant start) {
        if(lastAccessed != DIRTY) {
            this.lastAccessed = diff(start);
        }
    }

    // public void dirty() {
    //     lastAccessed = DIRTY;
    // }

    public boolean expired(final Expires expires, final TimeUnits timeUnits,
                           final Instant asOf, final Instant start) {
        if(lastAccessed == DIRTY) {
            return true;
        }

        if(expires == NEVER) {
            return false;
        }

        final int tmp = (expires == FIXED) ? created : lastAccessed;
        final Instant expiresAt = Instant.ofEpochMilli(expand(tmp) + start.toEpochMilli()).plus(timeUnits.getInterval(), timeUnits.getChronoUnit());
        return expiresAt.isBefore(asOf);
    }

    private int diff(final Instant start) {
        return truncate(Instant.now().toEpochMilli() - start.toEpochMilli());
    }

    private int truncate(final long val) {
        return (int) (val / 1_000L);
    }

    private long expand(final int val) {
        return ((long) val) * 1_000L;
    }

    public static long start() {
        return Instant.now().toEpochMilli();
    }

    public static class IntHolder extends Expiration {
        private final int val;

        public IntHolder(final Instant start, final int val) {
            super(start);
            this.val = val;
        }

        public int value() {
            return val;
        }
    }

    public static class LongHolder extends Expiration {
        private final long val;

        public LongHolder(final Instant start, final long val) {
            super(start);
            this.val = val;
        }

        public long value() {
            return val;
        }
    }

    public static class DoubleHolder extends Expiration {
        private final double val;

        public DoubleHolder(final Instant start, final double val) {
            super(start);
            this.val = val;
        }

        public double value() {
            return val;
        }
    }

    public static class ObjectHolder extends Expiration {
        private final Object val;

        public ObjectHolder(final Instant start, final Object val) {
            super(start);
            this.val = val;
        }

        public Object value() {
            return val;
        }
    }

    public static IntHolder intHolder(final Instant start, final int val) {
        return new IntHolder(start, val);
    }

    public static LongHolder longHolder(final Instant start, final long val) {
        return new LongHolder(start, val);
    }

    public static DoubleHolder doubleHolder(final Instant start, final double val) {
        return new DoubleHolder(start, val);
    }

    public static ObjectHolder objectHolder(final Instant start, final Object val) {
        return new ObjectHolder(start, val);
    }
}
