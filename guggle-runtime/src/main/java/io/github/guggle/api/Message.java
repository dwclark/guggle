package io.github.guggle.api;

public interface Message extends Runnable {
    void resource(Object o);
    ActorId actorId();
}
