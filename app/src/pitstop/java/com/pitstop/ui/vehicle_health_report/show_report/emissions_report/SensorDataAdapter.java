package com.pitstop.ui.vehicle_health_report.show_report.emissions_report;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by Karol Zdebel on 1/29/2018.
 */

public class SensorDataAdapter extends RecyclerView.Adapter<SensorDataAdapter.SensorDataHolder> {

    private final String TAG = SensorDataAdapter.class.getSimpleName();
    private LinkedHashMap<String,String> sensors;

    public SensorDataAdapter(LinkedHashMap<String,String> sensors){
        this.sensors = sensors;
    }

    @Override
    public SensorDataHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG,"onCreateViewHolder()");
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
        private final String TAG = SensorDataHolder.class.getSimpleName();

        private TextView sensorName;
        private TextView sensorValue;

        public SensorDataHolder(View itemView) {
            super(itemView);
            sensorName = itemView.findViewById(R.id.sensor_name);
            sensorValue = itemView.findViewById(R.id.sensor_value);
        }

        public void bindView(int pos){
            Log.d(TAG,"bindView() pos: "+pos);
            int curPos = 0;
            Iterator<Map.Entry<String,String>> iterator = sensors.entrySet().iterator();
            while(iterator.hasNext() && curPos <= pos){
                Map.Entry<String,String> entry = iterator.next();
                if (curPos == pos){
                    sensorName.setText(entry.getKey());
                    sensorValue.setText(entry.getValue());
                }
                curPos++;
            }
        }
    }

}
