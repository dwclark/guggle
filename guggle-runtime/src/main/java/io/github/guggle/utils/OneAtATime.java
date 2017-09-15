package io.github.guggle.utils;

import java.util.ArrayList;

final class OneAtATime {

    private final static int INIT = 0;

    private int hash;

    public OneAtATime hashByte(final byte val) {
        hash += val;
        hash += (hash << 10);
        hash ^= (hash >>> 6);
        return this;
    }

    public OneAtATime hashBoolean(final boolean b) {
        hashByte((byte) (b ? 1 : 0));
        return this;
    }

    public OneAtATime hashShort(final short s) {
        hashByte((byte) ((s >>> 8) & 0xFF));
        hashByte((byte) (s & 0xFF));
        return this;
    }
    
    public OneAtATime hashInt(final int i) {
        hashByte((byte) ((i >>> 24) & 0xFF));
        hashByte((byte) ((i >>> 16) & 0xFF));
        hashByte((byte) ((i >>> 8) & 0xFF));
        hashByte((byte) (i & 0xFF));
        return this;
    }

    public OneAtATime hashFloat(final float f) {
        hashInt(Float.floatToIntBits(f));
        return this;
    }

    public OneAtATime hashLong(final long l) {
        hashInt((int) ((l >>> 32) & 0xFFFF_FFFF));
        hashInt((int) (l & 0xFFFF_FFFF));
        return this;
    }

    public OneAtATime hashDouble(final double d) {
        hashLong(Double.doubleToLongBits(d));
        return this;
    }

    public OneAtATime hashObject(final Object o) {
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
        int hash = _tl.get().pop();
        hash += (hash << 3);
        hash ^= (hash >>> 11);
        hash += (hash << 15);
        return hash;
    }

    public static OneAtATime start() {
        return _tl.get().push();
    }

    private static final ThreadLocal<OneAtATimeStack> _tl = ThreadLocal.withInitial(OneAtATimeStack::new);
    
    private static class OneAtATimeStack {
        final ArrayList<OneAtATime> objects = new ArrayList<>();
        int index = -1;

        OneAtATime push() {
            ++index;
            if(index > (objects.size() - 1)) {
                objects.add(new OneAtATime());
            }

            OneAtATime object = objects.get(index);
            object.hash = index == 0 ? INIT : objects.get(index-1).hash;
            
            return object;
        }

        int pop() {
            final int ret = objects.get(index).hash;
            --index;
            return ret;
        }
    }
}
