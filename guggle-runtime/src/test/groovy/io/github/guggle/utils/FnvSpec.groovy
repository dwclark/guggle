package io.github.guggle.utils;

import spock.lang.*;

class FnvSpec extends Specification {

    def 'zeros gives init'() {
        expect:
        Fnv.start().hashByte((byte) 0).finish() == (Fnv.INIT * Fnv.MIX);
    }
}
