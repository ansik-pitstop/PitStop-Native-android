package com.pitstop.ui.services.custom_service.view_fragments;

import android.graphics.Color;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;
import com.pitstop.models.service.CustomIssueListItem;

import java.util.List;

/**
 * Created by Matt on 2017-07-26.
 */

public class CustomServiceListAdapter extends RecyclerView.Adapter<CustomServiceListAdapter.CustomServiceViewHolder>{
    public static String SERVICE_ACTION_KEY = "service_action";
    public static String SERVICE_PART_KEY = "service_part_name";
    public static String SERVICE_PRIORITY_KEY = "service_priority";
    public static String SERVICE_ACTION_OTHER_KEY = "service_action_other";
    public static String SERVICE_PART_OTHER_KEY = "service_part_name_other";


    private List<CustomIssueListItem> items;
    private PresenterCallback callback;


    public CustomServiceListAdapter(List<CustomIssueListItem> items,PresenterCallback callback) {
        this.items = items;
        this.callback = callback;
    }
    @Override
    public CustomServiceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new CustomServiceViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_custom_serivce, parent, false));
    }

    @Override
    public void onBindViewHolder(final CustomServiceViewHolder holder, final int position) {
        CustomIssueListItem item = items.get(position);
        holder.text.setText(item.getText());
        holder.card.setCardBackgroundColor(Color.parseColor(item.getCardColor()));
        if(item.getTextColor() != null){
            holder.text.setTextColor(Color.parseColor(item.getTextColor()));
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(item.getKey().equals(SERVICE_ACTION_KEY)){
                    callback.onActionItemClicked(item);
                }else if(item.getKey().equals(SERVICE_PART_KEY)){
                    callback.onPartNameItemClicked(item);
                }else if(item.getKey().equals(SERVICE_PRIORITY_KEY)){
                    callback.onPriorityItemClicked(item);
                }else if(item.getKey().equals(SERVICE_ACTION_OTHER_KEY)){
                    callback.onActionOther();
                }else if(item.getKey().equals(SERVICE_PART_OTHER_KEY)){
                    callback.onPartNameOther();
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public class CustomServiceViewHolder extends RecyclerView.ViewHolder {
        private TextView text;
        private CardView card;

        public CustomServiceViewHolder(View itemView) {
            super(itemView);
            text = (TextView) itemView.findViewById(R.id.service_list_item_text);
            card = (CardView) itemView.findViewById(R.id.service_list_item_card);
        }
    }
}
