package com.pitstop.interactors.add;

import com.pitstop.EventBus.EventSource;
import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-07-31.
 */

public interface AddCustomServiceUseCase extends Interactor {
    interface Callback{
        void onIssueAdded(CarIssue data);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(CarIssue issue, EventSource eventSource, Callback callback);
}
