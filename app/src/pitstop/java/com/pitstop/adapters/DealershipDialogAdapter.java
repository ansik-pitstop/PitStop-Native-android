package com.pitstop.adapters;

import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.Dealership;
import com.pitstop.ui.my_garage.MyGaragePresenter;
import com.pitstop.ui.my_garage.MyGarageView;

import org.w3c.dom.Text;

import java.util.List;

/**
 * Created by ishan on 2017-09-20.
 */

public class DealershipDialogAdapter extends RecyclerView.Adapter<DealershipDialogAdapter.DealershipViewHolder> {
    private static final String TAG = DealershipDialogAdapter.class.getSimpleName();

    private List <Dealership> dealershipList;
    private MyGarageView garageView;
    private int origin;

    public DealershipDialogAdapter(List<Dealership> list, MyGarageView view, int origin){
        Log.d(TAG, "DealershipDealogAdapter");
        this.garageView = view;
        this.dealershipList = list;
        this.origin  = origin;

    }
    @Override
    public DealershipViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder()");

        View view  = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_dealership, parent ,false);
        DealershipDialogAdapter.DealershipViewHolder dealershipViewHolder = new DealershipDialogAdapter.DealershipViewHolder(view);
        int position = getItemViewType(viewType);
        view.setOnClickListener(v -> {
                garageView.onDealershipSelected(dealershipList.get(position), origin);


        });

        return dealershipViewHolder;

    }

    @Override
    public void onBindViewHolder(DealershipViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder");
        holder.bind(dealershipList.get(position));

    }

    @Override
    public int getItemCount() {
        return dealershipList.size();
    }

    @Override
    public int getItemViewType(int position){
        return position;
    }

    class DealershipViewHolder extends RecyclerView.ViewHolder{
        TextView nameTextView;

        public DealershipViewHolder(View itemView) {
            super(itemView);
            nameTextView = (TextView) itemView.findViewById(R.id.dealership_dialog_name);
        }

        public void bind(Dealership dealership){
            nameTextView.setText(dealership.getName());
        }





    }


}
