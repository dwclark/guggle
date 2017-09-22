package io.github.guggle.utils;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

public class MaxSizeQueue<T> extends ExemplarQueue<T> {

    private final T exemplar;
    private Object[] contents;
    private int pollAt;
    private int offerAt;
    private int max;

    public MaxSizeQueue(final int max, final T val) {
        this.max = max;
        this.exemplar = val;
        this.contents = new Object[max];
    }

    public MaxSizeQueue(final int max, final Supplier<T> supplier) {
        this.max = max;
        this.contents = new Object[max];
        for(int i = 0; i < max; ++i) {
            _offer(supplier.get());
        }

        this.exemplar = (T) contents[0];
    }

    public T exemplar() {
        return exemplar;
    }
    
    public int size() {
        if(pollAt < offerAt) {
            return offerAt - pollAt;
        }
        else if(pollAt > offerAt) {
            return (max - pollAt) + offerAt;
        }
        else if(contents[offerAt] != null) {
            return max;
        }
        else {
            return 0;
        }
    }

    private T at(final int idx) {
        return (T) contents[idx];
    }

    private boolean isFull() {
        return (offerAt == pollAt) && contents[offerAt] != null;
    }

    private int nextIndex(final int idx) {
        return ((idx + 1) != max) ? idx + 1 : 0;
    }

    private int previousIndex(final int idx) {
        return idx == 0 ? max - 1 : idx - 1;
    }
    
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            int left = size();
            int idx = previousIndex(pollAt);

            public boolean hasNext() {
                return left != 0;
            }

            public T next() {
                if(!hasNext()) {
                    throw new NoSuchElementException();
                }
                
                --left;
                idx = nextIndex(idx);
                return at(idx);
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    public T peek() {
        return at(pollAt);
    }

    public T poll() {
        if(pollAt != offerAt || isFull()) {
            final T ret = at(pollAt);
            contents[pollAt] = null;
            pollAt = nextIndex(pollAt);
            return ret;
        }
        else {
            return null;
        }
    }

    private boolean _offer(final T o) {
        if(o == null) {
            throw new IllegalArgumentException("Can't add null to queue");
        }
        
        if(isFull()) {
            return false;
        }
        else {
            contents[offerAt] = o;
            offerAt = nextIndex(offerAt);
            return true;
        }
    }
    
    public boolean offer(final T o) {
        return _offer(o);
    }
}
