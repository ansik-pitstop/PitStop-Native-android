package com.pitstop.ui.custom_shops;


import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Dealership;

import java.util.List;

/**
 * Created by Matthew on 2017-05-16.
 */

public class ShopAdapter extends RecyclerView.Adapter<ShopAdapter.ShopViewHolder>  {

    private final int VIEW_TYPE_EMPTY = 100;

    private final List<Dealership> dealerships;

    private ShopPresnter shopPresnter;


    public ShopAdapter(@NonNull List<Dealership> dealerships, ShopPresnter shopPresnter) {
        this.dealerships = dealerships;
        this.shopPresnter = shopPresnter;
    }


    @Override
    public ShopViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {


        return new ShopViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_trip, parent, false));
    }

    @Override
    public void onBindViewHolder(ShopViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_EMPTY) {
            holder.name.setText("No Matching Shops");
            holder.address.setText("");
        } else {
                holder.setClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shopPresnter.onShopClicked(dealerships.get(position));
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
        CardView item;
        public ShopViewHolder(View itemView) {
            super(itemView);
            item = (CardView) itemView.findViewById(R.id.trip_card);
            name = (TextView) itemView.findViewById(R.id.trip_date);
            address = (TextView) itemView.findViewById(R.id.trip_details);
        }

        public void setClickListener(final View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }

    }

}
