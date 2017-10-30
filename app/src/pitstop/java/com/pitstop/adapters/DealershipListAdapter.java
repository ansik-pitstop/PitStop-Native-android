package com.pitstop.adapters;

import android.content.Context;
import android.database.DataSetObserver;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Dealership;

import java.util.List;

/**
 * Created by ishan on 2017-09-21.
 */

public class DealershipListAdapter extends ArrayAdapter<Dealership> {

    Context context;
    List<Dealership> dealershipList;

    public DealershipListAdapter(@NonNull Context context, @LayoutRes int resource, List<Dealership> list) {
        super(context, resource);
        this.context= context;
        this.dealershipList = list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View View, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View dealerShipView = inflater.inflate(R.layout.list_item_dealership, parent, false);
        TextView dealershipName = (TextView)dealerShipView.findViewById(R.id.dealership_dialog_name);
        dealershipName.setText(dealershipList.get(position).getName());
        return dealerShipView;
    }
    @Override
    public int getCount() {
        return dealershipList.size();
    }

}

