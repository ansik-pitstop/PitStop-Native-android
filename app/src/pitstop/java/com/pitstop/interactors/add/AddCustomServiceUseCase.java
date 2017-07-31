package com.pitstop.interactors.add;

import com.pitstop.interactors.Interactor;

/**
 * Created by Matt on 2017-07-31.
 */

public interface AddCustomServiceUseCase extends Interactor {
    interface Callback{
        void onIssueAdded();
        void onError();
    }

    //Executes usecase
    void execute(int carId,String eventSource, Callback callback);
}
