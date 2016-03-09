package com.pitstop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;
import com.pitstop.parse.ParseApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

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
    private static final String SERVICES_PRIORITY_DEFAULT_VALUE = "1";


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
            if(model.getValue(PRIORITY_KEY) == null) {
                model.setValue(PRIORITY_KEY,SERVICES_PRIORITY_DEFAULT_VALUE);
            }
            setUpDisplayItems(model,null);
        }

        try {
            ParseApplication.mixpanelAPI.track("View Appeared", new JSONObject("{'View':'DisplayItemActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ParseApplication.mixpanelAPI.flush();
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
        try {
            ParseApplication.mixpanelAPI.track("Button Clicked",
                    new JSONObject("{'Button':'Request Service','View':'DisplayItemActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(isFinishing()) {
            return;
        }

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(DisplayItemActivity.this);
        alertDialog.setTitle("Enter additional comment");

        final String[] additionalComment = {""};
        final EditText userInput = new EditText(DisplayItemActivity.this);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT);
        alertDialog.setView(userInput);

        alertDialog.setPositiveButton("SEND", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                additionalComment[0] = userInput.getText().toString();
                sendRequest(additionalComment[0]);
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alertDialog.show();
    }

    private void sendRequest(String additionalComment) {
        String userId = ParseUser.getCurrentUser().getObjectId();
        HashMap<String,Object> output = new HashMap<String, Object>();
        List<HashMap<String,String>> services = new ArrayList<>();

        DBModel model = (DBModel)getIntent().getSerializableExtra("Model");
        String VIN = getIntent().getStringExtra("VIN");

        if(model instanceof Recalls) {
            LocalDataRetriever dataRetriever = new LocalDataRetriever(this);

            Recalls recall =  new Recalls();
            recall.setValue("item", model.getValue("name"));
            recall.setValue("action","Recall For");
            recall.setValue("itemDescription",model.getValue("description"));
            recall.setValue("priority",""+ 6); // high priority for recall
            services.add(recall.getValues());

            //update db
            model.setValue("state","pending");
            HashMap<String,String> tmp = new HashMap<>();
            tmp.put("state","pending");
            dataRetriever.updateData("Recalls", "RecallID", model.getValue("RecallID"),tmp);

            ParseQuery query = new ParseQuery("RecallEntry");
            query.whereEqualTo("objectId",model.getValue("RecallID"));
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e == null && !objects.isEmpty()) {
                        ParseObject objToUpdate = objects.get(0);
                        objToUpdate.put("state", "pending");
                        objToUpdate.saveEventually();
                    }
                }
            });

            output.put("services", services);
            output.put("carVin", VIN);
            output.put("userObjectId", userId);
            output.put("comments", additionalComment);



        } else if(model instanceof DTCs) {
            DTCs dtc = new DTCs();
            dtc.setValue("item", model.getValue("dtcCode"));
            dtc.setValue("action","Engine Issue: DTC Code");
            dtc.setValue("itemDescription",model.getValue("description"));
            dtc.setValue("priority", "" + 5); // must be 5
            services.add(dtc.getValues());

            output.put("services", services);
            output.put("carVin", VIN);
            output.put("userObjectId", userId);
            output.put("comments", additionalComment);


        } else if(model instanceof Services) {
            services.add(model.getValues());

            output.put("services", services);
            output.put("carVin", VIN);
            output.put("userObjectId", userId);
            output.put("comments",additionalComment);
        }

        ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output, new FunctionCallback<Object>() {
            @Override
            public void done(Object object, ParseException e) {
                if (e == null) {
                    Toast.makeText(DisplayItemActivity.this,
                            "Request sent", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(DisplayItemActivity.this,
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
        super.onBackPressed();
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
