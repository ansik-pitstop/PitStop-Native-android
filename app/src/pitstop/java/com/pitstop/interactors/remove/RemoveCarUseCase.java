package com.pitstop.interactors.remove;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to remove a car.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface RemoveCarUseCase extends Interactor {
    interface Callback{
        void onCarRemoved();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(Integer userId, Integer carId,String eventSource, Callback callback);

}
