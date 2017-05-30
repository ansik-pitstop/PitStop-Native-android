package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 5/30/2017.
 *
 *  This interface represents a execution unit for a use case to mark a service as done.
 * By convention this use case (Interactor) implementation will return the result using a Callback.
 * That callback should be executed in the UI thread.
 */

public interface MarkServiceDoneUseCase extends Interactor {
    interface Callback{
        void onServiceMarkedAsDone();
        void onError();
    }

    //Executes the use case
    void execute(int userId, int issueId, Callback callback);
}
