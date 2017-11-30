package com.pitstop.adapters;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Alarm;
import com.pitstop.ui.alarms.AlarmsView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Created by ishan on 2017-10-30.
 */

public class AlarmsAdapter extends RecyclerView.Adapter<AlarmsAdapter.AlarmViewHolder> {
    public static final String TAG = AlarmsAdapter.class.getSimpleName();
    private LinkedHashMap<String, ArrayList<Alarm>> alarmList;
    public boolean isDealershipMercedes = false;
    private AlarmsView alarmsView;
    Context ctx;
    public AlarmsAdapter(HashMap<String, ArrayList<Alarm>> map, Context context, AlarmsView alarmsView){
        Log.d(TAG, "AlarmsAdapter");
        this.alarmList = new LinkedHashMap<>(map);
        this.ctx = context;
        this.alarmsView = alarmsView;
    }

    public void setAlarmList(LinkedHashMap<String, ArrayList<Alarm>> list){
        this.alarmList = list;
    }


    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder");
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_day_alarms, parent, false);
        AlarmsAdapter.AlarmViewHolder alarmViewHolder = new AlarmViewHolder(view);
        return  alarmViewHolder;
    }

    @Override
    public void onBindViewHolder(AlarmViewHolder holder, int position) {
        Log.d(TAG, "onBindVIewHolder");
        holder.bind(alarmList.get(alarmList.keySet().toArray()[position]), ctx,(String) alarmList.keySet().toArray()[position] , isDealershipMercedes);

    }
    public void setDealershipMercedes(boolean dealershipMercedes) {
        isDealershipMercedes = dealershipMercedes;
    }


    @Override
    public int getItemCount() {
        return alarmList.size();
    }


    @Override
    public int getItemViewType(int position){
        return position;
    }

    public class AlarmViewHolder extends RecyclerView.ViewHolder{

       TextView dateView;
       RecyclerView recyclerView;

        public AlarmViewHolder(View itemView) {
            super(itemView);
            Log.d("alarmViewHolder", "ALarmViewholder:); ");
            this.dateView = itemView.findViewById(R.id.alarm_date);
            this.recyclerView  = itemView.findViewById(R.id.alarms_rec_view);
        }

        public void bind(List<Alarm> alarmList,Context context,  String date, boolean isDealershipMercedes){
            Log.d(TAG, date);
            dateView.setText(date);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            dayAlarmAdapter  adapter = new dayAlarmAdapter(alarmList, isDealershipMercedes);
            recyclerView.setAdapter(adapter);
        }
    }


    private class dayAlarmAdapter extends RecyclerView.Adapter<dayAlarmAdapter.alarmView>{

        List<Alarm> alarmList;
        Boolean isDealershipMercedes = false;

        public dayAlarmAdapter(List<Alarm> alarms, boolean mercedes){
            Log.d(TAG, "dayAlarmAdapter");
            this.alarmList = alarms;
            this.isDealershipMercedes = mercedes;

        }
        @Override
        public alarmView onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.d(TAG, "onSubCreateVIewHolder");
            View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_alarm, parent, false);
            alarmView alarmView = new alarmView(view);
            view.setOnClickListener(v->{
                alarmsView.onAlarmClicked(alarmList.get(viewType));
            });
            return  alarmView;
        }

        @Override
        public void onBindViewHolder(alarmView holder, int position) {
            Log.d(TAG, "onSubCreatebindHolder");
            holder.bind(alarmList.get(position),isDealershipMercedes );


        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        @Override
        public int getItemCount() {
            return alarmList.size();
        }

        class alarmView extends RecyclerView.ViewHolder{

            ImageView alarmIcon;
            TextView alarmName;
            TextView alarmTime;

            public alarmView(View itemView) {
                super(itemView);
                this.alarmIcon = itemView.findViewById(R.id.alarm_icon);
                this.alarmName = itemView.findViewById(R.id.alarm_name);
                this.alarmTime  = itemView.findViewById(R.id.alarm_time);
            }

            public void bind(Alarm alarm, boolean isDealershipMercedes){
                Log.d(TAG, alarm.getName());
                alarmIcon.setImageResource(getAlarmIcon(alarm.getEvent(), isDealershipMercedes));
                alarmName.setText(alarm.getName());
                Log.d(TAG, Boolean.toString(isDealershipMercedes));
                Date date = new Date ();
                date.setTime(Long.parseLong(alarm.getRtcTime())*1000);
                SimpleDateFormat jdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
                String java_date = jdf.format(date).substring(11, 19);
                // changing millitary time to normal time
                int hour = Integer.valueOf(java_date.substring(0,2));
                if (hour>12){
                    java_date = Integer.toString(hour-12) + java_date.substring(2);
                }
                alarmTime.setText(java_date);
            }
        }
    }


    public int getAlarmIcon(int alarmEvent, boolean mercedes){
        if (mercedes){
            switch(alarmEvent){
                case 0:
                    // supposed to be power on
                    return R.drawable.mercedes_sharp_turn_3x;
                case 1:
                    return R.drawable.mercedes_ignition_on_3x;
                case 2:
                    return R.drawable.mercedes_ignition_off_3x;
                case 3:
                    // supposed to be engine coolant temp
                    return R.drawable.mercedes_sharp_turn_3x;
                case 4:
                    // supposed to be high rpm
                    return R.drawable.mercedes_sharp_turn_3x;
                case 5:
                    return R.drawable.low_voltage_mercedes_3x;
                case 6:
                    return R.drawable.mercedes_idling_3x;
                case 7:
                    return R.drawable. mercedes_fatigue_driving_3x;
                case 8:
                    return R.drawable.mercedes_speeding_3x;
                case 9:
                    return R.drawable.mercedes_collision_3x;
                case 10:
                    // supposed to be shock
                    return R.drawable.mercedes_sharp_turn_3x;
                case 11:
                    // suppused to be towing ;
                    return R.drawable.mercedes_sharp_turn_3x;
                case 12:
                    // supposed to be dangerous driving
                    return R.drawable.mercedes_sharp_turn_3x;
                case 13:
                    return R.drawable.mercedes_acceleration_3x;
                case 14:
                    return R.drawable.mercedes_deceleration_3x;
                case 15:
                    return R.drawable.mercedes_sharp_turn_3x;
                case 16:
                    return R.drawable.mercedes_quick_lane_change_3x;
                default:
                    return R.drawable.mercedes_sharp_turn_3x;
            }

        }
        else {
            switch(alarmEvent){
                case 0:

                case 1:
                    return R.drawable.ignition_on_3x;
                case 2:
                    return R.drawable.ignition_off_3x;
                case 3:
                    // supposed to be engine coolant temp
                    return R.drawable.sharp_turn_3x;
                case 4:
                    return R.drawable.high_rpm_3x;
                case 5:
                    return R.drawable.low_voltage_3x;
                case 6:
                    return R.drawable.idling_3x;
                case 7:
                    return R.drawable. fatigue_driving_3x;
                case 8:
                    return R.drawable.speeding_3x;
                case 9:
                    return R.drawable.collision_3x;
                case 10:
                    // supposed to be shock
                    return R.drawable.sharp_turn_3x;
                case 11:
                    // suppused to be towing ;
                    return R.drawable.sharp_turn_3x;
                case 12:
                    // supposed to be dangerous driving
                    return R.drawable.sharp_turn_3x;
                case 13:
                    return R.drawable.acceleration_3x;
                case 14:
                    return R.drawable.deceleration_3x;
                case 15:
                    return R.drawable.sharp_turn_3x;
                case 16:
                    return R.drawable.quick_lane_change_3x;
                default:
                    return R.drawable.sharp_turn_3x;
            }
        }

    }
}
