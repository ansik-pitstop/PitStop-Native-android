package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.pitstop.parse.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by David Liu on 1/30/2016.
 */
public class CarHistoryActivity extends AppCompatActivity {
    private CustomAdapter customAdapter;
    private RecyclerView mRecyclerView;
    private CardView messageCard;

    private ArrayList<Container> array;

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private static final String TAG = CarHistoryActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_history);

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        array = new ArrayList<>();

        messageCard = (CardView) findViewById(R.id.message_card);
        // set up listview
        mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //get all service history
        final ParseQuery query = new ParseQuery("ServiceHistory");
        query.whereEqualTo("carId",getIntent().getStringExtra("carId"));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(final List<ParseObject> objects, ParseException e) {
                if( e == null) {
                    ArrayList<HashSet<String>> storage = new ArrayList<HashSet<String>>();
                    storage.add(new HashSet<String>());
                    storage.add(new HashSet<String>());
                    storage.add(new HashSet<String>());
                    storage.add(new HashSet<String>());
                    storage.add(new HashSet<String>());

                    Log.i(MainActivity.TAG, "Parse objects service history: "+objects.size());
                    if(objects.isEmpty()) {
                        messageCard.setVisibility(View.VISIBLE);
                    }

                    for (ParseObject object : objects){
                        Log.i(MainActivity.TAG, "Parse objects service history: "+object.getObjectId());
                        if(object.get("type")==null && object.get("serviceId") != null){
                            if(object.get("serviceId").equals("124")) {
                                storage.get(3).add(object.getString("serviceObjectId"));
                            } else {
                                storage.get(4).add(object.getString("serviceObjectId"));
                            }
                        }else if(object.getInt("type")==0){
                            storage.get(0).add(object.getString("serviceObjectId"));
                        }else if(object.getInt("type")==1){
                            storage.get(1).add(object.getString("serviceObjectId"));
                        }else if(object.getInt("type")==2) {
                            storage.get(2).add(object.getString("serviceObjectId"));
                        }
                    }
                    String[] a = new String[]{"EdmundsService", "ServiceFixed", "ServiceInterval","RecallEntry","DTC"};
                    for (int i = 0; i<a.length; i++) {
                        ParseQuery intervalQuery = ParseQuery.getQuery(a[i]);
                        Log.i(MainActivity.TAG, a[i]+ " : " + storage.get(i).toString());
                        intervalQuery.whereContainedIn("objectId", storage.get(i));
                        intervalQuery.findInBackground(new FindCallback<ParseObject>() {

                            @Override
                            public void done(List<ParseObject> descObjects, ParseException e) {
                                if( e == null) {
                                    Log.i(MainActivity.TAG, "interval query result: "+descObjects.size());
                                    for (ParseObject obj : descObjects) {
                                        Container con = new Container();
                                        if(obj.get("forRecallMasters")!=null) {
                                            con.name = obj.getString("name");
                                            con.description = obj.getString("description");
                                        } else if(obj.get("dtcCode") != null) {
                                            con.name = obj.getString("dtcCode");
                                            con.description = obj.getString("description");
                                        } else {
                                            Log.i(MainActivity.TAG, "Object for recall masters is null");
                                            con.name = obj.getString("item");
                                            con.description = obj.getString("itemDescription");
                                        }
                                        array.add(con);
                                    }
                                    customAdapter.dataList.clear();
                                    customAdapter.dataList.addAll(array);
                                    customAdapter.notifyDataSetChanged();
                                } else {
                                    Log.i(MainActivity.TAG, "Parse error: "+e.getMessage());
                                }
                            }
                        });
                    }
                } else {
                    Log.i(MainActivity.TAG, e.getMessage());
                }
            }


        });

        customAdapter = new CustomAdapter(array);
        mRecyclerView.setAdapter(customAdapter);

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
    }

    private class Container{
        public String name, description, date;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id ==  android.R.id.home){
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adapter for listview
     */
    private class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder>{

        ArrayList<Container> dataList;

        public CustomAdapter(ArrayList<Container> dataList){
            this.dataList = new ArrayList<Container>(dataList);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_details_list_item, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.title.setText(dataList.get(position).name);
            holder.desc.setText(dataList.get(position).description);
            //holder.date.setText(String.format("Last done on %s", dataList.get(position).date));
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView desc;
            TextView date;
            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView)itemView.findViewById(R.id.title);
                desc = (TextView)itemView.findViewById(R.id.description);
                //date = (TextView)itemView.findViewById(R.id.date);
            }
        }
    }

}
