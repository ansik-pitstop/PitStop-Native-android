package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Created by David Liu on 1/30/2016.
 */
public class CarHistoryActivity extends AppCompatActivity {
    CustomAdapter customAdapter;
    ArrayList<Container> array;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_history);
        array = new ArrayList<>();
        // set up listview
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.history_recycler_view);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        final ParseQuery query = new ParseQuery("ServiceHistory");
        query.whereEqualTo("carId",getIntent().getStringExtra("carId"));
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                ArrayList<HashSet<String>> storage = new ArrayList<HashSet<String>>();
                storage.add(new HashSet<String>());
                storage.add(new HashSet<String>());
                storage.add(new HashSet<String>());
                storage.add(new HashSet<String>());
                for (ParseObject object : objects){
                    if(object.get("type")==null){
                        storage.get(3).add(object.getString("serviceObjectId"));
                    }else if(object.getInt("type")==0){
                        storage.get(0).add(object.getString("serviceObjectId"));
                    }else if(object.getInt("type")==1){
                        storage.get(2).add(object.getString("serviceObjectId"));
                    }else{
                        storage.get(1).add(object.getString("serviceObjectId"));
                    }
                }
                String[] a = new String[]{"EdmundsService", "ServiceInterval", "ServiceFixed", "RecallEntry"};
                for (int i = 0; i<a.length; i++) {
                    ParseQuery intervalQuery = new ParseQuery(a[i]);
                    intervalQuery.whereContainedIn("objectId", storage.get(i));
                    intervalQuery.findInBackground(new FindCallback<ParseObject>() {

                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            for (ParseObject obj : objects) {
                                Container con = new Container();
                                con.name = obj.getString("item");
                                con.description = obj.getString("itemDescription");
                                array.add(con);
                            }
                            customAdapter.dataList.clear();
                            customAdapter.dataList.addAll(array);
                            customAdapter.notifyDataSetChanged();
                        }
                    });
                }
            }


        });

        customAdapter = new CustomAdapter(array);
        mRecyclerView.setAdapter(customAdapter);
    }

    private class Container{
        public String name, description;
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
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.title.setText(dataList.get(position).name);
            holder.desc.setText(dataList.get(position).description);
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView title;
            TextView desc;
            public ViewHolder(View itemView) {
                super(itemView);
                title = (TextView)itemView.findViewById(R.id.title);
                desc = (TextView)itemView.findViewById(R.id.description);
            }
        }
    }

}
