package io.github.guggle.ast;

import spock.lang.*;

class CacheSpec extends Specification {

    def basic() {
        setup:
        def cv1 = new CacheValue1();

        expect:
        cv1.myToString(1) == "1";
        cv1.myToString(1) == "1";
    }
}
