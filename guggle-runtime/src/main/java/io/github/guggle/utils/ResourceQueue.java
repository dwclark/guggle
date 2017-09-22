package io.github.guggle.utils;

import java.util.PriorityQueue;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.function.Consumer;
import java.time.Instant;
import java.util.concurrent.ExecutorService;

public class ResourceQueue<T> extends ExemplarQueue<T> {
    
    private static class Node<NT> implements Comparable<Node<NT>> {
        private final Instant created;
        private final NT resource;
        
        public int compareTo(final Node<NT> rhs) {
            return created.compareTo(rhs.created);
        }

        public Node(final NT resource) {
            this.resource = resource;
            this.created = Instant.now();
        }

        public NT get() {
            return resource;
        }
    }

    private final T exemplar;
    private final PriorityQueue<Node<T>> ready;
    private final IdentityHashMap<Object,Node<T>> all;
    private final int max;
    private final Supplier<T> supplier;
    private final ExecutorService executor;
    private final Consumer<T> whenSupplied;

    private int waiters = 0;
    private final int maxWaiters;
    
    private void miss() {
        ++waiters;

        if(waiters >= maxWaiters && all.size() < max) {
            final Object o = new Object();
            all.put(o, null);
            executor.submit(() -> {
                    final T supplied = supplier.get();
                    whenSupplied.accept(supplied);
                    all.remove(o);
                });
        }
    }

    private void hit() {
        if(waiters > 0) {
            --waiters;
        }
    }

    private void reset() {
        waiters = 0;
    }
    
    public ResourceQueue(final int max, final int maxWaiters, final Supplier<T> supplier,
                         final ExecutorService executor, final Consumer<T> whenSupplied) {
        this.max = max;
        this.maxWaiters = maxWaiters;
        this.ready = new PriorityQueue<>(max);
        this.all = new IdentityHashMap<>(max);
        this.supplier = supplier;
        this.executor = executor;
        this.whenSupplied = whenSupplied;

        final T first = supplier.get();
        this.exemplar = first;
        placeNew(first);
    }

    public T exemplar() {
        return exemplar;
    }

    public int size() {
        return ready.size();
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {
            final Iterator<Node<T>> iter = ready.iterator();

            public boolean hasNext() {
                return iter.hasNext();
            }

            public T next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }

                return iter.next().get();
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public T peek() {
        final Node<T> node = ready.peek();
        return node != null ? node.get() : null;
    }

    public T poll() {
        final Node<T> node = ready.poll();
        if(node != null) {
            hit();
            final T ret = node.get();
            all.put(ret, node);
            return ret;
        }
        else {
            miss();
            return null;
        }
    }

    private void placeNew(final T val) {
        final Node<T> node = new Node<>(val);
        all.put(val, node);
        ready.offer(node);
    }

    public boolean offer(final T val) {
        if(ready.size() < max) {
            return false;
        }
        else {
            Node<T> node = all.get(val);
            if(!all.containsKey(val)) {
                placeNew(val);
            }
            else {
                ready.offer(node);
            }
            
            return true;
        }
    }
}
