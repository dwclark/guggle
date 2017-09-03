package io.github.guggle.api;

import java.util.List;
import java.util.Collections;
import io.github.guggle.utils.Fnv;

public final class MethodId {
    private final Class type;
    private final String method;
    private final List<Class> args;

    public MethodId(final Class type, final String method, final List<Class> args) {
        this.type = type;
        this.method = method;
        this.args = Collections.unmodifiableList(args);
    }

    public Class getType() {
        return type;
    }

    public String getMethod() {
        return method;
    }

    public List<Class> getArgs() {
        return args;
    }

    public boolean partialMatch(final String type, final String method) {
        return type.equals(this.type) && method.equals(this.method);
    }

    @Override
    public boolean equals(final Object o) {
        if(!(o instanceof MethodId)) {
            return false;
        }

        final MethodId rhs = (MethodId) o;
        return (type.equals(rhs.type) &&
                method.equals(rhs.method) &&
                args.equals(rhs.args));
    }

    @Override
    public int hashCode() {
        return Fnv.start().hashObject(type).hashObject(method).hashCollection(args).finish();
    }
}
