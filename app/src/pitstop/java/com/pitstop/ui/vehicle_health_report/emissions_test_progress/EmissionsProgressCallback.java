package com.pitstop.ui.vehicle_health_report.emissions_test_progress;

import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-14.
 */

public interface EmissionsProgressCallback {
     void end();
     void setViewReport(JSONObject emissions);
}
