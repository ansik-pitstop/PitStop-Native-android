package com.pitstop.interactors;

import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to add a car.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface AddCarUseCase extends Interactor{
    interface Callback{
        void onCarAdded();
        void onError();
    }

    //Executes usecase
    void execute(Car car,String eventSource, Callback callback);
}
