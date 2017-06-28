package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface Subject<T> {
    void subscribe(T observer);
    void unsubscribe(T observer);
}
