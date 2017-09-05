package com.pitstop.adapters

import android.app.Notification
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import com.pitstop.R
import com.pitstop.ui.Notifications.NotificationViewHolder

import java.util.LinkedList

/**
 * Created by ishan on 2017-09-05.
 */

class NotificationListAdapter : RecyclerView.Adapter<NotificationViewHolder>() {

    internal var notificationList: List<Notification>? = null


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder? {

        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_notification, parent, false)



        return null
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {

    }


    override fun getItemCount(): Int {
        return notificationList!!.size

    }
}
