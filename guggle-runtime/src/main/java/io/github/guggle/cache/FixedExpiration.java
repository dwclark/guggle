package io.github.guggle.cache;

import java.time.Instant;
import io.github.guggle.utils.TimeUnits;
import io.github.guggle.api.Expires;

public class FixedExpiration implements Expiration {

    private final int accessed;

    protected FixedExpiration(final int accessed) {
        this.accessed = accessed;
    }
    
    public void accessed() { }

    public int getAccessed() {
        return accessed;
    }
    
    public Expired expired(final Expires expires, final TimeUnits timeUnits, final Instant asOf) {
        return expires == Expires.FIXED ? AllCaches.expired(accessed, timeUnits, asOf) : Expired.UNKNOWN;
    }

    public static class ForInt extends FixedExpiration implements IntHolder {

        private final int value;
        
        public ForInt(final int value) {
            this(value, AllCaches.sinceEpoch());
        }

        public ForInt(final int value, final int accessed) {
            super(accessed);
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static class ForLong extends FixedExpiration implements LongHolder {
        
        private final long value;
        
        public ForLong(final long value) {
            this(value, AllCaches.sinceEpoch());
        }

        public ForLong(final long value, final int accessed) {
            super(accessed);
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    public static class ForDouble extends FixedExpiration implements DoubleHolder {

        private final double value;
        
        public ForDouble(final double value) {
            this(value, AllCaches.sinceEpoch());
        }

        public ForDouble(final double value, final int accessed) {
            super(accessed);
            this.value = value;
        }

        public double value() {
            return value;
        }
    }

    public static class ForObject extends FixedExpiration implements ObjectHolder {

        private final Object value;
        
        public ForObject(final Object value) {
            this(value, AllCaches.sinceEpoch());
        }

        public ForObject(final Object value, final int accessed) {
            super(accessed);
            this.value = value;
        }

        public Object value() {
            return value;
        }
    }
}
