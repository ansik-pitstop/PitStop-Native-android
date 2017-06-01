package com.pitstop.interactors;

import com.pitstop.models.CarIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetDoneServicesUseCase extends Interactor {
    interface Callback{
        void onGotDoneServices(List<CarIssue> doneServices);
        void onError();
    }

    //Executes usecase
    void execute(Callback callback);
}
