package com.pitstop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;
import com.castel.obd.util.ObdDataUtil;
import com.castel.obd.util.Utils;
import com.google.android.gms.vision.barcode.Barcode;
import com.pitstop.BarcodeScanner.BarcodeScanner;
import com.pitstop.BarcodeScanner.BarcodeScannerBuilder;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.CarDataManager;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class AddCarActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener,
        View.OnClickListener, EasyPermissions.PermissionCallbacks {

    private GlobalApplication application;
    private MixpanelHelper mixpanelHelper;

    public static int ADD_CAR_SUCCESS = 51;
    private String VIN = "", scannerID = "", mileage = "", dtcs ="";

    private int shopSelected = 1;

    private boolean askForDTC = false;

    private BluetoothAutoConnectService autoConnectService;
    private boolean serviceIsBound;

    private ProgressDialog dialog;

    private ToggleButton yesButton, noButton;
    private Button scannerButton, abstractButton;

    private EditText vinEditText, mileageEditText;
    private TextView vinHint, searchForCarInfo;

    private LinearLayout vinSection;

    private NetworkHelper networkHelper;

    /** is true when bluetooth has failed enough that we want to show the manual VIN entry UI */
    private boolean hasBluetoothVinEntryFailed = false;

    private boolean isSearchingForCar = false, isGettingVinAndCarIsConnected = false;

    private static final int RC_LOCATION_PERM = 101;
    private static final int RC_PENDING_ADD_CAR = 102;

    private CallMashapeAsync vinDecoderApi;
    private LocalCarAdapter localCarAdapter;

    private Intent intentFromMainActivity;

    private static String TAG = AddCarActivity.class.getSimpleName();

    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            serviceIsBound = true;
            autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            autoConnectService.setCallbacks(AddCarActivity.this); // register
            Log.i(TAG, "connecting: onServiceConnection");

            if (BluetoothAdapter.getDefaultAdapter()!=null) {

                if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, AppMasterActivity.RC_ENABLE_BT);
                    return;
                }

                if(EasyPermissions.hasPermissions(AddCarActivity.this, AppMasterActivity.LOC_PERMS)) {
                    autoConnectService.startBluetoothSearch();
                } else {
                    EasyPermissions.requestPermissions(AddCarActivity.this,
                            getString(R.string.location_request_rationale),
                            RC_LOCATION_PERM, AppMasterActivity.LOC_PERMS);
                }

            }


        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            serviceIsBound = false;
            autoConnectService = null;
            Log.i("Disconnecting","onServiceConnection");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);
        Log.i(TAG, "onCreate()");

        networkHelper = new NetworkHelper(getApplicationContext());
        application = (GlobalApplication) getApplicationContext();
        mixpanelHelper = new MixpanelHelper(application);
        intentFromMainActivity = getIntent();
        vinDecoderApi = new CallMashapeAsync();
        localCarAdapter = new LocalCarAdapter(this);

        setupUIReferences();

        bindService(new Intent(AddCarActivity.this, BluetoothAutoConnectService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);

        //watch the number of characters in the textbox for VIN
        vinEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Editable vin = vinEditText.getText();

                String whitespaceRemoved = String.valueOf(vin);
                whitespaceRemoved = whitespaceRemoved.replace(" ", "").replace("\t", "")
                        .replace("\r", "").replace("\n", "");

                if (String.valueOf(vin).equals(whitespaceRemoved)) {
                    if (isValidVin(vin.toString())) {
                        Log.i(TAG,"AfterTextChanged -- valid vin");
                        abstractButton.setVisibility(View.VISIBLE);
                        scannerButton.setVisibility(View.GONE);
                        abstractButton.setEnabled(true);
                    } else {
                        Log.i(TAG,"AfterTextChanged -- Vin not valid");
                        abstractButton.setVisibility(View.GONE);
                        scannerButton.setVisibility(View.VISIBLE);
                        abstractButton.setEnabled(false);
                    }
                } else {
                    Log.i(TAG, "whitespace in VIN input removed. Original input: " + vin);
                    vinEditText.setText(whitespaceRemoved);
                }
            }
        });

        // Select shop
        Log.i(TAG,"Select dealership");
        Intent intent = new Intent(this,SelectDealershipActivity.class);
        intent.putExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD, intentFromMainActivity != null
                && intentFromMainActivity.getBooleanExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD,false));
        startActivityForResult(intent,
                SelectDealershipActivity.RC_DEALERSHIP);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_car, menu);
        return true;
    }

    @Override
    protected void onResume() {
        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        application.getMixpanelAPI().flush();
        if(vinDecoderApi!=null && vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
            vinDecoderApi.cancel(true);
            vinDecoderApi = null;
        }

        //hideLoading();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        hideLoading();

        if(serviceIsBound) {
            unbindService(serviceConnection);
        }

        if(vinDecoderApi!=null && vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
            vinDecoderApi.cancel(true);
            vinDecoderApi = null;
        }

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(dialog.isShowing()) {
            hideLoading();
            return;
        }

        try {
            mixpanelHelper.trackButtonTapped("Back", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Intent intent = new Intent(this,SelectDealershipActivity.class);
        intent.putExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD, intentFromMainActivity != null
                && intentFromMainActivity.getBooleanExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD,false));
        startActivityForResult(intent, SelectDealershipActivity.RC_DEALERSHIP);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            try {
                mixpanelHelper.trackButtonTapped("Settings", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent i = new Intent(AddCarActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        if(id == android.R.id.home) {
            Intent intent = getIntent();
            if(intent!=null && intent.getBooleanExtra(AppMasterActivity.HAS_CAR_IN_DASHBOARD,false)){
                super.onBackPressed();
            } else {
                Toast.makeText(this,"There are no cars in your dashboard",Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == SelectDealershipActivity.RC_DEALERSHIP
                && resultCode == SelectDealershipActivity.RESULT_OK) {
            if(data != null) {
                int selectedShopId =
                        data.getIntExtra(SelectDealershipActivity.SELECTED_DEALERSHIP, 1);
                setDealership(selectedShopId);
            }

        } else if(requestCode == AppMasterActivity.RC_ENABLE_BT
                && resultCode == AppMasterActivity.RC_ENABLE_BT) {
            beginSearchForCar();
        } else if(requestCode == RC_PENDING_ADD_CAR
                && resultCode == AppMasterActivity.RESULT_OK) {
            resumeFromPendingAddCar(data);

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void resumeFromPendingAddCar(Intent intent) {
        //setup restore possibilities for pending activity
        Log.i(TAG, "Resume from pending--");

        if(Utils.isEmpty(mileage)) {
            Toast.makeText(this,"Please enter mileage",Toast.LENGTH_SHORT).show();
            return;
        }

        if(!TextUtils.isEmpty(VIN) ) {
            Log.i(TAG, "Vin is not empty");

            Log.i(TAG,"Adding car from pending ---");

            showLoading("Adding car");

            runVinTask();
        }
    }

    /**
     * Button clicked for getting VIN
     * @param view
     */
    private long searchTime = 0;

    public void searchForCar(View view) {
        Log.i(TAG,"Searching for car");

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);

        if(TextUtils.isEmpty(mileageEditText.getText().toString())) {
            Toast.makeText(this, "Please enter mileage", Toast.LENGTH_SHORT).show();
            return;
        } else if(mileageEditText.getText().toString().length() > 9) {
            Toast.makeText(this, "Please enter valid mileage", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading("Adding car");

        mileage = mileageEditText.getText().toString();
        if (isValidVin(vinEditText.getText().toString())) { // valid VIN is already entered

            // Vin is manually entered or retrieved using the barcode scanner
            try {
                mixpanelHelper.trackCarAdded(TAG, mileage, MixpanelHelper.ADDED_MANUALLY);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.i(TAG, "Vin is valid -- (searching for car)");
            VIN = vinEditText.getText().toString();
            makeCar();

        } else {
            if (BluetoothAdapter.getDefaultAdapter() == null) { // Device doesn't support bluetooth
                hideLoading();
                vinSection.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Your device does not support bluetooth",
                        Toast.LENGTH_SHORT).show();
            } else {
                if (autoConnectService.getState() == IBluetoothCommunicator.CONNECTED) { // Already connected to module

                    try {
                        mixpanelHelper.trackCarAdded(TAG, mileage, MixpanelHelper.ADDED_WITH_DEVICE);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    showLoading("Getting car vin");
                    Log.i(TAG, "Getting car vin with device connected");
                    autoConnectService.getCarVIN();
                    vinRetrievalStartTime = System.currentTimeMillis();
                    isGettingVinAndCarIsConnected = true;
                    mHandler.postDelayed(vinDetectionRunnable, 3000);

                } else { // Need to search for module
                    showLoading("Searching for Car");

                    Log.i(TAG, "Searching for car but device not connected");
                    autoConnectService.startBluetoothSearch();
                    isSearchingForCar = true;
                    searchTime = System.currentTimeMillis();
                    if(view != null) {
                        connectionAttempts = 0;
                    }
                    mHandler.postDelayed(carSearchRunnable, 3000);
                }
            }
        }
    }

    private void tryAgainDialog() {

        hideLoading();
        isSearchingForCar = false;

        if(isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(AddCarActivity.this);
        alertDialog.setTitle("Device not connected");

        // Alert message
        alertDialog.setMessage("Could not connect to device. " +
                "\n\nMake sure your vehicle engine is on and " +
                "OBD device is properly plugged in.\n\nTry again ?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                (abstractButton).performClick();
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

    long vinRetrievalStartTime = 0;


    private Runnable vinDetectionRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - vinRetrievalStartTime;
            int seconds = (int) (timeDiff / 1000);

            if(seconds > 15 && isGettingVinAndCarIsConnected) {
                mHandler.sendEmptyMessage(1);
                mHandler.post(vinDetectionRunnable);
                return;
            }

            if(!isGettingVinAndCarIsConnected) {
                mHandler.removeCallbacks(vinDetectionRunnable);
                return;
            }

            mHandler.post(vinDetectionRunnable);

        }
    };

    private int connectionAttempts = 0;

    private Runnable carSearchRunnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - searchTime;
            int seconds = (int) (timeDiff / 1000);
            if(seconds > 15 && (isSearchingForCar) && autoConnectService.getState()
                    != IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS) {
                mHandler.sendEmptyMessage(0);
                mHandler.removeCallbacks(carSearchRunnable);
            } else if (!isSearchingForCar) {
                mHandler.removeCallbacks(carSearchRunnable);
            } else {
                mHandler.post(carSearchRunnable);
            }
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0: {
                    if(connectionAttempts++ == 2) {
                        tryAgainDialog();
                    } else {
                        Log.i(TAG, "connection reattempt: " + connectionAttempts);
                        searchForCar(null);
                    }
                    break;
                }

                case 1: {
                    autoConnectService.getCarVIN();
                    vinRetrievalStartTime = System.currentTimeMillis();
                    break;
                }
            }
        }
    };

    /**
     * Create a new Car
     */
    private void makeCar() {
        Log.i(TAG,"makeCar() -- function");

        Log.i(TAG,"Making car -- make car function");

        if(autoConnectService.getState() == IBluetoothCommunicator.CONNECTED) {

            Log.i(TAG, "Now connected to device");

            showLoading("Loading car engine codes");
            askForDTC=true;
            Log.i(TAG,"Make car --- Getting DTCs");
            autoConnectService.getDTCs();
            autoConnectService.getPendingDTCs();
        } else {
            Log.i(TAG, "Device not connected");
            Log.i(TAG, "Checking internet connection");

            if(NetworkHelper.isConnected(this)) {
                Log.i(TAG, "Internet connection found");

                Log.i(TAG, "Adding car --- make car func");

                runVinTask();
            } else {
                Log.i(TAG, "No Internet");
                hideLoading();
                startPendingAddCarActivity();
            }
        }
    }

    private boolean checkBackCamera() {
        final int CAMERA_FACING_BACK = 0;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for(int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i,info);
            if(CAMERA_FACING_BACK == info.facing) {
                return true;
            }
        }
        return false;
    }

    public void startScanner(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Scan VIN Barcode", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!checkBackCamera()) {
            Toast.makeText(this,"This device does not have a back facing camera",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG,"Starting barcode scanner");
        new BarcodeScannerBuilder()
                .withActivity(AddCarActivity.this)
                .withEnableAutoFocus(true)
                .withCenterTracker()
                .withBackfacingCamera()
                .withText("Scanning for barcode...")
                .withResultListener(new BarcodeScanner.OnResultListener() {
                    @Override
                    public void onResult(Barcode barcode) {
                        VIN = barcode.displayValue;

                        String possibleEditedVin = checkVinForInvalidCharacter(VIN);
                        if(possibleEditedVin!=null) {
                            VIN = possibleEditedVin;
                        }

                        try {
                            application.getMixpanelAPI().track("Scanned VIN",
                                    new JSONObject("{'VIN':'" + VIN + "','Device':'Android'}"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        vinEditText.setText(VIN);
                        vinSection.setVisibility(View.VISIBLE);
                        Log.i(TAG, "Barcode read: " + barcode.displayValue);

                        if (isValidVin(VIN)) { // show add car button iff vin is valid
                            abstractButton.setVisibility(View.VISIBLE);
                            scannerButton.setVisibility(View.GONE);
                            abstractButton.setEnabled(true);
                        } else {
                            abstractButton.setVisibility(View.GONE);
                            scannerButton.setVisibility(View.VISIBLE);
                            Toast.makeText(AddCarActivity.this,"Invalid VIN",Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .build().startScan();
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(TAG,"getBluetoothState func--");
        if(state == IBluetoothCommunicator.BLUETOOTH_CONNECT_SUCCESS) {
            if(isSearchingForCar) {
                isSearchingForCar = false;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showLoading("Linking with Device, give it a few seconds");
                    }
                });
                Log.i(TAG,"Getting car vin --- getBluetoothState");
                autoConnectService.getCarVIN();
                vinRetrievalStartTime = System.currentTimeMillis();
                isGettingVinAndCarIsConnected = true;
                mHandler.post(vinDetectionRunnable);
            }
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {}

    /**
     * After resetting device time, the bluetooth connection is lost.
     * */
    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        Log.i(TAG,"Set parameter");
        if((responsePackageInfo.type+responsePackageInfo.value)
                .equals(ObdManager.RTC_TAG)) {
            // Once device time is reset, the obd device disconnects from mobile device
            Log.i(TAG, "Set parameter() device time is set-- starting bluetooth search");
            autoConnectService.startBluetoothSearch();
        }
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
        Log.i(TAG,"getParameterData()");

        if(parameterPackageInfo.value.get(0).tlvTag.equals(ObdManager.VIN_TAG)) {
            Log.i(TAG,"VIN response received");

            isGettingVinAndCarIsConnected = false;
            scannerID = parameterPackageInfo.deviceId;
            LogUtil.i("parameterPackage.size():"
                    + parameterPackageInfo.value.size());

            List<ParameterInfo> parameterValues = parameterPackageInfo.value;
            VIN = parameterValues.get(0).value;
            try {
                application.getMixpanelAPI().track("Retrieved VIN from device",
                        new JSONObject("{'VIN':'" + VIN + "','Device':'Android'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (isValidVin(VIN)) {

                Log.i(TAG,"VIN is valid");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        vinEditText.setText(VIN);
                        showLoading("Loaded car vin");
                        makeCar();
                    }
                });

            } else {
                // same as in manual input plus vin hint
                Log.i(TAG, "Vin value returned not valid");
                Log.i(TAG,"VIN: "+VIN);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        showManualEntryUI();
                        vinHint.setVisibility(View.VISIBLE);
                    }
                });
            }
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(TAG, "getIOData()");
        Log.i(TAG, "result: "+dataPackageInfo.result);

        if (dataPackageInfo.result == 6 && askForDTC) {
            Log.i(TAG,"Result: "+dataPackageInfo.result+ " Asking for dtcs --getIOData()");
            dtcs = "";
            if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                Log.i(TAG,"Parsing DTCs");
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for(String dtc : DTCs) {
                    dtcs+= ObdDataUtil.parseDTCs(dtc)+",";
                }
            }

            Log.i(AppMasterActivity.TAG, "getIOData --- Adding car");
            if(NetworkHelper.isConnected(this)){
                Log.i(TAG, "Internet connection found");
                runVinTask();
            } else {
                Log.i(TAG, "No internet");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        hideLoading();
                        startPendingAddCarActivity();
                    }
                });
            }
            askForDTC=false;
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(ObdManager.DEVICE_LOGOUT_FLAG))) {
            Log.i(TAG, "Device disconnected");
        }
    }

    @Override
    public void onClick(View view) {
        if (view == yesButton || view == noButton) {
            // this flag will be true after the block if the buttons have changed.
            // if the yes button was checked and they press it => newAction = false
            // if the yes button was checked and they press the no button => newAction = true
            boolean newAction = true;

            // after this block, exactly one of the yes and no buttons will be checked
            if (view == yesButton) {
                if (yesButton.isChecked()) {
                    noButton.setChecked(false);
                } else if (!noButton.isChecked()) {
                    yesButton.setChecked(true);
                    newAction = false;
                }
            } else {
                if (noButton.isChecked()) {
                    yesButton.setChecked(false);
                } else if (!yesButton.isChecked()) {
                    noButton.setChecked(true);
                    newAction = false;
                }
            }

            if (newAction) {
                // don't hide the manual entry UI if we're showing it after bluetooth failure
                if (yesButton.isChecked() && !hasBluetoothVinEntryFailed) {
                    try {
                        mixpanelHelper.trackButtonTapped("Yes I have Pitstop Hardware", TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showBluetoothEntryUI();
                } else if (noButton.isChecked()) {
                    try {
                        mixpanelHelper.trackButtonTapped("No I do not have Pitstop Hardware", TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showManualEntryUI();
                }
            }
        }
    }

    private String checkVinForInvalidCharacter(String vin) {
        Log.i(TAG,"checkVinForInvalidCharacter--");
        if(vin!=null && vin.length() == 18 && vin.startsWith("I")) {
            return vin.substring(1,vin.length()-1);
        }
        return null;
    }

    boolean isValidVin(String vin) {
        Log.i(TAG,"isValidVin()-- func");
        return vin != null && vin.length() == 17;
    }
    /**
     * show Manual VIN UI
     */
    void showManualEntryUI() {
        Log.i(TAG, "showManual");
        vinSection.setVisibility(View.VISIBLE);
        searchForCarInfo.setText(getString(R.string.add_car_manual));
        abstractButton.setText("ADD CAR");

        String vin = String.valueOf(vinEditText.getText());
        ((ImageView) findViewById(R.id.obd_ports)).setImageDrawable(getResources()
                .getDrawable(R.drawable.illustration_car));

        Log.d("isValidVin() result", String.valueOf(isValidVin(vin)));

        if (isValidVin(vin)) {
            abstractButton.setVisibility(View.VISIBLE);
            scannerButton.setVisibility(View.GONE);
            abstractButton.setEnabled(true);
        }
        else {
            abstractButton.setVisibility(View.GONE);
            scannerButton.setVisibility(View.VISIBLE);
        }
    }

    /**
     * show Bluetooth auto UI
     */
    void showBluetoothEntryUI() {
        abstractButton.setEnabled(true);
        vinSection.setVisibility(View.GONE);
        searchForCarInfo.setText(getString(R.string.add_car_bluetooth));
        ((ImageView) findViewById(R.id.obd_ports)).setImageDrawable(getResources()
                .getDrawable(R.drawable.illustration_dashboard));
        abstractButton.setText("SEARCH FOR CAR");

        // TODO: scanner button should be in VIN_SECTION view

        abstractButton.setVisibility(View.VISIBLE);
        scannerButton.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult (int requestCode, String[] permissions,
                                            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == BarcodeScanner.CAM_PERM_REQ) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanner(null);
            } else {
                Snackbar.make(findViewById(R.id.add_car_root), R.string.camera_request_rationale,
                        Snackbar.LENGTH_LONG)
                        .setAction("Retry", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                ActivityCompat.requestPermissions(AddCarActivity.this,
                                        new String[]{android.Manifest.permission.CAMERA},
                                        BarcodeScanner.CAM_PERM_REQ);
                            }
                        })
                        .show();
            }
        } else {
            // Forward results to EasyPermissions
            EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
        }
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {

    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Toast.makeText(AddCarActivity.this,"Access to location not granted",
                Toast.LENGTH_SHORT).show();
    }


    /**
     * Hide the loading screen
     */
    private void hideLoading() {
        if(dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    /**
     * Show the loading screen
     */
    private void showLoading(String text) {
        dialog.setMessage(text);
        if(!dialog.isShowing()) {
            dialog.show();
        }
    }


    private void setDealership(int shopId) {
        shopSelected = shopId;
    }

    private int getDealership() {
        return shopSelected;
    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void beginSearchForCar() {
        abstractButton.performClick();
    }

    private void setupUIReferences() {
        Log.i(TAG,"Setting up ui...");
        yesButton = (ToggleButton)findViewById(R.id.yes_i_do_button);
        noButton = (ToggleButton)findViewById(R.id.no_i_dont_button);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        abstractButton = (Button) findViewById(R.id.button);
        scannerButton = (Button) findViewById(R.id.scannerButton);
        vinEditText = (EditText) findViewById(R.id.VIN);
        mileageEditText = (EditText) findViewById(R.id.mileage);
        vinHint = (TextView) findViewById(R.id.VIN_hint);
        searchForCarInfo = (TextView)findViewById(R.id.search_for_car_info);
        vinSection = (LinearLayout) findViewById(R.id.VIN_SECTION);

        dialog = new ProgressDialog(this);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (vinDecoderApi != null &&
                        vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
                    vinDecoderApi.cancel(true);
                    vinDecoderApi = null;
                }

                isSearchingForCar = false;
                mHandler.removeCallbacks(carSearchRunnable);
                mHandler.removeCallbacks(vinDetectionRunnable);
            }
        });
    }

    private void scannerIdCheck(final JSONObject carInfo) {
        Log.i(TAG,"ScannerIdCheck() -- func");

        vinCheck(carInfo);
    }

    private void vinCheck(final JSONObject carInfo) {
        Log.i(TAG, "vinCheck()");
        //check if car already exists!
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoading("Checking car vin");
            }
        });

        networkHelper.getCarsByVin(VIN, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if(requestError == null) {
                    if(response.equals("{}")) {
                        saveCarToServer(carInfo);
                    } else {
                        hideLoading();

                        Toast.makeText(AddCarActivity.this,"Car Already Exists!",
                                Toast.LENGTH_SHORT).show();

                        vinEditText.setText("");
                    }
                } else {
                    hideLoading();
                    Log.e(TAG, "Check vin: " + requestError.getMessage());
                    Toast.makeText(AddCarActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void saveCarToServer(JSONObject carInfo) {
        Log.i("shop selected:", String.valueOf(getDealership()));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                showLoading("Saving car details");
            }
        });

        networkHelper.createNewCar(application.getCurrentUserId(),
                mileage.equals("") ? 0 : Integer.valueOf(mileage),
                VIN,
                scannerID == null ? "" : scannerID,
                shopSelected,
                new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        showLoading("Final Touches");

                        if(requestError == null) {
                            Log.i(TAG, "Create car response: " + response);
                            try {
                                Car newCar = Car.createCar(response);

                                if(scannerID != null) {
                                    networkHelper.createNewScanner(newCar.getId(), scannerID, new RequestCallback() {
                                        @Override
                                        public void done(String response, RequestError requestError) {

                                        }
                                    });
                                }

                                returnToMainActivity(newCar);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                hideLoading();
                                Toast.makeText(AddCarActivity.this, "There was an error adding your car, please try again", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            hideLoading();
                            Log.e(TAG, "Create new car: " + requestError.getMessage());
                            Toast.makeText(AddCarActivity.this, "There was an error, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void startPendingAddCarActivity() {
        Intent intent = new Intent(AddCarActivity.this,PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, mileage);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_DTCS, dtcs);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, scannerID);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, VIN);
        startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    private void returnToMainActivity(Car addedCar) {

        PreferenceManager.getDefaultSharedPreferences(this).edit().putInt(MainDashboardFragment.pfCurrentCar, addedCar.getId()).commit();

        hideLoading();
        CarDataManager.getInstance().setDashboardCar(addedCar);
        Intent data = new Intent();
        //data.putExtra(AppMasterActivity.CAR_EXTRA, addedCar);
        data.putExtra(AppMasterActivity.REFRESH_FROM_SERVER, true);
        setResult(ADD_CAR_SUCCESS, data);
        finish();
    }

    private void runVinTask() {
        if(vinDecoderApi == null) {
            vinDecoderApi = new CallMashapeAsync();
        } else if(vinDecoderApi.getStatus().equals(AsyncTask.Status.PENDING)) {
            Log.i("VIN DECODER","Pending TASK");
        } else if(vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
            vinDecoderApi.cancel(true);
            vinDecoderApi = null;
            vinDecoderApi = new CallMashapeAsync();
        } else if(vinDecoderApi.getStatus().equals(AsyncTask.Status.FINISHED)) {
            vinDecoderApi = null;
            vinDecoderApi  = new CallMashapeAsync();
        }
        vinDecoderApi.execute();
    }

    /**
     * Complex call in adding car.
     * First it goes to Mashape and get info based on VIN -> determines if valid
     * Second it tries to add to database -> determines if already existing in Parse
     * Third it shows the Shops to choose from and add it to Parse.
     * Fourth uploads the DTCs to server (where server code will run to link to car)
     */
    private class CallMashapeAsync extends AsyncTask<String, Void, String>{



        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(String error) {

            Log.i(TAG, "On post execute");

            if(!Utils.isEmpty(error)) {
                hideLoading();
                Toast.makeText(AddCarActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        }

        protected String doInBackground(String... msg) {

            String error = "";

            try {

                StringBuilder response  = new StringBuilder();
                URL url = new URL("https://vindecoder.p.mashape.com/decode_vin?vin="+VIN);
                // Starts the query

                HttpURLConnection httpconn = (HttpURLConnection)url.openConnection();
                httpconn.addRequestProperty("Content-Type", "application/x-zip");
                httpconn.addRequestProperty("X-Mashape-Key", getString(R.string.mashape_key));
                httpconn.addRequestProperty("Accept", "application/json");
                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK)
                {
                    BufferedReader input = new BufferedReader(
                            new InputStreamReader(httpconn.getInputStream()),8192);
                    String strLine = null;
                    while ((strLine = input.readLine()) != null)
                    {
                        response.append(strLine);
                    }
                    input.close();
                }
                if(new JSONObject(response.toString()).getBoolean("success")) {
                    Log.i(TAG, "Call to mashape succeed");
                    //load mashape info
                    final JSONObject jsonObject =
                            new JSONObject(response.toString()).getJSONObject("specification");
                    // Check if scannerId already exists and if Vin already exists
                    scannerIdCheck(jsonObject);
                } else {
                    error = "Failed to find by VIN, may be invalid";
                }
            } catch (Exception e) {
                error = e.getMessage();
                e.printStackTrace();
            }
            return error;
        }
    }
}
