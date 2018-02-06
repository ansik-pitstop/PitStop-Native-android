package com.pitstop.ui.my_appointments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Appointment;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Created by Matthew on 2017-05-02.
 */

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.MyAppointmentsViewHolder>  {

    private final int VIEW_TYPE_EMPTY = 100;
    private final int VIEW_TYPE_CUSTOM = 101;

    private final List<Appointment> mAppts;
    private final Context mContext;

    public AppointmentsAdapter(Context context, @NonNull List<Appointment> appointments) {
        mAppts = appointments;
        mContext = context;
    }


    @Override
    public MyAppointmentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MyAppointmentsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_appointment, parent, false));
    }

    @Override
    public void onBindViewHolder(AppointmentsAdapter.MyAppointmentsViewHolder holder, int position) {
        int viewType = getItemViewType(position);
        String pretext = "";


        if (viewType == VIEW_TYPE_EMPTY) {
            holder.date.setText(R.string.no_appointments);
            holder.details.setText(R.string.no_appointments);

        } else {
            Appointment currentApp = mAppts.get(position);

            SimpleDateFormat newFormat = new SimpleDateFormat("EEE dd MMM yyyy - hh:mm aa", Locale.CANADA);
            holder.date.setText(newFormat.format(currentApp.getDate()));

            if(currentApp.getComments().equals("")) {
                if(currentApp.getState().equals("tentative")){
                    holder.details.setText(R.string.no_salesperson);
                }else {
                    holder.details.setText(R.string.no_comments);
                }
            }else{
                if(currentApp.getState().equals("tentative")){
                    pretext = "Salesperson: ";
                }else{
                    pretext = "Comments: ";
                }
                holder.details.setText(pretext+currentApp.getComments());
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mAppts.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (mAppts.isEmpty()) return 1;
        return mAppts.size();
    }

    public class MyAppointmentsViewHolder extends RecyclerView.ViewHolder{
        TextView date;
        TextView details;
        public MyAppointmentsViewHolder(View itemView) {
            super(itemView);
            date = (TextView) itemView.findViewById(R.id.appointment_date);
            details = (TextView) itemView.findViewById(R.id.appointment_details);
        }
    }
}
