package com.pitstop;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
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
import com.pitstop.utils.InternetChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SelectDealershipActivity extends AppCompatActivity {
    public static String SELECTED_DEALERSHIP = "selected_dealership";
    public static int RESULT_OK = 103;
    public static int RC_DEALERSHIP = 104;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ProgressBar progressBar;
    private CardView message_card;
    private TextView message;

    private boolean hadInternetConnection = false;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dealership);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_dealership_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.refresh) {
            setup();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        if(intent!=null && intent.getBooleanExtra(MainActivity.hasCarsInDashboard,false)) {
            startActivity(new Intent(this,MainActivity.class));
        } else {
            if(hadInternetConnection) {
                Toast.makeText(SelectDealershipActivity.this,"Please select dealership",
                        Toast.LENGTH_SHORT).show();
            } else {
                setup();
            }
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    private void setup() {
        recyclerView = (RecyclerView) findViewById(R.id.dealership_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        message_card = (CardView) findViewById(R.id.message_card);
        message = (TextView) findViewById(R.id.message);
        progressBar.setVisibility(View.VISIBLE);
        message_card.setVisibility(View.GONE);

        try {
            if(new InternetChecker(this).execute().get()) {
                hadInternetConnection = true;
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
            } else {
                hadInternetConnection = false;
                progressBar.setVisibility(View.GONE);
                message_card.setVisibility(View.VISIBLE);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
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
            final String displayedShopId = shop.getObjectId();

            holder.dealershipName.setText(shop.get("name").toString());
            holder.dealershipAddress.setText(shop.get("addressText").toString());
            holder.dealershipTel.setText(shop.get("phoneNumber").toString());
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent data = new Intent();
                    data.putExtra(SELECTED_DEALERSHIP, displayedShopId);
                    setResult(RESULT_OK, data);
                    finish();
                }
            });
        }

        @Override
        public int getItemCount() {
            return (shops != null ? shops.size() : 0);
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            public TextView dealershipName;
            public TextView dealershipAddress;
            public TextView dealershipTel;
            public CardView container;

            public ViewHolder(View itemView) {
                super(itemView);

                dealershipName = (TextView) itemView.findViewById(R.id.dealership_name);
                dealershipAddress = (TextView) itemView.findViewById(R.id.dealership_address);
                dealershipTel = (TextView) itemView.findViewById(R.id.dealership_tel);
                container = (CardView) itemView.findViewById(R.id.dealership_row_item);
            }
        }
    }

}
