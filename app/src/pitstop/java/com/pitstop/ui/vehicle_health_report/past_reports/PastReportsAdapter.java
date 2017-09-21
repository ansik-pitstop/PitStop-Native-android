package com.pitstop.ui.vehicle_health_report.past_reports;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.pitstop.models.report.VehicleHealthReport;

import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsAdapter extends RecyclerView.Adapter {

    private List<VehicleHealthReport> vehicleHealthReports;
    private PastReportsView pastReportsView;

    public PastReportsAdapter(PastReportsView pastReportsView
            , List<VehicleHealthReport> vehicleHealthReports) {
        this.vehicleHealthReports = vehicleHealthReports;
        this.pastReportsView = pastReportsView;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
