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
import com.pitstop.ui.service_request.view_fragment.ServiceIssueAdapter;

import java.util.List;

/**
 * Created by Matthew on 2017-05-02.
 */

public class AppointmentsAdapter extends RecyclerView.Adapter<AppointmentsAdapter.MyAppointmentsViewHolder>  {
    private static final String TAG = ServiceIssueAdapter.class.getSimpleName();

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

        if (viewType == VIEW_TYPE_EMPTY) {
            holder.date.setText("No Appointments");
            holder.details.setText("There are currently no appointments");

        } else {
            holder.date.setText(mAppts.get(position).getDate());
            if(mAppts.get(position).getComments().equals("")) {
                holder.details.setText("No Comments");
            }else{
                holder.details.setText(mAppts.get(position).getComments());
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
