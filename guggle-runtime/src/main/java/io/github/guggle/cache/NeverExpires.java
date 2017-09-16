package io.github.guggle.cache;

import java.time.Instant;
import io.github.guggle.utils.TimeUnits;
import io.github.guggle.api.Expires;

public class NeverExpires implements Expiration {

    public int getAccessed() {
        return -1;
    }
    
    public void accessed() { }
    
    public Expired expired(final Expires expires, final TimeUnits timeUnits, final Instant asOf) {
        return expires == Expires.NEVER ? Expired.FALSE : Expired.UNKNOWN;
    }

    public static class ForInt extends NeverExpires implements IntHolder {

        private final int value;
        
        public ForInt(final int value) {
            this.value = value;
        }

        public int value() {
            return value;
        }
    }

    public static class ForLong extends NeverExpires implements LongHolder {

        private final long value;
        
        public ForLong(final long value) {
            this.value = value;
        }

        public long value() {
            return value;
        }
    }

    public static class ForDouble extends NeverExpires implements DoubleHolder {

        private final double value;
        
        public ForDouble(final double value) {
            this.value = value;
        }

        public double value() {
            return value;
        }
    }

    public static class ForObject extends NeverExpires implements ObjectHolder {

        private final Object value;
        
        public ForObject(final Object value) {
            this.value = value;
        }

        public Object value() {
            return value;
        }
    }
}
