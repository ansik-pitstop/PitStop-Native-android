package com.pitstop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

public class CarDetailsActivity extends AppCompatActivity {

    private CustomAdapter customAdapter;
    private ArrayList<ListItem> arrayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);
        setTitle(getIntent().getExtras().getString("title").toUpperCase());
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Service");
        Object[] a = (Object[]) getIntent().getSerializableExtra("servicesDue");
        ArrayList<Integer> serviceCodes = new ArrayList<Integer>();
        for (int i = 0; i<a.length; i++){
            serviceCodes.add((int) a[i]);
        }
        query.whereContainedIn("serviceId", serviceCodes);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                for (ParseObject parseObject: objects) {
                    Service a = new Service();
                    a.title = parseObject.getString("item");
                    a.description = parseObject.getString("itemDescription");
                    a.priority = parseObject.getInt("priority");
                    a.action = parseObject.getString("action");
                    arrayList.add(a);
                }
                customAdapter.dataList.clear();
                customAdapter.dataList.addAll(arrayList);
                customAdapter.notifyDataSetChanged();
            }
        });

        //get recalls
        query = ParseQuery.getQuery("EdmundsRecall");

        a = (Object[]) getIntent().getSerializableExtra("pendingRecalls");
        ArrayList<String> recallCodes = new ArrayList<String>();
        for (int i = 0; i<a.length; i++){
            recallCodes.add(a[i].toString());
        }
        query.whereContainedIn("objectId", recallCodes);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                for (ParseObject parseObject : objects) {
                    Recall a = new Recall();
                    a.title = parseObject.getString("componentDescription");
                    a.description = parseObject.getString("defectDescription");
                    a.action = parseObject.getString("defectCorrectiveAction");
                    a.consequence = parseObject.getString("defectConsequence");
                    arrayList.add(a);
                }
                customAdapter.dataList.clear();
                customAdapter.dataList.addAll(arrayList);
                customAdapter.notifyDataSetChanged();
            }
        });

        customAdapter = new CustomAdapter(this,arrayList);
        ((ListView) findViewById(R.id.car_event_listview)).setAdapter(customAdapter);
        findViewById(R.id.evcheck_layout).setBackgroundColor(getResources().getColor(R.color.evcheck));
        ((TextView) findViewById(R.id.evcheck)).setTextColor(Color.WHITE);
        findViewById(R.id.evcheck_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CarDetailsActivity.this, EnviornmentalCheckActivity.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_car_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    class CustomAdapter extends BaseAdapter {

        ArrayList<ListItem> dataList;
        Context context;
        public CustomAdapter(Context c, ArrayList<ListItem> list){
            context = c;
            dataList = new ArrayList<>(list);
        }

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int i) {
            return dataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RelativeLayout convertview = (RelativeLayout)view;
            LayoutInflater inflater = LayoutInflater.from(context);
            convertview = (RelativeLayout)inflater.inflate(R.layout.car_details_list_item, null);
            ((TextView)convertview.findViewById(R.id.title)).setText(dataList.get(i).title);
            ((TextView)convertview.findViewById(R.id.description)).setText(dataList.get(i).description);
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            //params.setMargins(10, 10, 10, 10);
            if(dataList.get(i) instanceof Recall) {
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getDrawable(R.drawable.ic_error_red_600_24dp));
            }else{
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }
            convertview.setLayoutParams(params);
            return convertview;
        }
    }

    private class Service extends ListItem {
        String action;
        int priority;
    }
    private class Recall extends ListItem {
        String consequence, action;
    }

    private class ListItem {
        String title, description;
    }
}
