package com.pitstop.ui.custom_shops;


import android.support.annotation.NonNull;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
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


        return new ShopViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_shop, parent, false));
    }

    @Override
    public void onBindViewHolder(ShopViewHolder holder, final int position) {
        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_EMPTY) {
            holder.name.setText("No Matching Shops");
            holder.address.setText("");
        } else {
            Dealership dealership = dealerships.get(position);
                holder.setClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        shopPresnter.onShopClicked(dealership);
                    }
                });
                holder.name.setText(dealership.getName());
                holder.address.setText(dealership.getAddress());
                if(dealership.getRating()==0){
                    holder.rating.setVisibility(View.GONE);
                    holder.ratingBar.setVisibility(View.GONE);
                }else{
                    holder.rating.setText(Double.toString(dealership.getRating()));
                    holder.ratingBar.setRating((float)dealership.getRating());
                }
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
        TextView rating;
        RatingBar ratingBar;
        CardView item;
        public ShopViewHolder(View itemView) {
            super(itemView);
            rating = (TextView) itemView.findViewById(R.id.rating_number);
            ratingBar = (RatingBar) itemView.findViewById(R.id.rating_bar);
            item = (CardView) itemView.findViewById(R.id.shop_card);
            name = (TextView) itemView.findViewById(R.id.shop_date);
            address = (TextView) itemView.findViewById(R.id.shop_details);
        }

        public void setClickListener(final View.OnClickListener clickListener){
            itemView.setOnClickListener(clickListener);
        }

    }

}
