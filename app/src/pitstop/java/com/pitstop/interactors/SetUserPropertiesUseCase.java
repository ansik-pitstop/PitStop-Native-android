package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to set the user properties.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface SetUserPropertiesUseCase {
    interface Callback{
        void onUserPropertiesSet();
        void onError();
    }

    //Executes the use case
    void execute(Callback callback);
}
