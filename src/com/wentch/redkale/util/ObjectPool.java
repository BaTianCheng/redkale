/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.wentch.redkale.util;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.logging.*;

/**
 *
 * @author zhangjx
 * @param <T>
 */
public final class ObjectPool<T> {

    private static final Logger logger = Logger.getLogger(ObjectPool.class.getSimpleName());

    private final boolean debug;

    private final Queue<T> queue;

    private Creator<T> creator;

    private final Predicate<T> recycler;

    private final AtomicLong creatCounter;

    private final AtomicLong cycleCounter;

    public ObjectPool(Class<T> clazz, Predicate<T> recycler) {
        this(2, clazz, recycler);
    }

    public ObjectPool(int max, Class<T> clazz, Predicate<T> recycler) {
        this(max, Creator.create(clazz), recycler);
    }

    public ObjectPool(Creator<T> creator, Predicate<T> recycler) {
        this(2, creator, recycler);
    }

    public ObjectPool(int max, Creator<T> creator, Predicate<T> recycler) {
        this(null, null, max, creator, recycler);
    }

    public ObjectPool(AtomicLong creatCounter, AtomicLong cycleCounter, int max, Creator<T> creator, Predicate<T> recycler) {
        this.creatCounter = creatCounter;
        this.cycleCounter = cycleCounter;
        this.creator = creator;
        this.recycler = recycler;
        this.queue = new ArrayBlockingQueue<>(Math.max(Runtime.getRuntime().availableProcessors() * 2, max));
        this.debug = logger.isLoggable(Level.FINER);
    }

    public void setCreator(Creator<T> creator) {
        this.creator = creator;
    }

    public T poll() {
        T result = queue.poll();
        if (result == null) {
            if (creatCounter != null) creatCounter.incrementAndGet();
            result = this.creator.create();
        }
        return result;
    }

    public void offer(final T e) {
        if (e != null && recycler.test(e)) {
            if (cycleCounter != null) cycleCounter.incrementAndGet();
            if (debug) queue.forEach(t -> {
                if (t == e) logger.log(Level.WARNING, "repeat offer the same object(" + e + ")", new Exception());
            });
            queue.offer(e);
        }
    }

    public long getCreatCount() {
        return creatCounter.longValue();
    }

    public long getCycleCount() {
        return cycleCounter.longValue();
    }
}