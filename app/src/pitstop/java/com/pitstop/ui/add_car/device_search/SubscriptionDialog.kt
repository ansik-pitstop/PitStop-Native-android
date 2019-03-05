package com.pitstop.ui.add_car.device_search

import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import android.widget.TextView
import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.widget.AdapterView
import com.continental.rvd.mobile_sdk.AvailableSubscriptions


class SubscriptionDialog: DialogFragment() {

    private var subscriptionList: RecyclerView? = null
    var adapter: SubscriptionSelectionViewAdapter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_subscription_dialog,container,false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        subscriptionList = view.findViewById(R.id.subscription_list)

        val context = context
        if (context != null) {
            subscriptionList?.layoutManager = LinearLayoutManager(context)
        }
        subscriptionList?.adapter = adapter
    }
}

class SubscriptionSelectionViewAdapter(context: Context, data: AvailableSubscriptions): RecyclerView.Adapter<SubscriptionSelectionViewAdapter.SubscriptionViewHolder>() {

    private var mData: AvailableSubscriptions? = null
    private var mInflater: LayoutInflater? = null
    private val mClickListener: AdapterView.OnItemClickListener? = null

    // data is passed into the constructor
    init {
        this.mInflater = LayoutInflater.from(context)
        this.mData = data
    }

    // inflates the row layout from xml when needed
    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): SubscriptionViewHolder {
        val view = mInflater!!.inflate(R.layout.list_item_day_alarms, parent, false)
        return SubscriptionViewHolder(view)
    }

    // binds the data to the TextView in each row
    override fun onBindViewHolder(holder: SubscriptionViewHolder, position: Int) {
        holder.textView.text = mData!!.subscriptions[position].description
    }

    // total number of rows
    override fun getItemCount(): Int {
        if (mData == null) return 0
        return mData!!.subscriptions.size
    }

     class SubscriptionViewHolder(itemView: View): RecyclerView.ViewHolder(itemView), View.OnClickListener {
         val textView: TextView = itemView.findViewById(R.id.alarm_date)

         init {
             itemView.setOnClickListener(this)
         }

         override fun onClick(view: View?) {
             print("onClick(view: View?)")
         }
    }

}