package com.pitstop.observer;

/**
 * Created by Karol Zdebel on 6/28/2017.
 */

public interface Subject {
    void subscribe(Observer observer);
    void unsubscribe(Observer observer);
}
