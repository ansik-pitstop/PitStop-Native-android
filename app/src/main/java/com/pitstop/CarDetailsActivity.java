package com.pitstop;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;

import java.util.ArrayList;

public class CarDetailsActivity extends AppCompatActivity {

    private CustomAdapter customAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);
        setTitle(getIntent().getExtras().getString("title").toUpperCase());
        customAdapter = new CustomAdapter(this);
        ((ListView)findViewById(R.id.car_event_listview)).setAdapter(customAdapter);
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

        ArrayList<String[]> array = new ArrayList<>();
        Context context;
        public CustomAdapter(Context c){
            context = c;
            for(int i =0; i<25;i++){
                array.add(new String[]{
                        "asdfasd","ASfdsa","sadfads"
                });
            }
        }

        @Override
        public int getCount() {
            return array.size();
        }

        @Override
        public Object getItem(int i) {
            return array.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            RelativeLayout convertview = (RelativeLayout)view;
            if(view==null){
                LayoutInflater inflater = LayoutInflater.from(context);
                convertview = (RelativeLayout)inflater.inflate(R.layout.car_details_list_item, null);
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
                params.setMargins(10,10,10,10);
                convertview.setLayoutParams(params);
            }
            return convertview;
        }
    }
}
