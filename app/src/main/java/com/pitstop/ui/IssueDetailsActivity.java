package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.CarIssue;
import com.pitstop.database.LocalCarIssueAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.ServiceRequestUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

import io.smooch.core.Smooch;

import static com.pitstop.R.drawable.severity_high_indicator;
import static com.pitstop.R.drawable.severity_low_indicator;
import static com.pitstop.R.drawable.severity_medium_indicator;
import static com.pitstop.R.drawable.severity_critical_indicator;

public class IssueDetailsActivity extends AppCompatActivity {

    private Car dashboardCar;
    private CarIssue carIssue;

    private boolean fromHistory; // opened from history (no request service)

    private LocalCarIssueAdapter carIssueAdapter;

    GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    private NetworkHelper networkHelper;

    private static final String TAG = IssueDetailsActivity.class.getSimpleName();

    private BluetoothAutoConnectService autoConnectService;
    private Intent serviceIntent;

    private boolean needToRefresh = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG,"connecting: onServiceConnection");
            // cast the IBinder and get MyService instance

            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i(TAG,"Disconnecting: onServiceConnection");
            autoConnectService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_issue_details);

        networkHelper = new NetworkHelper(getApplicationContext());

        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);

        Intent intent = getIntent();
        dashboardCar = intent.getParcelableExtra(MainActivity.CAR_EXTRA);
        carIssue = intent.getParcelableExtra(MainActivity.CAR_ISSUE_EXTRA);
        fromHistory = intent.getBooleanExtra(CarHistoryActivity.ISSUE_FROM_HISTORY, false);

        setUpDisplayItems(carIssue);

        View requestServiceButton = findViewById(R.id.btnRequestService);
        View clearDtcButton = findViewById(R.id.btnClearDtc);
        if(fromHistory) {
            if (requestServiceButton != null) {
                requestServiceButton.setVisibility(View.INVISIBLE);
            }
            if (clearDtcButton != null) {
                clearDtcButton.setVisibility(View.INVISIBLE);
            }
        }

        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        serviceIntent = new Intent(this, BluetoothAutoConnectService.class);
        //startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        application.getMixpanelAPI().flush();
        unbindService(serviceConnection);
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
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, needToRefresh);
        setResult(MainActivity.RESULT_OK, intent);
        super.finish();
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    public void requestService(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Request Service", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(isFinishing()) {
            return;
        }

        new ServiceRequestUtil(this, dashboardCar).start();
    }

    private void setUpDisplayItems(CarIssue carIssue) {

        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.item_display);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //list the information
        View view = getLayoutInflater().inflate(R.layout.activity_issue_details_item,null);
        RelativeLayout rLayout = (RelativeLayout) view.findViewById(R.id.severity_indicator_layout);
        TextView severityTextView = (TextView) view.findViewById(R.id.severity_text);

        // clear DTCs will clear all DTCs in module and backend
        //if(carIssue.getIssueType().equals(CarIssue.PENDING_DTC) || carIssue.getIssueType().equals(CarIssue.DTC)) {
        //    View clearDtcButton = view.findViewById(R.id.btnClearDtc);
        //    clearDtcButton.setVisibility(View.VISIBLE);
        //    clearDtcButton.setOnClickListener(new View.OnClickListener() {
        //        @Override
        //        public void onClick(View v) {
        //            if(autoConnectService.getState() != IBluetoothCommunicator.CONNECTED) {
        //                Toast.makeText(IssueDetailsActivity.this, "Device must be connected", Toast.LENGTH_SHORT).show();
        //            } else {
        //                new AlertDialog.Builder(IssueDetailsActivity.this)
        //                        .setTitle("Are you sure you want to clear all engine codes?")
        //                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
        //                            @Override
        //                            public void onClick(DialogInterface dialogInterface, int i) {
        //                                autoConnectService.clearDTCs();
        //                                clearDtcs();
        //                                dialogInterface.dismiss();
        //                            }
        //                        })
        //                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
        //                            @Override
        //                            public void onClick(DialogInterface dialog, int which) {
        //                                dialog.cancel();
        //                            }
        //                        })
        //                        .show();
        //            }
        //        }
        //    });
        //}

        String title = carIssue.getAction() + " "
                + carIssue.getItem();
        String description = carIssue.getDescription();
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

    private class ErrorIndicator { // because boolean needs to be used in inner class
        boolean wasError = false;
    }

    private void clearDtcs() {

        carIssueAdapter = new LocalCarIssueAdapter(this);

        // marking each dtc as done in the backend and locally
        final ErrorIndicator errorIndicator = new ErrorIndicator();
        for(int i = 0 ; i < dashboardCar.getActiveIssues().size() ; i++) {
            final CarIssue issue = dashboardCar.getActiveIssues().get(i);
            final int index = i;
            if(issue.getIssueType().equals(CarIssue.DTC) || issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                networkHelper.serviceDone(dashboardCar.getId(), issue.getId(), 0, dashboardCar.getTotalMileage(),
                        new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(requestError != null) {
                                    issue.setStatus("done");
                                    dashboardCar.getActiveIssues().set(index, issue);
                                    carIssueAdapter.updateCarIssue(issue);
                                    errorIndicator.wasError = true;
                                }
                            }
                        });
            }
        }

        if(errorIndicator.wasError) {
            Toast.makeText(this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
        }

        List<CarIssue> issues = carIssueAdapter.getAllCarIssues(dashboardCar.getId());
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(System.currentTimeMillis());
        final String currentYear = String.valueOf(calendar.get(Calendar.YEAR));
        final String currentMonth = calendar.get(Calendar.MONTH) < 10 ? "0" + calendar.get(Calendar.MONTH)
                : String.valueOf(calendar.get(Calendar.MONTH));
        final String currentDay = calendar.get(Calendar.DAY_OF_MONTH) < 10 ? "0" + calendar.get(Calendar.DAY_OF_MONTH)
                : String.valueOf(calendar.get(Calendar.DAY_OF_MONTH));
        for(CarIssue issue : issues) {
            if(!issue.getStatus().equals(CarIssue.ISSUE_DONE) &&
                    issue.getIssueType().equals(CarIssue.DTC) || issue.getIssueType().equals(CarIssue.PENDING_DTC)) {
                issue.setStatus(CarIssue.ISSUE_DONE);
                issue.setDoneAt(currentYear + "-" + currentMonth + "-" + currentDay);
                carIssueAdapter.updateCarIssue(issue);
            }
        }

        needToRefresh = true;
    }
}
