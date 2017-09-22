package io.github.guggle.utils;

import spock.lang.*;

class AtomicBitSetSpec extends Specification {

    def 'basic set if unset ops'() {
        setup:
        def bitSet = new AtomicBitSet(32);

        expect:
        bitSet.setIfUnset(15);
        bitSet.get(15);
        !bitSet.setIfUnset(15);
    }

    def 'basic clear if set ops'() {
        setup:
        def bitSet = new AtomicBitSet(99);

        expect:
        bitSet.setIfUnset(77);
        bitSet.get(77);
        bitSet.clearIfSet(77);
        !bitSet.clearIfSet(77);
    }

    def 'test array index of bounds'() {
        setup:
        def bitSet = new AtomicBitSet(128);

        when:
        bitSet.setIfUnset(-1);

        then:
        thrown(ArrayIndexOutOfBoundsException);

        when:
        bitSet.setIfUnset(128);

        then:
        thrown(ArrayIndexOutOfBoundsException);
    }

    def 'test basic properties'() {
        setup:
        def single = new AtomicBitSet(9);
        def large = new AtomicBitSet(257);

        expect:
        single.max == 9;
        single.length == 1;
        large.max == 257;
        large.length == 5;
    }

    def 'test next clear bit'() {
        setup:
        def bitSet = new AtomicBitSet(3);

        expect:
        bitSet.nextClearBit(0) == 0;
        bitSet.setIfUnset(0);
        bitSet.setIfUnset(1);
        bitSet.setIfUnset(2);
        bitSet.nextClearBit(0) == 3;
    }
}
