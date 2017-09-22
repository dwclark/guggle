package io.github.guggle.utils;

import java.util.concurrent.atomic.AtomicLongArray;

final public class AtomicBitSet {
    
    private static final int BITS_PER_WORD = 64;
    private static final long MASK = 0xFFFF_FFFF_FFFF_FFFFL;
    private final int length;
    private final AtomicLongArray array;
    private final int max;
    
    public AtomicBitSet(final int max) {
        this.max = max;
        this.length = 1 + ((max - 1) >>> 6);
        this.array = new AtomicLongArray(length);
    }

    public int getMax() {
        return max;
    }

    private void checkLowIndex(final int bitIndex) {
        if(bitIndex < 0) {
            throw new ArrayIndexOutOfBoundsException("bitIndex cannot be negative");
        }
    }

    private void checkHighIndex(final int bitIndex) {
        if(bitIndex >= max) {
            throw new ArrayIndexOutOfBoundsException("bitIndex cannot be above " + max + ", you passed: " + bitIndex);
        }
    }

    public int nextClearBit(final int bitIndex) {
        checkLowIndex(bitIndex);

        if(bitIndex >= length) {
            return bitIndex;
        }

        int arrayIndex = bitIndex >>> 6;
        long word = ~array.get(arrayIndex) & (MASK << bitIndex);

        while(true) {
            if(word != 0) {
                return (arrayIndex * BITS_PER_WORD) + Long.numberOfTrailingZeros(word);
            }
            
            if(++arrayIndex == length) {
                return length * BITS_PER_WORD;
            }
            
            word = ~array.get(arrayIndex);
        }
    }

    public boolean setIfUnset(final int bitIndex) {
        checkLowIndex(bitIndex);
        checkHighIndex(bitIndex);

        final long mask = (1L << bitIndex);
        final int arrayIndex = bitIndex >>> 6;
        final long prev = array.get(arrayIndex);

        if((mask & prev) == 0) {
            final long newVal = prev | (1L << bitIndex);
            return array.compareAndSet(arrayIndex, prev, newVal);
        }
        else {
            return false;
        }
    }

    public boolean clearIfSet(final int bitIndex) {
        checkLowIndex(bitIndex);
        checkHighIndex(bitIndex);

        final long mask = (1L << bitIndex);
        final int arrayIndex = bitIndex >> 6;
        final long prev = array.get(arrayIndex);

        if((mask & prev) != 0) {
            final long newVal = prev & ~mask;
            return array.compareAndSet(arrayIndex, prev, newVal);
        }
        else {
            return false;
        }
    }

    public boolean get(final int bitIndex) {
        checkLowIndex(bitIndex);
        checkHighIndex(bitIndex);
        
        final long mask = (1L << bitIndex);
        final int arrayIndex = bitIndex >>> 6;
        final long extracted = array.get(arrayIndex) & mask;
        return extracted != 0;
    }
}
