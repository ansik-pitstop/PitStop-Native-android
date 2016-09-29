package com.pitstop.ui.addPresetIssueFragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.pitstop.R;

import java.util.List;

/**
 * Created by yifan on 16/9/28.
 */
public class PresetItemFragment extends Fragment {

    private static final String TAG = PresetItemFragment.class.getSimpleName();

    private List<String> mItems;
    private RecyclerView mRecyclerView;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_add_preset_issue_pick_item, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.add_preset_issues_recycler_view);

        return rootView;
    }


    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.CustomHolder>{

        private List<String> mData;

        public CustomAdapter(List<String> data) {
            mData = data;
        }

        @Override
        public CustomHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getActivity()).inflate(R.layout.activity_add_preset_issue_item, null);
            return new CustomHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomHolder holder, int position) {
            holder.mItemText.setText(mData.get(position));
        }

        @Override
        public int getItemCount() {
            return mData.size();
        }

        public class CustomHolder extends RecyclerView.ViewHolder{

            public TextView mItemText;

            public CustomHolder(View itemView) {
                super(itemView);
                mItemText = (TextView)itemView.findViewById(R.id.preset_list_item);
            }
        }
    }

    public interface ItemSelectCallback{
        void setPickedItem(String item);
    }
}
