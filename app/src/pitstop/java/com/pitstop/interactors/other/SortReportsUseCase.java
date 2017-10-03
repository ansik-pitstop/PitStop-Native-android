package com.pitstop.interactors.other;

import com.pitstop.interactors.Interactor;
import com.pitstop.models.report.FullReport;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/22/2017.
 */

public interface SortReportsUseCase extends Interactor {

    enum SortType{ DATE_NEW, DATE_OLD, ENGINE_ISSUE, SERVICE, RECALL }

    interface Callback{
        void onSorted(List<FullReport> reports);
    }

    void execute(List<FullReport> reports, SortType sortType
            , Callback callback);
}
