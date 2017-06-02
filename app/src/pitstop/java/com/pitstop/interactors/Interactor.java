package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * Common interface for an Interactor declared in the application.
 * This interface represents a execution unit for different use cases (this means any use case
 * in the application should implement this contract).
 *
 * By convention each Interactor implementation will return the result using a Callback that should
 * be executed in the UI thread.
 *
 */

public interface Interactor extends Runnable{
    void run();

}
