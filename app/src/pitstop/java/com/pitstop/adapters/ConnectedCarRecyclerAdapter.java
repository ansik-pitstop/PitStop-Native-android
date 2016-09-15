package com.pitstop.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.utils.PIDParser;

import java.util.ArrayList;

/**
 * Created by David Liu on 3/13/2016.
 */
public class ConnectedCarRecyclerAdapter extends RecyclerView.Adapter<ConnectedCarRecyclerAdapter.ViewHolder> {
    public ArrayList<PIDParser.Pair<String,String>> dataList = new ArrayList<>();
    Context context;
    public ConnectedCarRecyclerAdapter(Context c){
        context = c;
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_connected_car_display, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.key.setText((String)dataList.get(position).key);
        holder.value.setText((String)dataList.get(position).value);

    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView key;
        public TextView value;
        public ViewHolder(View v) {
            super(v);
            key = (TextView) v.findViewById(R.id.key);
            value = (TextView) v.findViewById(R.id.value);
        }
    }
}
