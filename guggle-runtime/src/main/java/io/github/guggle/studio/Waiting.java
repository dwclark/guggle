package io.github.guggle.studio;

import java.util.concurrent.atomic.AtomicLong;
import io.github.guggle.api.Message;

public class Waiting implements Comparable<Waiting> {

    enum Type {
        RESOURCE(0), MESSAGE(1);

        private Type(final int id) {
            this.id = id;
        }

        final int id;
    }
    
    private static final AtomicLong counter = new AtomicLong();

    private final long messageId;
    private final Type type;
    private final Object o;
    
    public int compareTo(final Waiting rhs) {
        int ret = Integer.compare(type.id, rhs.type.id);
        if(ret != 0) {
            return ret;
        }

        return Long.compare(messageId, rhs.messageId);
    }

    public Type type() {
        return type;
    }

    public Waiting(final Object o) {
        this.messageId = counter.incrementAndGet();
        this.type = Type.RESOURCE;
        this.o = o;
    }

    public Waiting(final Message m) {
        this.messageId = counter.incrementAndGet();
        this.type = Type.MESSAGE;
        this.o = m;
    }
}
