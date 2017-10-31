package com.pitstop.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.castel.obd215b.util.DateUtil;
import com.pitstop.R;
import com.pitstop.models.Alarm;

import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Created by ishan on 2017-10-30.
 */

public class AlarmsAdapter extends RecyclerView.Adapter<AlarmsAdapter.AlarmViewHolder> {
    public static final String TAG = AlarmsAdapter.class.getSimpleName();
    private List<Alarm> alarmList;


    public AlarmsAdapter(List<Alarm> alarmList){
        this.alarmList = alarmList;
    }


    @Override
    public AlarmViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_alarm, parent, false);
        AlarmsAdapter.AlarmViewHolder alarmViewHolder = new AlarmViewHolder(view);
        return  alarmViewHolder;

    }

    @Override
    public void onBindViewHolder(AlarmViewHolder holder, int position) {
        holder.bind(alarmList.get(position));

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

        ImageView alarmIcon;
        TextView alarmName;
        TextView alarmValue;
        TextView alarmTime;

        public AlarmViewHolder(View itemView) {
            super(itemView);
            this.alarmIcon = itemView.findViewById(R.id.alarm_icon);
            this.alarmName = itemView.findViewById(R.id.alarm_name);
            this.alarmValue = itemView.findViewById(R.id.alarm_value);
            this.alarmTime  = itemView.findViewById(R.id.alarm_time);
        }

        public void bind(Alarm alarm){
            alarmName.setText(getAlarmName(alarm.getAlarmEvent()));
            alarmValue.setText(Float.toString(alarm.getAlarmValue()));
            SimpleDateFormat sdf = new SimpleDateFormat("MMMM d, yyyy 'at' h:mm a");
            String date = sdf.format(alarm.getRtcTime());
            alarmTime.setText(date);

        }
    }

    public String getAlarmName(int alarmId){
        switch(alarmId){

            case 0:
                return "Power On Alarm";
            case 1:
                return "Ignition On Alarm";
            case 2:
                return "Ignition Off Alarm" ;
            case 3 :
                return "Engine Coolant Over Temperature" ;
            case 4:
                return "High RPM";
            case 5:
                return "Low Voltage";
            case 6:
                return "Idling";
            case 7:
                return "Fatigue Driving";
            case 8:
                return "Speeding";
            case 9:
                return "Collision";
            case 10:
                return "Shock";
            case 11:
                return "Towing";
            case 12:
                return "Dangerous Driving";
            case 13:
                return "Acceleration";
            case 14:
                return "Deceleration";
            case 15:
                return "Sharp Turn";
            case 16:
                return "Quick Lane Change";
            default:
                return "";
        }
    }
}
