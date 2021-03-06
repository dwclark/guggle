package io.github.guggle.utils;

import java.util.ArrayList;

final public class Fnv {

    public static final int OFFSET = (int) (0xFFFF_FFFF & 2_166_136_261L);
    public static final int PRIME = 16_777_619;

    private int hash;

    public Fnv hashByte(final byte b) {
        hash = hash ^ b;
        hash = hash * PRIME;
        return this;
    }

    public Fnv hashBoolean(final boolean b) {
        hashByte((byte) (b ? 1 : 0));
        return this;
    }

    public Fnv hashShort(final short s) {
        hashByte((byte) ((s >>> 8) & 0xFF));
        hashByte((byte) (s & 0xFF));
        return this;
    }

    public Fnv hashInt(final int i) {
        hashByte((byte) ((i >>> 24) & 0xFF));
        hashByte((byte) ((i >>> 16) & 0xFF));
        hashByte((byte) ((i >>> 8) & 0xFF));
        hashByte((byte) (i & 0xFF));
        return this;
    }

    public Fnv hashFloat(final float f) {
        hashInt(Float.floatToIntBits(f));
        return this;
    }

    public Fnv hashLong(final long l) {
        hashInt((int) ((l >>> 32) & 0xFFFF_FFFF));
        hashInt((int) (l & 0xFFFF_FFFF));
        return this;
    }

    public Fnv hashDouble(final double d) {
        hashLong(Double.doubleToLongBits(d));
        return this;
    }

    public Fnv hashObject(final Object o) {
        if(o == null) {
            return this;
        }
        
        if(o instanceof Number) {
            if(o instanceof Byte) {
                hashByte((Byte) o);
            }
            else if(o instanceof Short) {
                hashShort((Short) o);
            }
            else if(o instanceof Integer) {
                hashInt((Integer) o);
            }
            else if(o instanceof Long) {
                hashLong((Long) o);
            }
            else if(o instanceof Float) {
                hashFloat((Float) o);
            }
            else if(o instanceof Double) {
                hashDouble((Double) o);
            }
            else {
                hashInt(o.hashCode());
            }
        }
        else if(o instanceof Boolean) {
            hashBoolean((Boolean) o);
        }
        else {
            hashInt(o.hashCode());
        }
        
        return this;
    }

    public int finish() {
        return _tl.get().pop();
    }

    public static Fnv start() {
        return _tl.get().push();
    }

    private static final ThreadLocal<FnvStack> _tl = ThreadLocal.withInitial(FnvStack::new);
    
    private static class FnvStack {
        final ArrayList<Fnv> fnvs = new ArrayList<>();
        int index = -1;

        Fnv push() {
            ++index;
            if(index > (fnvs.size() - 1)) {
                fnvs.add(new Fnv());
            }

            Fnv fnv = fnvs.get(index);
            fnv.hash = (index == 0) ? OFFSET : fnvs.get(index-1).hash;
            
            return fnv;
        }

        int pop() {
            final int ret = fnvs.get(index).hash;
            --index;
            return ret;
        }
    }
}
