package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by Matthew on 2017-07-18.
 */

public interface GetShopHoursUseCase extends Interactor {
    interface Callback{
        void onHoursGot(List<String> hours);
        void onNoHoursAvailable(List<String> defaultHours);
        void onNotOpen();
        void onError(RequestError error);
    }

    //Executes the use case
    void execute(int year, int month, int day, int shopId, String dayInWeek, Callback callback);
}
