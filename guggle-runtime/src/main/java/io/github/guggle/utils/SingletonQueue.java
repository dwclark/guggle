package io.github.guggle.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SingletonQueue<T> extends ExemplarQueue<T> {

    private T contents;
    private final T exemplar;

    public SingletonQueue(final T val) {
        this.contents = val;
        this.exemplar = val;
    }

    public T exemplar() {
        return exemplar;
    }

    public int size() {
        return contents == null ? 0 : 1;
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private boolean moved = false;
            
            public boolean hasNext() {
                return !moved && contents != null;
            }

            public T next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                
                moved = true;
                return contents;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public T peek() {
        return contents;
    }

    public T poll() {
        final T ret = contents;
        contents = null;
        return ret;
    }

    public boolean offer(final T val) {
        if(val == null) {
            throw new IllegalArgumentException("Can't add null to queue");
        }

        if(contents == null) {
            contents = val;
            return true;
        }
        else {
            return false;
        }
    }
}
