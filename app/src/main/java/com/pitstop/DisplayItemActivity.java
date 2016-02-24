package com.pitstop;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.pitstop.database.DBModel;
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;

import java.util.HashMap;

import static com.pitstop.R.drawable.severity_high_indicator;
import static com.pitstop.R.drawable.severity_low_indicator;
import static com.pitstop.R.drawable.severity_medium_indicator;
import static com.pitstop.R.drawable.severity_critical_indicator;

public class DisplayItemActivity extends AppCompatActivity {
    private static final String PRIORITY_KEY = "priority";
    private static final String ITEM_KEY = "item";
    private static final String ITEM_DESCRIPTION_KEY = "itemDescription";
    private static final String ACTION_KEY = "action";
    private static final String DTCCODE_KEY = "dtcCode";
    private static final String RECALLS_ITEM_KEY = "name";
    private static final String DESCRIPTION_KEY = "description";

    private static final String RECALLS_PRIORITY_DEFAULT_VALUE = "6";
    private static final String DTCS_PRIORITY_DEFAULT_VALUE = "5";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_item);

        DBModel model = (DBModel)getIntent().getSerializableExtra("Model");
        if(model instanceof Recalls){
            Log.i("TYPE RECALLS", "Recalls");
            setTitle("Recall");

            model.setValue(PRIORITY_KEY,RECALLS_PRIORITY_DEFAULT_VALUE);
            model.setValue(ITEM_KEY, model.getValue(RECALLS_ITEM_KEY));
            model.setValue(ITEM_DESCRIPTION_KEY, model.getValue(DESCRIPTION_KEY));
            setUpDisplayItems(model, "Recall for ");

        }else if(model instanceof DTCs){
            setTitle("Engine Code");
            Log.i("TYPE DTCS", "Engine Code");

            model.setValue(PRIORITY_KEY,DTCS_PRIORITY_DEFAULT_VALUE);
            model.setValue(ITEM_KEY,model.getValue(DTCCODE_KEY));
            model.setValue(ITEM_DESCRIPTION_KEY, model.getValue(DESCRIPTION_KEY));

			setUpDisplayItems(model, "Engine Issue: DTC code ");
        }else{
            setTitle("Service");
            Log.i("TYPE Service", "Service");
            setUpDisplayItems(model,null);
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

    public void requestService(View view) {
        Snackbar.make(view,"Service requested",Snackbar.LENGTH_LONG).show();
    }

    private void setUpDisplayItems(DBModel model, String action) {
        HashMap<String,String> info = model.getValues();
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.item_display);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //list the information
        View view = getLayoutInflater().inflate(R.layout.activity_display_item_item,null);
        RelativeLayout rLayout = (RelativeLayout) view.findViewById(R.id.severity_indicator_layout);
        TextView severityTextView = (TextView) view.findViewById(R.id.severity_text);

        String title = info.get(ITEM_KEY);
        String description = info.get(ITEM_DESCRIPTION_KEY);
        Log.i("RECALLS",info.get("item"));
        int severity =  Integer.parseInt(info.get(PRIORITY_KEY));

        if(action != null) {
            title = action + title;
        } else {
            title = info.get(ACTION_KEY) +" "+ title;
        }

        ((TextView)view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.description)).setText(description);

        switch (severity) {
            case 1:
                rLayout.setBackgroundDrawable(getResources().getDrawable(severity_low_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[0]);
                break;
            case 2:
                rLayout.setBackgroundDrawable(getResources().getDrawable(severity_medium_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[1]);
                break;
            case 3:
                rLayout.setBackgroundDrawable(getResources().getDrawable(severity_high_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[2]);
                break;
            default:
                rLayout.setBackgroundDrawable(getResources().getDrawable(severity_critical_indicator));
                severityTextView.setText(getResources().getStringArray(R.array.severity_indicators)[3]);
                break;
        }

        linearLayout.addView(view, 0);
    }
}
