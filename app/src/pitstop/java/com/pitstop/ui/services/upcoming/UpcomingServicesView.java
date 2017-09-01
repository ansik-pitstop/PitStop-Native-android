package com.pitstop.ui.services.upcoming;

import com.pitstop.models.service.UpcomingService;
import com.pitstop.ui.ErrorHandlingView;
import com.pitstop.ui.LoadingTabView;

import java.util.List;
import java.util.Map;

/**
 * Created by Karol Zdebel on 8/31/2017.
 */

public interface UpcomingServicesView extends LoadingTabView, ErrorHandlingView{
    void populateUpcomingServices(Map<Integer,List<UpcomingService>> upcomingServices);
    void displayNoServices();
    boolean isEmpty();
}
