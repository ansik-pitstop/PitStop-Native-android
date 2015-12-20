package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pitstop.database.DBModel;
import com.pitstop.database.models.Recalls;

import java.util.HashMap;

public class DisplayItemActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_item);
        DBModel model = (DBModel)getIntent().getSerializableExtra("Model");
        if(model instanceof Recalls){
            setTitle("Recall");
        }else{
            setTitle("Service");
        }
        HashMap<String,String> info = model.getValues();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.item_display);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        for (String key : info.keySet()){
            View view = getLayoutInflater().inflate(R.layout.activity_display_item_item,null);
            ((TextView)view.findViewById(R.id.title)).setText(key);
            ((TextView)view.findViewById(R.id.description)).setText(info.get(key));
             linearLayout.addView(view, 0);
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_item, menu);
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
        if (id ==  android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
