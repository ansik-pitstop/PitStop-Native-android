package com.pitstop.interactors.get;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.report.FullReport;
import com.pitstop.network.RequestError;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public interface GetReportsUseCase extends Interactor {
    interface Callback {
        void onGotReports(List<FullReport> fullReports);
        void onError(RequestError error);
    }

    void execute(Callback callback);
}
