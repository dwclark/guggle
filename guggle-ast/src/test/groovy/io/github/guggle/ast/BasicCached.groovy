package io.github.guggle.ast;

import groovy.transform.CompileStatic;
import groovy.transform.Immutable;

@Immutable @CompileStatic
class BasicValue {
    String one;
    int two;
}

class TestCache {
    final Map m = [:];

    void testKey(final Object k) {
        assert(!m.containsKey(k));
    }

    void place(final Object k, final Object v) {
        m[k] = v;
    }
}

@CompileStatic
class CacheValue1 extends TestCache {

    @Cache
    public String myToString(final int i) {
        println("Called myToString(${i})");
        testKey(i);
        place(i, i as String);
        return i as String;
    }
}
