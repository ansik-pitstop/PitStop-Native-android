package com.pitstop.ui.Notifications

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.TextView

import com.pitstop.R
import com.pitstop.models.Notification

/**
 * Created by ishan on 2017-09-05.
 */

class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    internal val titleTV: TextView;
    internal val descriptionTV: TextView;
    internal val dateTV: TextView;

    init {
        titleTV = itemView.findViewById(R.id.notificationTitle) as TextView
        descriptionTV = itemView.findViewById(R.id.notificationDescription) as TextView
        dateTV = itemView.findViewById(R.id.dateTV) as TextView

    }
    fun bind(notification: Notification) {
        titleTV.text = ("Welcome to Pitstop")
        descriptionTV.text =("Your first Pitstop notification")

    }


}
