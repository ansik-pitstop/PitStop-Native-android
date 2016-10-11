package com.pitstop.adapters;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Dealership;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by David on 7/22/2016.
 */
public class DealershipSelectAdapter extends RecyclerView.Adapter<DealershipSelectAdapter.ViewHolder> implements Filterable {

    public List<Dealership> shops;
    private int chosen;
    private DealershipSelectAdapterCallback callback;
    private DealershipAdapterFilter adapterFilter;

    public DealershipSelectAdapter(List<Dealership> shops, DealershipSelectAdapterCallback callback) {
        this.shops = shops;
        this.chosen = 0;
        this.callback = callback;
        callback.dealershipSelectedCallback(shops.get(0));
    }

    @Override
    public DealershipSelectAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.list_item_dealerships, null);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(DealershipSelectAdapter.ViewHolder holder, final int position) {
        final Dealership shop = shops.get(position);
        final int displayedShopId = shop.getId();

        holder.dealershipName.setText(shop.getName());
        holder.dealershipAddress.setText(shop.getAddress());
        if (position == chosen) {
            holder.checkbox.setChecked(true);
        } else {
            holder.checkbox.setChecked(false);
        }
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chosen = position;
                notifyDataSetChanged();
                callback.dealershipSelectedCallback(shop);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (shops != null ? shops.size() : 0);
    }

    @Override
    public Filter getFilter() {
        if (adapterFilter == null)
            adapterFilter = new DealershipAdapterFilter(this, shops);
        return adapterFilter;
    }

    public class DealershipAdapterFilter extends Filter {

        private final DealershipSelectAdapter adapter;
        private final List<Dealership> originalList;
        private final List<Dealership> filteredList;

        public DealershipAdapterFilter(DealershipSelectAdapter dealershipSelectAdapter, List<Dealership> shops) {
            super();
            this.adapter = dealershipSelectAdapter;
            this.originalList = new LinkedList<>(shops);
            this.filteredList = new ArrayList<>();
        }

        @Override
        protected FilterResults performFiltering(CharSequence charSequence) {
            filteredList.clear();
            final FilterResults results = new FilterResults();

            if (charSequence.length() == 0) {
                filteredList.addAll(originalList);
            } else {
                final String filterPattern = charSequence.toString().toLowerCase().trim();

                for (final Dealership user : originalList) {
                    if (user.getName().toLowerCase().trim().contains(filterPattern)) {
                        filteredList.add(user);
                    }
                }
            }
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
            adapter.shops.clear();
            adapter.shops.addAll((ArrayList<Dealership>) filterResults.values);
            adapter.chosen = 0;
            if (!adapter.shops.isEmpty()) {
                callback.dealershipSelectedCallback(adapter.shops.get(chosen));
            }
            adapter.notifyDataSetChanged();
        }
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public TextView dealershipName;
        public TextView dealershipAddress;
        public RelativeLayout container;
        public CheckBox checkbox;

        public ViewHolder(View itemView) {
            super(itemView);

            dealershipName = (TextView) itemView.findViewById(R.id.dealership_name);
            dealershipAddress = (TextView) itemView.findViewById(R.id.dealership_address);
            checkbox = (CheckBox) itemView.findViewById(R.id.checkBox);
            container = (RelativeLayout) itemView.findViewById(R.id.container);
        }
    }

    public interface DealershipSelectAdapterCallback {
        void dealershipSelectedCallback(Dealership shop);
    }
}
