package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetCurrentServicesUseCase extends Interactor {
    interface Callback{
        void onGotCurrentServices(List<CarIssue> currentServices, List<CarIssue> customIssues);
        void onNoCarAdded();
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(Callback callback);
}
