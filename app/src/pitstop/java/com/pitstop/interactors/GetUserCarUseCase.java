package com.pitstop.interactors;

import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to retrieve the users car.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface GetUserCarUseCase {
    interface Callback{
        void onCarRetrieved(Car car);
        void onNoCarSet();
        void onError();
    }

    //Execute the use case
    void execute(Callback callback);
}
