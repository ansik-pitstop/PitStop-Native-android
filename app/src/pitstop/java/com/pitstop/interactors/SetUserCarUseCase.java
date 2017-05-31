package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to set the users current car.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface SetUserCarUseCase extends Interactor {
    interface Callback{
        void onUserCarSet();
        void onError();
    }

    //Executes the use case
    void execute(int userId,int carId, Callback callback);
}
