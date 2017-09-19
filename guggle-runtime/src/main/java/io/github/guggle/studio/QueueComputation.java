package io.github.guggle.studio;

import java.util.function.BiFunction;
import java.util.Queue;

public class QueueComputation implements BiFunction<Object, Queue<Object>, Queue<Object>> {

    private Object found;
    
    public Queue<Object> apply(final Object key, final Queue<Object> queue) {
        this.found = queue.poll();
        return null;
    }

    public Object getFound() {
        return found;
    }

    private static final ThreadLocal<QueueComputation> _tl = ThreadLocal.withInitial(QueueComputation::new);

    public static QueueComputation threadLocal() {
        return _tl.get();
    }
}
