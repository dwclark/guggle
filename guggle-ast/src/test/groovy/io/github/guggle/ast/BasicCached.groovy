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

    void testKey(final Tuple tuple) {
        assert(!m.containsKey(tuple));
    }

    void place(final Tuple tuple, final Object v) {
        m[tuple] = v;
    }
}

@CompileStatic
class CacheValue1 extends TestCache {

    @Cache
    public String myToString(final int i) {
        def tuple = new Tuple(CacheValue1, 'myToString', i);
        def ret = i as String;
        println("Called myToString(${i})");
        testKey(tuple);
        place(tuple, ret);
        return ret;
    }

    @Cache
    public int intMultiplyBy2(final int i) {
        def tuple = new Tuple(CacheValue1, 'intMultiplyBy2', i);
        def ret = i * 2;
        println("Called intMultiplyBy2(${i})");
        testKey(tuple);
        place(tuple, ret);
        return ret;
    }

    @Cache
    public long longMultiplyBy2(final long i) {
        def tuple = new Tuple(CacheValue1, 'longMultiplyBy2', i);
        long ret = i * 2;
        println("Called longMultiplyBy2(${i})");
        testKey(tuple);
        place(tuple, ret);
        return ret;
    }
}
