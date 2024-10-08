package com.pitstop.interactors.other;

import com.pitstop.EventBus.EventSource;
import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 *  This interface represents a execution unit for a use case to mark a service as done.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface MarkServiceDoneUseCase extends Interactor {
    interface Callback{
        void onServiceMarkedAsDone(CarIssue carIssue);
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(CarIssue carIssue, EventSource eventSource, Callback callback);
}
