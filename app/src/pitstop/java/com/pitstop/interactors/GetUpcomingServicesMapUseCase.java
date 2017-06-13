package com.pitstop.interactors;

import com.pitstop.models.service.UpcomingService;

import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 5/31/2017.
 */

public interface GetUpcomingServicesMapUseCase extends Interactor {
    interface Callback{
        void onGotUpcomingServicesMap(Map<Integer,List<UpcomingService>> serviceMap);
        void onError();
    }

    //Executes usecase
    void execute(Callback callback);
}
