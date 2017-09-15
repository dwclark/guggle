package io.github.guggle.ast;

import spock.lang.*;

class CacheSpec extends Specification {

    def 'basic object caching'() {
        setup:
        def cv1 = new CacheValue1();

        expect:
        cv1.myToString(1) == "1";
        cv1.myToString(1) == "1";
    }

    def 'basic int caching'() {
        setup:
        def o = new CacheValue1();

        expect:
        o.intMultiplyBy2(1) == 2;
        o.intMultiplyBy2(1) == 2;
    }

    def 'basic long caching'() {
        setup:
        def o = new CacheValue1();

        expect:
        o.longMultiplyBy2(1L) == 2L;
        o.longMultiplyBy2(1L) == 2L;
    }

    def 'basic double caching'() {
        setup:
        def o = new CacheValue1();

        expect:
        o.doubleMultiplyBy2(1.0d) == 2.0d;
        o.doubleMultiplyBy2(1.0d) == 2.0d
    }
}
