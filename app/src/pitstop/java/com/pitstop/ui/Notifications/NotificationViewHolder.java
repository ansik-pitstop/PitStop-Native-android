package com.pitstop.ui.Notifications;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.pitstop.R;

import org.w3c.dom.Text;

/**
 * Created by ishan on 2017-09-05.
 */

public class NotificationViewHolder extends RecyclerView.ViewHolder {
    TextView titleTV;
    TextView descriptionTV;




    public NotificationViewHolder(View itemView) {
        super(itemView);
        titleTV = (TextView) itemView.findViewById(R.id.notificationTitle);
        descriptionTV = (TextView) itemView.findViewById(R.id.notificationDescription);


    }






}
