package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Karol Zdebel on 1/29/2018.
 */

public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.SensorDataHolder> {

    private LinkedHashMap<String,String> sensors;

    public SensorDataAdapter(LinkedHashMap<String,String> sensors){
        this.sensors = sensors;
    }

    @Override
    public SensorDataHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SensorDataHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_emission_result_sensor, parent, false));
    }

    @Override
    public void onBindViewHolder(SensorDataHolder holder, int position) {
        holder.bindView(position);
    }

    @Override
    public int getItemCount() {
        return sensors.size();
    }

    class SensorDataHolder extends RecyclerView.ViewHolder{

        private TextView sensorName;
        private TextView sensorValue;

        public SensorDataHolder(View itemView) {
            super(itemView);
            sensorName = itemView.findViewById(R.id.sensor_name);
            sensorValue = itemView.findViewById(R.id.sensor_value);
        }

        public void bindView(int pos){
            int curPos = 0;
            while(sensors.entrySet().iterator().hasNext() && curPos <= pos){
                Map.Entry<String,String> entry = sensors.entrySet().iterator().next();
                if (curPos == pos){
                    sensorName.setText(entry.getKey());
                    sensorValue.setText(entry.getValue());
                }
                curPos++;
            }
        }
    }

}
