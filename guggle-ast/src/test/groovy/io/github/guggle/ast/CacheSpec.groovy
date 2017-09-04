package io.github.guggle.ast;

import spock.lang.*;

class CacheSpec extends Specification {

    def basic() {
        setup:
        def basic = new BasicCached();
        basic.first(1, '2', ["1": [1,2,3] ]);
        basic.firstCopied(1, '2', ["1": [1,2,3]]);
        null;
    }
}
