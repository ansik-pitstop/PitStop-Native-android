package com.pitstop.interactors;

import com.pitstop.models.issue.CarIssue;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 * This interface represents a execution unit for a use case to requesting a service.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface AddServiceUseCase extends Interactor {
    interface Callback{
        void onServiceRequested();
        void onError();
    }

    //Executes the use case
    void execute(CarIssue carIssue, Callback callback);
}
