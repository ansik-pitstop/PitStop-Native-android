package com.pitstop.ui;

/**
 * Created by Karol Zdebel on 9/5/2017.
 */

public interface Presenter<T> {
    void subscribe(T view);
    void unsubscribe();

}
