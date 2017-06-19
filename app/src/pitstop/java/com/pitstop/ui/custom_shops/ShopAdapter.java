package com.pitstop.ui.custom_shops;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Dealership;
import com.pitstop.ui.add_car.AddCarContract;

import java.util.List;

/**
 * Created by Matthew on 2017-05-16.
 */

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder>  {

    private final int VIEW_TYPE_EMPTY = 100;

    private final List<Dealership> dealerships;

    private ShopTypePresnter shopTypePresnter;

    public ShopAdapter(@NonNull List<Dealership> dealerships, ShopTypePresnter shopTypePresnter) {
        this.dealerships = dealerships;
        this.shopTypePresnter = shopTypePresnter;
    }


    @Override
    public ShopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        return new ShopViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false));
    }

    @Override
    public void onBindViewHolder(ShopViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_EMPTY) {
            holder.name.setText("Name");
            holder.address.setText("Address");
        } else {
            holder.setClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shopTypePresnter.onShopClicked(dealerships.get(position));
                }
            });
            holder.name.setText(dealerships.get(position).getName());
            holder.address.setText(dealerships.get(position).getAddress());

        }
    }

    @Override
    public int getItemViewType(int position) {
        if (dealerships.isEmpty()) {
            return VIEW_TYPE_EMPTY;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public int getItemCount() {
        if (dealerships.isEmpty()) return 1;
        return dealerships.size();
    }


    public class ShopViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        TextView address;
        public ShopViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.trip_date);
            address = (TextView) itemView.findViewById(R.id.trip_details);
        }

        public void setClickListener(final View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }

    }

}
