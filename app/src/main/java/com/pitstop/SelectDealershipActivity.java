package com.pitstop;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.pitstop.R;

import java.util.List;

public class SelectDealershipActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ProgressBar progressBar;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dealership);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        recyclerView = (RecyclerView) findViewById(R.id.dealership_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.VISIBLE);

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");
        query.findInBackground(new FindCallback<ParseObject>() {

            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                progressBar.setVisibility(View.GONE);
                if(e == null) {
                    adapter = new DealershipAdapter(objects);
                    recyclerView.setAdapter(adapter);
                } else {
                    Toast.makeText(SelectDealershipActivity.this, "Failed to get dealership info",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    public void finish() {
        super.finish();
    }

    public class DealershipAdapter extends RecyclerView.Adapter<DealershipAdapter.ViewHolder> {
        private List<ParseObject> shops;

        public DealershipAdapter(List<ParseObject> shops) {
            this.shops = shops;
        }

        @Override
        public DealershipAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dealership_list_row, null);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(DealershipAdapter.ViewHolder holder, int position) {
            ParseObject shop = shops.get(position);

            holder.dealershipName.setText(shop.get("name").toString());
            holder.dealershipAddress.setText(shop.get("addressText").toString());
            holder.dealershipTel.setText(shop.get("phoneNumber").toString());
        }

        @Override
        public int getItemCount() {
            return (shops != null ? shops.size() : 0);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView dealershipName;
            public TextView dealershipAddress;
            public TextView dealershipTel;

            public ViewHolder(View itemView) {
                super(itemView);

                dealershipName = (TextView) itemView.findViewById(R.id.dealership_name);
                dealershipAddress = (TextView) itemView.findViewById(R.id.dealership_address);
                dealershipTel = (TextView) itemView.findViewById(R.id.dealership_tel);
            }
        }
    }

}
