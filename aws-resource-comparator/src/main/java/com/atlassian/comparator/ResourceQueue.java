package com.atlassian.comparator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ResourceQueue<T> {
    private final ConcurrentLinkedQueue<T> arrayDeque;
    private Boolean isFinishedPopulating = false;

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

    public void removeAll(Collection<T> collection) {
        arrayDeque.removeAll(collection);
    }

    public List<Object> getAll() {
        return new ArrayList<>(Arrays.asList(arrayDeque.toArray()));
    }

    public Boolean isFinishedPopulating() {
        return isFinishedPopulating;
    }

    public ResourceQueue<T> setFinishedPopulating(Boolean stoppedAdding) {
        isFinishedPopulating = stoppedAdding;
        return this;
    }
}
