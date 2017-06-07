package com.pitstop.interactors;

import com.pitstop.models.issue.UpcomingIssue;

import java.util.List;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetUpcomingServicesUseCase extends Interactor {
    interface Callback{
        void onGotUpcomingServices(List<UpcomingIssue> doneServices);
        void onError();
    }

    //Executes usecase
    void execute(Callback callback);
}
