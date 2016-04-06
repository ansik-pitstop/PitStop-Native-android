package com.pitstop;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.github.mikephil.charting.charts.PieChart;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.R;
import com.pitstop.background.BluetoothAutoConnectService;

import java.util.HashMap;
import java.util.List;

public class CarScanActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {

    private BluetoothAutoConnectService autoConnectService;

    private RelativeLayout loadingRecalls, recallsStateLayout, recallsCountLayout;
    private ImageView recallsState;
    private TextView recallsText, recallsCount;

    private RelativeLayout loadingServices, servicesStateLayout, servicesCountLayout;
    private ImageView servicesState;
    private TextView servicesText, servicesCount;

    private RelativeLayout loadingEngineIssues, engineIssuesStateLayout, engineIssuesCountLayout;
    private ImageView engineIssuesState;
    private TextView engineIssuesText, engineIssuesCount;

    private TextView carMileage;
    private Button carScanButton;

    private static String TAG = "CarScanActivity";

    private boolean askingForDtcs = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            Log.i(TAG, "onServiceConnection");
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            autoConnectService = binder.getService();
            autoConnectService.setCallbacks(CarScanActivity.this); // register
            if (BluetoothAdapter.getDefaultAdapter()!=null) {
                autoConnectService.startBluetoothSearch();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {

            Log.i("Disconnecting","onServiceConnection");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_scan);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        bindService(MainActivity.serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        setupUiReferences();

        Car dashboardCar = (Car) getIntent().getSerializableExtra(MainActivity.CAR_EXTRA);
        carMileage.setText(String.valueOf(dashboardCar.getTotalMileage()));


    }

    @Override
    protected void onPause() {
        unbindService(serviceConnection);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        data.putExtra(MainActivity.REFRESH_LOCAL, true);
        setResult(MainActivity.RESULT_OK, data);
        finish();
    }

    private void setupUiReferences() {
        carMileage = (TextView) findViewById(R.id.car_mileage);

        carScanButton = (Button) findViewById(R.id.car_scan_btn);
        carScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(autoConnectService.isCommunicatingWithDevice()) {
                    updateMileage();
                } else {
                    tryAgainDialog();
                }
            }
        });

        recallsText = (TextView) findViewById(R.id.recalls_text);
        servicesText = (TextView) findViewById(R.id.services_text);
        engineIssuesText = (TextView) findViewById(R.id.engine_issues_text);

        recallsCount = (TextView) findViewById(R.id.recalls_count);
        servicesCount = (TextView) findViewById(R.id.services_count);
        engineIssuesCount = (TextView) findViewById(R.id.engine_issues_count);

        loadingRecalls = (RelativeLayout) findViewById(R.id.loading_recalls);
        loadingRecalls.setVisibility(View.GONE);
        recallsCountLayout = (RelativeLayout) findViewById(R.id.recalls_count_layout);

        recallsStateLayout = (RelativeLayout) findViewById(R.id.recalls_state_layout);
        recallsStateLayout.setVisibility(View.GONE);
        recallsState = (ImageView) findViewById(R.id.recalls_state_image_icon);

        loadingServices = (RelativeLayout) findViewById(R.id.loading_services);
        loadingServices.setVisibility(View.GONE);
        servicesCountLayout = (RelativeLayout) findViewById(R.id.services_count_layout);

        servicesStateLayout = (RelativeLayout) findViewById(R.id.services_state_layout);
        servicesStateLayout.setVisibility(View.GONE);
        servicesState = (ImageView) findViewById(R.id.services_state_image_icon);

        loadingEngineIssues = (RelativeLayout) findViewById(R.id.loading_engine_issues);
        loadingEngineIssues.setVisibility(View.GONE);
        engineIssuesCountLayout = (RelativeLayout) findViewById(R.id.engine_issues_count_layout);

        engineIssuesStateLayout = (RelativeLayout) findViewById(R.id.engine_issues_state_layout);
        engineIssuesStateLayout.setVisibility(View.GONE);
        engineIssuesState = (ImageView) findViewById(R.id.engine_issues_state_image_icon);
    }

    public void updateMileage() {

        final EditText input = new EditText(CarScanActivity.this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setRawInputType(Configuration.KEYBOARD_12KEY);

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CarScanActivity.this);

        alertDialog.setTitle("Update Mileage");
        alertDialog.setView(input);
        alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                carScanButton.setEnabled(false);
                recallsCountLayout.setVisibility(View.GONE);
                loadingRecalls.setVisibility(View.VISIBLE);
                recallsText.setText("Checking for recalls");

                servicesCountLayout.setVisibility(View.GONE);
                loadingServices.setVisibility(View.VISIBLE);
                servicesText.setText("Checking for services");

                engineIssuesCountLayout.setVisibility(View.GONE);
                loadingEngineIssues.setVisibility(View.VISIBLE);
                engineIssuesText.setText("Checking for engine issues");

                updateMileage(input.getText().toString());
                dialogInterface.dismiss();
            }
        }).show();
    }

    private void updateMileage (String mileage) {
        Log.i(TAG, mileage);

        carMileage.setText(mileage);
        Car dashboardCar = (Car) getIntent().getSerializableExtra(MainActivity.CAR_EXTRA);

        final HashMap<String, Object> params = new HashMap<String, Object>();

        params.put("carVin", dashboardCar.getVin());
        params.put("mileage", Integer.valueOf(mileage));

        // update the server information
        ParseCloud.callFunctionInBackground("carServicesUpdate", params, new FunctionCallback<Object>() {
            public void done(Object o, ParseException e) {
                if (e == null) {
                    startCarScan();
                } else {
                    Toast.makeText(CarScanActivity.this,
                            "failed to update mileage", Toast.LENGTH_SHORT).show();
                    Log.i(TAG, "Parse Error: " + e.getMessage());

                    carScanButton.setEnabled(true);
                    recallsCountLayout.setVisibility(View.VISIBLE);
                    loadingRecalls.setVisibility(View.GONE);
                    recallsText.setText("Recalls");

                    servicesCountLayout.setVisibility(View.VISIBLE);
                    loadingServices.setVisibility(View.GONE);
                    servicesText.setText("Services");

                    engineIssuesCountLayout.setVisibility(View.VISIBLE);
                    loadingEngineIssues.setVisibility(View.GONE);
                    engineIssuesText.setText("Engine issues");
                }
            }
        });
    }

    private void startCarScan() {

        checkForRecalls();

        checkForServices();

        checkForEngineIssues();
    }

    private void checkForRecalls() {
        recallsText.setText("No recalls");
        loadingRecalls.setVisibility(View.GONE);
        recallsStateLayout.setVisibility(View.VISIBLE);
    }

    private void checkForServices() {

        String userId = "";

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        if (ParseUser.getCurrentUser() != null) {
            userId = ParseUser.getCurrentUser().getObjectId();
        }
        query.whereContains("owner", userId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if (e == null) {
                    Car currentCar = getMainCar(Car.createCarsList(objects));

                    if (currentCar != null) {
                        loadingServices.setVisibility(View.GONE);

                        int totalServiceCount = 0;
                        totalServiceCount += currentCar.getPendingEdmundServicesIds().size();
                        totalServiceCount += currentCar.getPendingFixedServicesIds().size();
                        totalServiceCount += currentCar.getPendingIntervalServicesIds().size();

                        if (totalServiceCount > 0) {
                            servicesCountLayout.setVisibility(View.VISIBLE);
                            servicesCount.setText(String.valueOf(currentCar.getNumberOfServices()));
                            servicesText.setText("Services");

                            Drawable background = servicesCountLayout.getBackground();
                            GradientDrawable gradientDrawable = (GradientDrawable) background;
                            gradientDrawable.setColor(Color.rgb(203, 77, 69));

                        } else {
                            servicesStateLayout.setVisibility(View.VISIBLE);
                        }

                    } else {
                        Log.i(TAG, "Main car is null");
                    }

                    if (!servicesStateLayout.isShown() && !recallsStateLayout.isShown() &&
                            !engineIssuesStateLayout.isShown()) {
                        carScanButton.setEnabled(true);
                    }
                } else {
                    Log.i(TAG, e.getMessage());
                }
            }
        });
    }

    private void checkForEngineIssues() {
        askingForDtcs = true;
        autoConnectService.getPendingDTCs();
        autoConnectService.getDTCs();
    }

    private void tryAgainDialog() {

        if(isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(CarScanActivity.this);
        alertDialog.setTitle("Device not connected");

        // Alert message
        alertDialog.setMessage("Make sure your vehicle engine is on and " +
                "OBD device is properly plugged in.\n\nTry again ?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    autoConnectService.startBluetoothSearch();
                }
                (carScanButton).performClick();
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();

            }
        });
        alertDialog.show();
    }

    private Car getMainCar(List<Car> cars) {
        for(Car car : cars) {
            if(car.isCurrentCar()) {
                return car;
            }
        }
        return null;
    }

    public void checkServices(View view) {
        onBackPressed();
    }

    @Override
    public void getBluetoothState(int state) {

    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(TAG, "Result "+dataPackageInfo.result);
        Log.i(TAG, "DTC "+dataPackageInfo.dtcData);

        if(dataPackageInfo.dtcData != null && !dataPackageInfo.dtcData.equals("") && askingForDtcs) {

            String[] dtcs = dataPackageInfo.dtcData.split(",");

            engineIssuesCountLayout.setVisibility(View.VISIBLE);
            engineIssuesCount.setText(String.valueOf(dtcs.length));
            engineIssuesText.setText("Engine issues");

            Drawable background = engineIssuesCountLayout.getBackground();
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setColor(Color.rgb(203, 77, 69));

            if (!servicesStateLayout.isShown() && !recallsStateLayout.isShown() &&
                    !engineIssuesStateLayout.isShown()) {
                carScanButton.setEnabled(true);
            }

            askingForDtcs = false;
        } else if(askingForDtcs && (dataPackageInfo.dtcData == null || dataPackageInfo.dtcData.equals("") )) {
            engineIssuesStateLayout.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }
}
