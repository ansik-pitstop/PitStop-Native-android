package com.pitstop.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.R;
import com.pitstop.models.Notification;
import com.pitstop.ui.Notifications.NotificationView;
import com.pitstop.ui.Notifications.NotificationViewHolder;

import java.util.List;

/**
 * Created by ishan on 2017-09-12.
 */

public class NotificationAdapter extends RecyclerView.Adapter<NotificationViewHolder> {

    NotificationView notificationView;
    List<Notification> notificationList;

    public NotificationAdapter(NotificationView notifView, List<Notification> notiflist){
        this.notificationList = notiflist;
        this.notificationView = notifView;

    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent ,false);
        Log.d("Notification Adapter", "Adapting");
        NotificationViewHolder notificationViewHolder = new NotificationViewHolder(view);
        int position = getItemViewType(viewType);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                notificationView.onNotificationClicked(notificationList.get(position).getTitle());

            }
        });

        return notificationViewHolder;

    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        holder.bind(notificationList.get(position));

    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    @Override
    public int getItemViewType(int position){
        return position;
    }
}
