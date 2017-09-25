package com.pitstop.adapters;

import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Notification;
import com.pitstop.ui.Notifications.NotificationView;

import java.util.List;

/**
 * Created by ishan on 2017-09-12.
 */

public class NotificationAdapter extends RecyclerView.Adapter<
        NotificationAdapter.NotificationViewHolder> {

    NotificationView notificationView;
    List<Notification> notificationList;

    public NotificationAdapter(NotificationView notifView, List<Notification> notiflist){
        this.notificationList = notiflist;
        this.notificationView = notifView;

    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent ,false);
        NotificationViewHolder notificationViewHolder = new NotificationViewHolder(view, notificationView);
        int position = getItemViewType(viewType);
        view.setOnClickListener(v -> notificationView
                .onNotificationClicked(notificationList.get(position).getPushType()));

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


    class NotificationViewHolder extends RecyclerView.ViewHolder{
        private TextView titleTV;
        private TextView descriptionTV;
        private TextView dateTV;
        private ImageView imageView;
        private NotificationView notificationView;



        public NotificationViewHolder(View itemView, NotificationView notificationView){
            super(itemView);
            titleTV = (TextView)itemView.findViewById(R.id.notificationTitle);
            descriptionTV = (TextView)itemView.findViewById(R.id.notificationDescription);
            dateTV = (TextView)itemView.findViewById(R.id.dateTV);
            this.notificationView = notificationView;
            imageView = (ImageView) itemView.findViewById(R.id.notification_image);


        }
        public void bind(Notification notification) {
            titleTV.setText(notification.getTitle());
            descriptionTV.setText(notification.getContent());
            dateTV.setText(notification.getDateCreated());
            imageView.setImageResource(notificationView.changeimage(notification.getPushType()));

        }
    }
}
