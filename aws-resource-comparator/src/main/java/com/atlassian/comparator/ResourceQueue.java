package com.atlassian.comparator;

import java.util.ArrayDeque;
import java.util.Collection;

public class ResourceQueue<T> {
    private ArrayDeque<T> arrayDeque = new ArrayDeque();


    public ResourceQueue(ArrayDeque<T> arrayDeque) {
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

    public ArrayDeque<T> getAll() {
        return arrayDeque.clone();
    }

    public T poll() {
        return arrayDeque.poll();
    }
    //get, put, poll -> syhncronized
}
