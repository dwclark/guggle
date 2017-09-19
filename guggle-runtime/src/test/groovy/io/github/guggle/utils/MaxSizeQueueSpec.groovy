package io.github.guggle.utils;

import spock.lang.*;
import java.util.function.Supplier;

class MaxSizeQueueSpec extends Specification {

    def 'test fills up correctly'() {
        setup:
        int idx = 0;
        def supplier = { -> return idx++; } as Supplier;
        def q = new MaxSizeQueue(5, supplier);
        def iter = q.iterator();
        
        expect:
        !q.offer(6);
        iter.next() == 0;
        iter.next() == 1;
        iter.next() == 2;
        iter.next() == 3;
        iter.next() == 4;
        !iter.hasNext();
    }

    def 'test offer and poll'() {
        setup:
        def q = new MaxSizeQueue(3);
        def iter;
        
        expect:
        q.offer(1);

        q.offer(2);
        q.offer(3);
        !q.offer(4);
        q.poll() == 1;
        q.poll() == 2;
        q.offer(4);
        q.offer(5);
        !q.offer(6);
        (iter = q.iterator()) != null;
        iter.next() == 3;
        iter.next() == 4;
        iter.next() == 5;
        !iter.hasNext();
        q.size() == 3;
        q.poll() == 3;
        q.size() == 2;
        q.poll() == 4;
        q.size() == 1;
        q.poll() == 5;
        q.size() == 0;
        !q.poll();
        (iter = q.iterator()) != null;
        !iter.hasNext();
    }

    def 'test peek'() {
        setup:
        def q = new MaxSizeQueue(5);

        expect:
        q.peek() == null;
        q.offer(1);
        q.peek() == 1;
        q.poll();
        q.peek() == null;
    }
}
