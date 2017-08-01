package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CustomIssue;
import com.pitstop.network.RequestError;

/**
 * Created by Matt on 2017-07-31.
 */

public interface AddCustomServiceUseCase extends Interactor {
    interface Callback{
        void onIssueAdded();
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(CustomIssue issue, String eventSource, Callback callback);
}
