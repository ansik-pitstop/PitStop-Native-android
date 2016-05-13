package com.pitstop;

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

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.InternetChecker;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.ExecutionException;

public class SelectDealershipActivity extends AppCompatActivity {

    private ParseApplication application;
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

    private static final String TAG = SelectDealershipActivity.class.getSimpleName();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_dealership);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        application = (ParseApplication) getApplicationContext();
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
            ParseUser.logOut();
            navigateToLogin();
        }
        return true;
    }

    @Override
    public void onBackPressed() {
        String userId = null;

        Intent intent = getIntent();
        if(intent!=null && intent.getBooleanExtra(MainActivity.HAS_CAR_IN_DASHBOARD,false)) {

            try {
                mixpanelHelper.trackButtonTapped("Back", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent mainActivity = new Intent(this, MainActivity.class);
            mainActivity.putExtra(MainActivity.FROM_ACTIVITY, ACTIVITY_NAME);
            startActivity(mainActivity);

        } else if(ParseUser.getCurrentUser() != null) {

            userId = ParseUser.getCurrentUser().getObjectId();

            if(InternetChecker.isConnected(this)) {
                Log.i(TAG, "Internet connection found");
                //hadInternetConnection = true;
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
                query.whereContains("owner", userId);
                progressBar.setVisibility(View.VISIBLE);
                query.findInBackground(new FindCallback<ParseObject>() {


                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        progressBar.setVisibility(View.GONE);
                        if(e == null) {
                            if (!objects.isEmpty()) {
                                startActivity(new Intent(SelectDealershipActivity.this,
                                        MainActivity.class));
                            } else {
                                if(hadInternetConnection) {
                                    Toast.makeText(SelectDealershipActivity.this,
                                            "Please select dealership",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    setup();
                                }
                            }
                        } else {
                            Log.i("ParseError",e.getMessage());
                        }
                    }
                });
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
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);

        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        message_card = (CardView) findViewById(R.id.message_card);
        message = (TextView) findViewById(R.id.message);
        message_card.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);


        if(InternetChecker.isConnected(this)) {
            Log.i(TAG, "Internet connection found");
            hadInternetConnection = true;
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");

            List<Dealership> dealerships = localStore.getAllDealerships();
            if(dealerships.isEmpty()) {
                query.findInBackground(new FindCallback<ParseObject>() {

                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        progressBar.setVisibility(View.GONE);
                        if(e == null) {
                            List<Dealership> list = Dealership.createDealershipList(objects);
                            localStore.storeDealerships(list);
                            adapter = new CustomAdapter(list);
                            recyclerView.setAdapter(adapter);
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

        } else {
            Log.i(TAG, "No internet");
            localStore.deleteAllDealerships();
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
                    .inflate(R.layout.dealership_list_row, null);

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(CustomAdapter.ViewHolder holder, int position) {
            final Dealership shop = shops.get(position);
            final String displayedShopId = shop.getParseId();

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
                dealershipTel = (TextView) itemView.findViewById(R.id.dealership_tel);
                container = (CardView) itemView.findViewById(R.id.dealership_row_item);
            }
        }
    }

}
