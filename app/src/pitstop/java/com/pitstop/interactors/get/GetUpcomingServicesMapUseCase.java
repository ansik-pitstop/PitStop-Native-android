package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.service.UpcomingService;
import com.pitstop.network.RequestError;

import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetUpcomingServicesMapUseCase extends Interactor {
    interface Callback{
        void onGotUpcomingServicesMap(Map<Integer,List<UpcomingService>> serviceMap);
        void onError(RequestError error);
    }

    //Executes usecase
    void execute(Callback callback);
}
