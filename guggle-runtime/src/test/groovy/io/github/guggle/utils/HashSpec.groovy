package io.github.guggle.utils;

import spock.lang.*;

class HashSpec extends Specification {

    def 'basic calculations for fnv'() {
        setup:
        long val1 = Fnv.start().hashByte((byte) 0x61).finish() & 0xFFFF_FFFF;
        long val2 = Fnv.start().hashByte((byte) 0x61).hashByte((byte) 0x61).finish() & 0xFFFF_FFFF;
        long val3 = Fnv.start().hashShort((short) 0x6161).finish() & 0xFFFF_FFFF;
        long val4 = Fnv.start().hashInt(0x61626364).finish() & 0xFFFF_FFFF;
        long val5 = Fnv.start().hashLong(0x6162636465666768L).finish();
        
        expect:
        val1 == 0xe40c292cL;
        val2 == 0x4c250437L;
        val3 == 0x4c250437L;
        val4 == 0xce3479bdL;
        val5 == 0x76daaa8dL;
    }

    def 'basic calculations for jenkins one at a time'() {
        setup:
        long val1 = OneAtATime.start().hashByte((byte) 0x61).finish() & 0xFFFF_FFFF;
        long val2 = OneAtATime.start().hashByte((byte) 0x61).hashByte((byte) 0x61).finish() & 0xFFFF_FFFF;
        long val3 = OneAtATime.start().hashShort((short) 0x6161).finish() & 0xFFFF_FFFF;
        long val4 = OneAtATime.start().hashInt(0x61626364).finish() & 0xFFFF_FFFF;
        long val5 = OneAtATime.start().hashLong(0x6162636465666768L).finish();

        expect:
        val1 == 0xCA2E9442L;
        val2 == 0x7081738EL;
        val3 == 0x7081738EL;
        val4 == 0xCD8B6206L;
        val5 == 0x44D2D3E1L;
    }
}
