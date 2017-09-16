package io.github.guggle.cache;

import java.time.Instant;
import io.github.guggle.utils.TimeUnits;
import io.github.guggle.api.Expires;

public interface Expiration {
    void accessed();
    int getAccessed();
    Expired expired(Expires expires, TimeUnits timeUnits, Instant asOf);

        public static Expiration convert(final Expires expires, final Expiration current) {
        if(current instanceof IntHolder) {
            return forInt(expires, current);
        }
        else if(current instanceof LongHolder) {
            return forLong(expires, current);
        }
        else if(current instanceof DoubleHolder) {
            return forDouble(expires, current);
        }
        else {
            return forObject(expires, current);
        }
    }
    
    public static Expiration forInt(final Expires expires, final int value) {
        switch(expires) {
        case NEVER: return new NeverExpires.ForInt(value);
        case FIXED: return new FixedExpiration.ForInt(value);
        case ACCESSED: return new AccessedExpiration.ForInt(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forInt(final Expires expires, final Expiration current) {
        final int accessed = current.getAccessed();
        final int value = ((IntHolder) current).value();
        
        switch(expires) {
        case NEVER: return new NeverExpires.ForInt(value);
        case FIXED: return (accessed >= 0) ? new FixedExpiration.ForInt(value, accessed) : new FixedExpiration.ForInt(value);
        case ACCESSED: return (accessed >= 0) ? new AccessedExpiration.ForInt(value, accessed) : new AccessedExpiration.ForInt(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forLong(final Expires expires, final long value) {
        switch(expires) {
        case NEVER: return new NeverExpires.ForLong(value);
        case FIXED: return new FixedExpiration.ForLong(value);
        case ACCESSED: new AccessedExpiration.ForLong(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forLong(final Expires expires, final Expiration current) {
        final int accessed = current.getAccessed();
        final long value = ((LongHolder) current).value();
        
        switch(expires) {
        case NEVER: return new NeverExpires.ForLong(value);
        case FIXED: return (accessed >= 0) ? new FixedExpiration.ForLong(value, accessed) : new FixedExpiration.ForLong(value);
        case ACCESSED: return (accessed >= 0) ? new AccessedExpiration.ForLong(value, accessed) : new AccessedExpiration.ForLong(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forDouble(final Expires expires, final double value) {
        switch(expires) {
        case NEVER: return new NeverExpires.ForDouble(value);
        case FIXED: return new FixedExpiration.ForDouble(value);
        case ACCESSED: return new AccessedExpiration.ForDouble(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forDouble(final Expires expires, final Expiration current) {
        final int accessed = current.getAccessed();
        final double value = ((DoubleHolder) current).value();
        
        switch(expires) {
        case NEVER: return new NeverExpires.ForDouble(value);
        case FIXED: return (accessed >= 0) ? new FixedExpiration.ForDouble(value, accessed) : new FixedExpiration.ForDouble(value);
        case ACCESSED: return (accessed >= 0) ? new AccessedExpiration.ForDouble(value, accessed) : new AccessedExpiration.ForDouble(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forObject(final Expires expires, final Object value) {
        switch(expires) {
        case NEVER: return new NeverExpires.ForObject(value);
        case FIXED: return new FixedExpiration.ForObject(value);
        case ACCESSED: return new AccessedExpiration.ForObject(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }

    public static Expiration forObject(final Expires expires, final Expiration current) {
        final int accessed = current.getAccessed();
        final Object value = ((ObjectHolder) current).value();
        
        switch(expires) {
        case NEVER: return new NeverExpires.ForObject(value);
        case FIXED: return (accessed >= 0) ? new FixedExpiration.ForObject(value, accessed) : new FixedExpiration.ForObject(value);
        case ACCESSED: return (accessed >= 0) ? new AccessedExpiration.ForObject(value, accessed) : new AccessedExpiration.ForObject(value);
        default:
            throw new IllegalArgumentException("Can't handle expiration: " + expires);
        }
    }
}
