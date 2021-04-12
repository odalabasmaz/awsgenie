package com.atlassian.comparator;

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResourceQueue<T> {
    private final ConcurrentLinkedQueue<T> arrayDeque;

    public ResourceQueue() {
        this.arrayDeque = new ConcurrentLinkedQueue<>();
    }

    public ResourceQueue(ConcurrentLinkedQueue<T> arrayDeque) {
        this.arrayDeque = arrayDeque;
    }

    public T peek() {
        return arrayDeque.peek();
    }

    public boolean add(T t) {
        return arrayDeque.add(t);
    }

    public boolean addAll(Collection<T> collection) {
        return arrayDeque.addAll(collection);
    }

    public ConcurrentLinkedQueue<T> getAll() {
        return arrayDeque.clone();
    }

    public T poll() {
        return arrayDeque.poll();
    }
    //get, put, poll -> syhncronized
}
