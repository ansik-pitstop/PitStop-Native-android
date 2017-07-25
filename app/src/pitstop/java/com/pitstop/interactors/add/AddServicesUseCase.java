package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;

import java.util.List;

/**
 * Created by Matthew on 2017-07-17.
 */

public interface AddServicesUseCase extends Interactor {
    interface Callback{
        void onServicesAdded();
        void onError();
    }

    //Executes the use case
    void execute(List<CarIssue> carIssues,String eventSource, Callback callback);
}
