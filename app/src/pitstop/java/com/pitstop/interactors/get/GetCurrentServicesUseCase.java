package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetCurrentServicesUseCase extends Interactor {
    interface Callback{
        void onGotCurrentServices(List<CarIssue> currentServices);
        void onError();
    }

    //Executes usecase
    void execute(Callback callback);
}
