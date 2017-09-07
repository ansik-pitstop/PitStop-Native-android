package com.pitstop.interactors.update;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

/**
 * Created by Karol Zdebel on 9/7/2017.
 */

public interface UpdateCarMileageUseCase extends Interactor {
    interface Callback{
        void onMileageUpdated();
        void onNoCarAdded();
        void onError(RequestError error);
    }

    void execute(Callback callback, double mileage);
}
