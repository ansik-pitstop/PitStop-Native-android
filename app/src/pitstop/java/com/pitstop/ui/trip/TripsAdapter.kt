package com.pitstop.ui.trip

import android.location.Location
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.pitstop.R
import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by Karol Zdebel on 3/5/2018.
 */
class TripsAdapter(val trips: List<List<Location>>): RecyclerView.Adapter<TripsAdapter.Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): TripsAdapter.Holder{
        val view = LayoutInflater.from(parent?.context).inflate(R.layout.list_item_trip, parent, false)
        return TripsAdapter.Holder(view)
    }

    override fun getItemCount(): Int {
        return trips.size
    }

    override fun onBindViewHolder(holder: TripsAdapter.Holder, position: Int) {
        holder.bind(trips[position])
    }

    class Holder(itemView: View): RecyclerView.ViewHolder(itemView){
        private var title: TextView = itemView.findViewById(R.id.title)
        private var description: TextView = itemView.findViewById(R.id.description)
        private var date: TextView = itemView.findViewById(R.id.date)

        fun bind(trip: List<Location>){
            val len = (trip[trip.size-1].time - trip[0].time)/1000/60
            title.text = String.format("%d minute trip recorded",len)
            description.text = String.format("Trip start coordinates (%f,%f), Trip end coordinates (%f,%f)"
                    ,trip[0].longitude,trip[0].latitude,trip.last().longitude,trip.last().latitude)
            date.text = SimpleDateFormat("yyyy/MM/dd", Locale.CANADA).format(Date(trip[0].time))
        }
    }
}