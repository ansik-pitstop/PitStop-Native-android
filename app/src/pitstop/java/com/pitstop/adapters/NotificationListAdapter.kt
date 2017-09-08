package com.pitstop.adapters

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.pitstop.R
import com.pitstop.ui.Notifications.NotificationFragment
import com.pitstop.ui.Notifications.NotificationViewHolder

/**
 * Created by ishan on 2017-09-05.
 */




class NotificationListAdapter(internal var notificationList: List<com.pitstop.models.Notification>) : RecyclerView.Adapter<NotificationViewHolder>(){


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder? {
        val view :View = LayoutInflater.from(parent.context).inflate(R.layout.list_item_notification, parent, false)
        Log.d("Notification Adapter", "Adapting")

        val holder = NotificationViewHolder(view);
        val position: Int = viewType
        view.setOnClickListener {
            NotificationFragment.onNotificationClicked(notificationList[position].title)
        }
        return holder
    }
    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(notificationList[position])
        Log.d("Notification Adapter", "binding");

    }
    override fun getItemCount(): Int {
        return notificationList.size
    }

    override fun getItemViewType(position: Int): Int {
        return position;
    }



}
