package com.pitstop;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.CarIssue;
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

    private Car dashboardCar;
    private CarIssue carIssue;

    ParseApplication application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_item);

        application = (ParseApplication) getApplicationContext();

        Intent intent = getIntent();
        dashboardCar = (Car) intent.getSerializableExtra(MainActivity.CAR_EXTRA);
        carIssue = (CarIssue) intent.getSerializableExtra(MainActivity.CAR_ISSUE_EXTRA);

        setUpDisplayItems(carIssue);

        try {
            application.getMixpanelAPI().track("View Appeared",
                    new JSONObject("{'View':'DisplayItemActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
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

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, false);
        setResult(MainActivity.RESULT_OK, intent);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void requestService(View view) {
        try {
            application.getMixpanelAPI().track("Button Clicked",
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
        HashMap<String,Object> output = new HashMap<>();
        List<HashMap<String,String>> services = new ArrayList<>();

        if(carIssue.getIssueType().equals("recall")) {
            /*LocalDataRetriever dataRetriever = new LocalDataRetriever(this);

            Recalls recall =  new Recalls();
            recall.setValue("item", carIssue.getIssueDetail().getItem());
            recall.setValue("action",carIssue.getIssueDetail().getAction());
            recall.setValue("itemDescription",carIssue.getIssueDetail().getDescription());
            recall.setValue("priority",String.valueOf(carIssue.getPriority())); // high priority for recall
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
            output.put("comments", additionalComment);*/

        } else if(carIssue.getIssueType().equals("dtc")) {

            HashMap<String, String> dtc = new HashMap<>();
            dtc.put("item", carIssue.getIssueDetail().getItem());
            dtc.put("action", carIssue.getIssueDetail().getAction());
            dtc.put("itemDescription", carIssue.getIssueDetail().getDescription());
            dtc.put("priority", String.valueOf(carIssue.getPriority()));
            services.add(dtc);

            output.put("services", services);
            output.put("carVin", dashboardCar.getVin());
            output.put("userObjectId", userId);
            output.put("comments", additionalComment);


        } else {
            HashMap<String, String> service = new HashMap<>();
            service.put("item",carIssue.getIssueDetail().getItem());
            service.put("action",carIssue.getIssueDetail().getAction());
            service.put("itemDescription",carIssue.getIssueDetail().getDescription());
            service.put("priority",String.valueOf(carIssue.getPriority()));
            services.add(service);

            output.put("services", services);
            output.put("carVin", dashboardCar.getVin());
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
                onBackPressed();
            }
        });
    }

    private void setUpDisplayItems(CarIssue carIssue) {

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.item_display);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //list the information
        View view = getLayoutInflater().inflate(R.layout.activity_display_item_item,null);
        RelativeLayout rLayout = (RelativeLayout) view.findViewById(R.id.severity_indicator_layout);
        TextView severityTextView = (TextView) view.findViewById(R.id.severity_text);

        String title = carIssue.getIssueDetail().getAction() +" "
                + carIssue.getIssueDetail().getItem();
        String description = carIssue.getIssueDetail().getDescription();
        int severity =  carIssue.getPriority();

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

        if (linearLayout != null) {
            linearLayout.addView(view, 0);
        }
    }
}
