package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to retrieve the users car.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface GetUserCarUseCase extends Interactor {

    interface Callback{
        void onCarRetrieved(Car car, Dealership dealership, boolean isLocal);
        void onNoCarSet(boolean isLocal);
        void onError(RequestError error);
    }

    //Execute the use case
    void execute(Integer carId, Repository.DATABASE_TYPE type, Callback callback);
}
