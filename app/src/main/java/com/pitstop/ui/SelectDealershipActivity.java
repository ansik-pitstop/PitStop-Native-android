package com.pitstop.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.models.Dealership;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;

import java.util.List;

public class SelectDealershipActivity extends AppCompatActivity {

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    public static String SELECTED_DEALERSHIP = "selected_dealership";
    public static String ACTIVITY_NAME = "select_dealership";
    public static int RESULT_OK = 103;
    public static int RC_DEALERSHIP = 104;

    private RecyclerView recyclerView;
    private RecyclerView.Adapter adapter;
    private RecyclerView.LayoutManager layoutManager;

    private ProgressBar progressBar;
    private CardView message_card;
    private TextView message;

    private boolean hadInternetConnection = false;
    private LocalShopAdapter localStore;

    private NetworkHelper networkHelper;

    private static final String TAG = SelectDealershipActivity.class.getSimpleName();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        networkHelper = new NetworkHelper(getApplicationContext());

        setContentView(R.layout.activity_select_dealership);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        localStore = new LocalShopAdapter(this);
        setup();
    }

    @Override
    protected void onResume() {
        super.onResume();

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.select_dealership_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        if(id == R.id.refresh) {
            setup();
        }

        if(id == R.id.log_out) {
            navigateToLogin();
            application.logOutUser();
        }
        return true;
    }

    @Override
    public void onBackPressed() {

        try {
            mixpanelHelper.trackButtonTapped("Back", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent mainActivity = new Intent(this, MainActivity.class);
        mainActivity.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
        startActivity(mainActivity);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
    }

    private void setup() {
        recyclerView = (RecyclerView) findViewById(R.id.dealership_list);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        message_card = (CardView) findViewById(R.id.message_card);
        message = (TextView) findViewById(R.id.message);
        message_card.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);


        if(NetworkHelper.isConnected(this)) {
            Log.i(TAG, "Internet connection found");
            hadInternetConnection = true;

            List<Dealership> dealerships = localStore.getAllDealerships();
            if(dealerships.isEmpty()) {
                networkHelper.getShops(new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            progressBar.setVisibility(View.GONE);
                            Log.i(TAG, "Get shops response: " + response);
                            try {
                                List<Dealership> list = Dealership.createDealershipList(response);
                                localStore.deleteAllDealerships();
                                localStore.storeDealerships(list);
                                adapter = new CustomAdapter(list);
                                recyclerView.setAdapter(adapter);
                            } catch (JSONException e) {
                                Toast.makeText(SelectDealershipActivity.this, "Failed to get dealership info",
                                        Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(SelectDealershipActivity.this, "Failed to get dealership info",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });

            } else {
                progressBar.setVisibility(View.GONE);
                adapter = new CustomAdapter(dealerships);
                recyclerView.setAdapter(adapter);
            }

        } else if(!localStore.getAllDealerships().isEmpty()) {
            Log.i(TAG, "No internet");
            progressBar.setVisibility(View.GONE);
            adapter = new CustomAdapter(localStore.getAllDealerships());
            recyclerView.setAdapter(adapter);
        } else {
            hadInternetConnection = false;
            message_card.setVisibility(View.VISIBLE);
        }
    }

    private void navigateToLogin() {
        Intent intent = new Intent(this, SplashScreen.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {
        private List<Dealership> shops;

        public CustomAdapter(List<Dealership> shops) {
            this.shops = shops;
        }

        @Override
        public CustomAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.list_item_dealerships, null);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
            final Dealership shop = shops.get(position);
            final int displayedShopId = shop.getId();

            holder.dealershipName.setText(shop.getName());
            holder.dealershipAddress.setText(shop.getAddress());
            holder.dealershipTel.setText(shop.getPhone());
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i(TAG, "Dealership selected: " + shop.getName());
                    try {
                        mixpanelHelper.trackButtonTapped("Selected " + shop.getName(), TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
            }
        }
    }

}
