package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.issue.CarIssue;
import com.pitstop.network.RequestError;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by Karol Zdebel on 9/1/2017.
 */

public interface GetHistoryServicesSortedByDateUseCase extends Interactor {
    interface Callback{
        void onGotDoneServices(LinkedHashMap<String, ArrayList<CarIssue>> sortedIssues
                , ArrayList<String> headers);
        void onError(RequestError error);
    }
    void execute(Callback callback);
}
