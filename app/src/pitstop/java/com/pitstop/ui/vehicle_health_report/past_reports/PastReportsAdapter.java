package com.pitstop.ui.vehicle_health_report.past_reports;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.report.VehicleHealthReport;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/21/2017.
 */

public class PastReportsAdapter extends RecyclerView.Adapter<PastReportsAdapter.ReportViewHolder> {

    private List<VehicleHealthReport> vehicleHealthReports;
    private PastReportsView pastReportsView;

    public PastReportsAdapter(PastReportsView pastReportsView
            , List<VehicleHealthReport> vehicleHealthReports) {
        this.vehicleHealthReports = vehicleHealthReports;
        this.pastReportsView = pastReportsView;
    }

    @Override
    public ReportViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ReportViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_past_report, parent, false));

    }

    @Override
    public void onBindViewHolder(ReportViewHolder holder, int position) {
        holder.bind(vehicleHealthReports.get(position));
        holder.setOnClickListener(pastReportsView);
    }

    @Override
    public int getItemCount() {
        return vehicleHealthReports.size();
    }

    public class ReportViewHolder extends RecyclerView.ViewHolder{

        private TextView title;
        private TextView description;
        private TextView date;
        private ImageView icon;
        private VehicleHealthReport vehicleHealthReport;
        private View thisView;

        public ReportViewHolder(View itemView) {
            super(itemView);
            thisView = itemView;
            title = itemView.findViewById(R.id.title);
            description = itemView.findViewById(R.id.description);
            date = itemView.findViewById(R.id.date);
            icon = itemView.findViewById(R.id.icon);
        }

        public void bind(VehicleHealthReport vehicleHealthReport){
            this.vehicleHealthReport = vehicleHealthReport;
            title.setText("Vehicle Health Report");

            description.setText(String.format("Contains %d engine issues" +
                    ", %d services and %d recalls",vehicleHealthReport.getEngineIssues().size()
                    , vehicleHealthReport.getServices().size()
                    ,vehicleHealthReport.getRecalls().size()));
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");

            date.setText(simpleDateFormat.format(vehicleHealthReport.getDate()));

            if (vehicleHealthReport.getEngineIssues().size() > 0
                    || vehicleHealthReport.getRecalls().size() > 0
                    || vehicleHealthReport.getServices().size() > 0){

                icon.setImageDrawable(thisView.getContext()
                        .getResources().getDrawable(R.drawable.ic_report_unhealthy));
            }
        }

        public void setOnClickListener(PastReportsView callback){
            if (vehicleHealthReport != null){
                thisView.setOnClickListener(view1 -> callback.onReportClicked(vehicleHealthReport));
            }
        }
    }
}
