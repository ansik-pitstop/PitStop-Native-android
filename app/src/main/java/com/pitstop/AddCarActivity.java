package com.pitstop;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.log.LogCatHelper;
import com.castel.obd.util.LogUtil;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.mixpanel.android.mpmetrics.MixpanelAPI;
import com.parse.ConfigCallback;
import com.parse.FindCallback;
import com.parse.ParseConfig;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.Debug.PrintDebugThread;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.background.BluetoothAutoConnectService.BluetoothBinder;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.InternetChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;


import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class AddCarActivity extends AppCompatActivity implements
        BluetoothManage.BluetoothDataListener, View.OnClickListener,
        EasyPermissions.PermissionCallbacks {

    private ParseApplication application;
    public static int RESULT_ADDED = 10;
    public static int ADD_CAR_SUCCESS = 51;
    // TODO: Transferring data through intents is safer than using global variables
    public static String VIN = "", scannerID = "", mileage = "", shopSelected = "", dtcs ="";
    private PrintDebugThread mLogDumper;
    private boolean bound;
    boolean makingCar = false;
    private BluetoothAutoConnectService service;
    private boolean askForDTC = false;
    private ArrayList<String> DTCData= new ArrayList<>();

    private ProgressDialog progressDialog;

    private ToggleButton yesButton;
    private ToggleButton noButton;
    private Button scannerButton;
    private Button abstractButton;

    private EditText vinEditText;
    private EditText mileageEditText;
    private TextView vinHint;
    private TextView searchForCarInfo;

    private LinearLayout vinSection;

    // Id to identify CAMERA permission request.
    //private static final int REQUEST_CAMERA = 0;
    private static final int RC_BARCODE_CAPTURE = 100;

    /** is true when bluetooth has failed enough that we want to show the manual VIN entry UI */
    private boolean hasBluetoothVinEntryFailed = false;
    //debugging storing TODO: Request permission for storage
    LogCatHelper mLogStore;

    private boolean isLoading = false;
    private boolean isGettingVin = false;

    private static final int RC_LOCATION_PERM = 101;

    private CallMashapeAsync vinDecoderApi;
    private String ACTIVITY_TAG = "ADD_CAR_DEBUG_TAG";

    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothBinder binder = (BluetoothBinder) service1;
            service = binder.getService();
            bound = true;
            service.setCallbacks(AddCarActivity.this); // register
            Log.i("connecting", "onServiceConnection");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
            Log.i("Disconnecting","onServiceConnection");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);
        Log.i(ACTIVITY_TAG, "onCreate()");

        application = (ParseApplication) getApplicationContext();

        vinDecoderApi = new CallMashapeAsync();

        setupUIReferences();

        bindService(MainActivity.serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        mLogStore = LogCatHelper.getInstance(this);

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
                        Log.i(ACTIVITY_TAG,"AfterTextChanged -- valid vin");
                        abstractButton.setVisibility(View.VISIBLE);
                        scannerButton.setVisibility(View.GONE);
                        abstractButton.setEnabled(true);
                    } else {
                        Log.i(ACTIVITY_TAG,"AfterTextChanged -- Vin not valid");
                        abstractButton.setVisibility(View.GONE);
                        scannerButton.setVisibility(View.VISIBLE);
                        abstractButton.setEnabled(false);
                    }
                } else {
                    Log.i(ACTIVITY_TAG, "whitespace in VIN input removed. Original input: " + vin);
                    vinEditText.setText(whitespaceRemoved);
                }
            }
        });

        try {
            application.getMixpanelAPI().track("View Appeared",
                    new JSONObject("{'View':'AddCarAcivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Select shop
        Log.i(ACTIVITY_TAG,"Select dealership");
        Intent intent = new Intent(this,SelectDealershipActivity.class);
        Intent intentMain = getIntent();
        intent.putExtra(MainActivity.HAS_CAR_IN_DASHBOARD, intentMain != null &&
                intentMain.getBooleanExtra(MainActivity.HAS_CAR_IN_DASHBOARD, false));
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
        super.onResume();
        if(BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
        mLogStore.start();
        //setup restore possibilities for pending activity
        Intent intent = getIntent();
        if(intent!=null && intent.getBooleanExtra(PendingAddCarActivity.PENDING,false)) {
            Log.i(ACTIVITY_TAG, "OnResume from pending--");
            if(TextUtils.isEmpty(mileage)) {
                Toast.makeText(this,"Please enter mileage",Toast.LENGTH_SHORT).show();
                return;
            }

            if(!TextUtils.isEmpty(VIN) ) {
                Log.i(ACTIVITY_TAG, "Vin is not empty");
                mileageEditText.setText(mileage);

                ParseConfig.getInBackground(new ConfigCallback() {

                    @Override
                    public void done(ParseConfig config, ParseException e) {
                        // TODO: Why is config returning null ?
                        if(config == null) {
                            return;
                        }

                        Log.i(ACTIVITY_TAG,"Adding car from pending ---");
                        showLoading("Adding car");

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
                        vinDecoderApi.execute(config.getString("MashapeAPIKey"));
                    }
                });
            }
        }
    }

    @Override
    protected void onPause() {
        if(BluetoothAdapter.getDefaultAdapter().isDiscovering()) {
            BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
        }
        mLogStore.stop();
        application.getMixpanelAPI().flush();
        if(vinDecoderApi!=null && vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
            vinDecoderApi.cancel(true);
            vinDecoderApi = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        if(vinDecoderApi!=null && vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
            vinDecoderApi.cancel(true);
            vinDecoderApi = null;
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = getIntent();
        if(intent!=null && intent.getBooleanExtra(MainActivity.HAS_CAR_IN_DASHBOARD,false)) {
            super.onBackPressed();
        } else {
            Toast.makeText(this,"There are no cars in your dashboard",Toast.LENGTH_SHORT).show();
        }
    }

    public void finish(boolean forceReset) {
        Log.i(ACTIVITY_TAG,"finishing activity");
        if(forceReset){
            VIN="";
            mileage="";
            scannerID="";
            dtcs="";

        }
        super.finish();
    }

    @Override
    public void finish() {
        VIN="";
        mileage="";
        scannerID="";
        dtcs="";
        super.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(AddCarActivity.this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        if(id == android.R.id.home) {
            Intent intent = getIntent();
            if(intent!=null && intent.getBooleanExtra(MainActivity.HAS_CAR_IN_DASHBOARD,false)){
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

        if (requestCode == RC_BARCODE_CAPTURE) {
            if (resultCode == CommonStatusCodes.SUCCESS) {
                if (data != null) {
                    Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                    VIN = barcode.displayValue;

                    String possibleEditedVin = checkVinForInvalidCharacter(VIN);
                    if(possibleEditedVin!=null) {
                        VIN = possibleEditedVin;
                    }

                    vinEditText.setText(VIN);
                    vinSection.setVisibility(View.VISIBLE);
                    Log.i(DTAG, "Barcode read: " + barcode.displayValue);

                    if (isValidVin(VIN)) { // show add car button iff vin is valid
                        abstractButton.setVisibility(View.VISIBLE);
                        scannerButton.setVisibility(View.GONE);
                        abstractButton.setEnabled(true);
                    } else {
                        abstractButton.setVisibility(View.GONE);
                        scannerButton.setVisibility(View.VISIBLE);
                        Toast.makeText(this,"Invalid VIN",Toast.LENGTH_SHORT).show();
                    }
                } else {
                    //statusMessage.setText(R.string.barcode_failure);
                    Log.i(DTAG, "No barcode captured, intent data is null");
                }
            }
        } else if(requestCode == SelectDealershipActivity.RC_DEALERSHIP &&
                resultCode == SelectDealershipActivity.RESULT_OK) {
            if(data != null) {
                String selectedShopId =
                        data.getStringExtra(SelectDealershipActivity.SELECTED_DEALERSHIP);
                setDealership(selectedShopId);
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Button clicked for getting VIN
     * @param view
     */
    private long startTime = 0;
    private boolean isSearching = false;
    private String DTAG = "ADD_CAR";

    public void searchForCar(View view) {
        Log.i(ACTIVITY_TAG,"Searching for car");
        String[] perms = {android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION};

        if(EasyPermissions.hasPermissions(AddCarActivity.this,perms)) {
            Log.i(ACTIVITY_TAG,"Has location permissions");

            if(!TextUtils.isEmpty(mileageEditText.getText().toString())) {
                Log.i(ACTIVITY_TAG, "Mileage is present");
                mileage = mileageEditText.getText().toString();
                if (isValidVin(vinEditText.getText().toString())) {
                    Log.i(ACTIVITY_TAG, "Vin is valid -- (searching for car)");
                    try {
                        application.getMixpanelAPI().track("Button Clicked",
                                new JSONObject("{'Button':'Add Car (Manual)','View':'AddCarActivity'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    Log.i(ACTIVITY_TAG,"Calling make car --search for car");
                    makeCar();
                } else {
                    if (BluetoothAdapter.getDefaultAdapter() == null) {
                        if(isLoading) {
                            hideLoading();
                        }
                        vinSection.setVisibility(View.VISIBLE);
                        Toast.makeText(this, "Your device does not support bluetooth",
                                Toast.LENGTH_SHORT).show();
                    } else {
                        if (service.getState() != BluetoothManage.CONNECTED) {

                            showLoading("Searching for Car");

                            Log.i(ACTIVITY_TAG, "Searching for car but device not connected");
                            service.startBluetoothSearch();

                            startTime = System.currentTimeMillis();
                            timerHandler.post(runnable);
                            isSearching = true;
                            isGettingVin = true;
                        } else {
                            try {
                                application.getMixpanelAPI().track("Button Clicked",
                                        new JSONObject("{'Button':'Add Car (BT)','View':'AddCarActivity'}"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            showLoading("Getting car vin");
                            Log.i(ACTIVITY_TAG, "Searching for car with device connected");
                            service.getCarVIN();
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Please enter Mileage", Toast.LENGTH_SHORT).show();
            }

        } else {
            EasyPermissions.requestPermissions(AddCarActivity.this,
                    getString(R.string.location_request_rationale), RC_LOCATION_PERM, perms);
        }
    }

    private void tryAgainDialog() {

        if(isLoading) {
            hideLoading();
        }

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
                isSearching= false;
                dialog.cancel();

            }
        });
        alertDialog.show();
    }
    /**
     *  Car search timer handler
     */

    private Handler timerHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what == 0) {
                tryAgainDialog();
            }
        }
    };

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - startTime;
            int seconds = (int) (timeDiff / 1000);
            //Log.i("AddCarString", "Timer Still Running");
            if(seconds > 120 && (isSearching) && service.getState()
                    != BluetoothManage.BLUETOOTH_CONNECT_SUCCESS) {
                timerHandler.sendEmptyMessage(0);
                timerHandler.removeCallbacks(runnable);
            } else if (!isSearching) {
                timerHandler.removeCallbacks(runnable);
            } else {
                timerHandler.post(runnable);
            }
        }
    };

    /**
     * Create a new Car
     */
    private void makeCar() {
        Log.i(ACTIVITY_TAG,"makeCar() -- function");

        if(isValidVin(vinEditText.getText().toString())&&!makingCar) {

            Log.i(ACTIVITY_TAG,"isValidVin(vinEditText.getText().toString())&&!makingCar");
            makingCar = true;
            Log.i(ACTIVITY_TAG,"Making car -- make car function");
            VIN = vinEditText.getText().toString();

            if(service.getState()==BluetoothManage.CONNECTED) {

                Log.i(ACTIVITY_TAG, "Now connected to device");
                showLoading("Loading Car Engine Code");
                askForDTC=true;
                Log.i(ACTIVITY_TAG,"Make car --- Getting DTCs");
                service.getDTCs();
                service.getPendingDTCs();
            } else {
                try {
                    Log.i(ACTIVITY_TAG, "Device not connected");
                    Log.i(ACTIVITY_TAG, "Checking internet connection");

                    showLoading("Checking internet connection");

                    if(new InternetChecker(this).execute().get()){

                        ParseConfig.getInBackground(new ConfigCallback() {
                            @Override
                            public void done(ParseConfig config, ParseException e) {

                                showLoading("Adding Car");
                                Log.i(ACTIVITY_TAG, "Adding car --- make car func");

                                if(vinDecoderApi == null) {
                                    vinDecoderApi = new CallMashapeAsync();
                                } else if(vinDecoderApi.getStatus().equals(AsyncTask.Status.PENDING)) {
                                    Log.i("VIN DECODER","Pending TASK");
                                } else if (vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
                                    vinDecoderApi.cancel(true);
                                    vinDecoderApi = null;
                                    vinDecoderApi = new CallMashapeAsync();
                                } else if (vinDecoderApi.getStatus().equals(AsyncTask.Status.FINISHED)) {
                                    vinDecoderApi = null;
                                    vinDecoderApi  = new CallMashapeAsync();
                                }
                                vinDecoderApi.execute(config.getString("MashapeAPIKey"));
                            }
                        });
                    } else {
                        hideLoading();
                        Intent intent = new Intent(AddCarActivity.this,PendingAddCarActivity.class);
                        startActivity(intent);
                        finish(false);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
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
        if(!checkBackCamera()) {
            Toast.makeText(this,"This device does not have a back facing camera",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        Log.i(ACTIVITY_TAG,"Starting barcode scanner");
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, true);
        intent.putExtra(BarcodeCaptureActivity.UseFlash, false); //If night use flash

        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(ACTIVITY_TAG,"getBluetoothState func--");
        if(state!=BluetoothManage.BLUETOOTH_CONNECT_SUCCESS) {
            Log.i(ACTIVITY_TAG, "Device is disconnected");
            /*hideLoading();
            service.startBluetoothSearch();*/
            /*if(isLoading) {
                hideLoading();
            }*/
            if(isLoading && !VIN.equals("")) {
                Log.i(ACTIVITY_TAG,"Vin is empty -- starting bluetooth search");
                service.startBluetoothSearch();
            }
            Log.i("GET BLUETOOTH STATE ","bluetooth not connected");
        }else{
            if(isGettingVin) {
                showLoading("Linking with Device, give it a few seconds");

                Log.i(ACTIVITY_TAG,"Getting car vin --- getBluetoothState");
                service.getCarVIN();
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
        Log.i(ACTIVITY_TAG,"Set parameter");
        if((responsePackageInfo.type+responsePackageInfo.value)
                .equals(BluetoothAutoConnectService.RTC_TAG)) {
            // Once device time is reset, the obd device disconnects from mobile device
            Log.i(ACTIVITY_TAG, "Set parameter() device time is set-- starting bluetooth search");
            service.startBluetoothSearch();
        }
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
        Log.i(ACTIVITY_TAG,"getParametData()");

        if(parameterPackageInfo.value.get(0).tlvTag.equals(BluetoothAutoConnectService.VIN_TAG)) {
            Log.i(ACTIVITY_TAG,"VIN response received");
            isSearching = false;
            isGettingVin = false;
            LogUtil.i("parameterPackage.size():"
                    + parameterPackageInfo.value.size());

            List<ParameterInfo> parameterValues = parameterPackageInfo.value;
            VIN = parameterValues.get(0).value;
            try {
                application.getMixpanelAPI().track("Scanned VIN",
                        new JSONObject("{'VIN':'"+VIN+"'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (isValidVin(VIN)) {

                Log.i(ACTIVITY_TAG,"VIN is valid");
                vinEditText.setText(VIN);
                showLoading("Loaded car VIN");

            } else {
                // same as in manual input plus vin hint
                Log.i(ACTIVITY_TAG, "Vin value returned not valid");
                Log.i(ACTIVITY_TAG,"VIN: "+VIN);
                hideLoading();
                showManualEntryUI();
                vinHint.setVisibility(View.VISIBLE);
            }
            scannerID = parameterPackageInfo.deviceId;
            Log.i(ACTIVITY_TAG,"Calling make car--- getParameter()");
            makeCar();
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        Log.i(ACTIVITY_TAG, "getIOData()");

        if (dataPackageInfo.result != 5&&dataPackageInfo.result!=4&&askForDTC) {
            showLoading("");
            Log.i(ACTIVITY_TAG,"Result: "+dataPackageInfo.result+ " Asking for dtcs --getIOData()");
            dtcs = "";
            if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                Log.i(ACTIVITY_TAG,"Parsing DTCs");
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for(String dtc : DTCs) {
                    dtcs+=service.parseDTCs(dtc)+",";
                }
            }
            try {
                Log.i(ACTIVITY_TAG, "getIOData --- Adding car");
                if(new InternetChecker(this).execute().get()){
                    ParseConfig.getInBackground(new ConfigCallback() {
                        @Override
                        public void done(ParseConfig config, ParseException e) {
                            showLoading("Adding Car");

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
                            vinDecoderApi.execute(config.getString("MashapeAPIKey"));
                        }
                    });
                }else{
                    Intent intent = new Intent(AddCarActivity.this,PendingAddCarActivity.class);
                    startActivity(intent);
                    finish(false);
                }
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            askForDTC=false;
        }else{
            //TODO why ?
            Log.i(ACTIVITY_TAG, "calling the strange get carVIN - --- Don't know why");
            //service.getCarVIN();
        }
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {
        if(loginPackageInfo.flag.equals(String.valueOf(BluetoothAutoConnectService.DEVICE_LOGOUT))) {
            Log.i(ACTIVITY_TAG, "Device connected");
            if(isLoading) {
                hideLoading();
            }
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
                    showBluetoothEntryUI();
                } else if (noButton.isChecked()) {
                    showManualEntryUI();
                }
            }
        }
    }

    private String checkVinForInvalidCharacter(String vin) {
        Log.i(ACTIVITY_TAG,"checkVinForInvalidCharacter--");
        if(vin!=null && vin.length() == 18 && vin.startsWith("I")) {
            return vin.substring(1,vin.length()-1);
        }
        return null;
    }

    boolean isValidVin(String vin) {
        Log.i(ACTIVITY_TAG,"isValidVin()-- func");
        return vin != null && vin.length() == 17;
    }
    /**
     * show Manual VIN UI
     */
    void showManualEntryUI() {
        Log.i(ACTIVITY_TAG, "showManual");
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

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
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
     * Complex call in adding car.
     * First it goes to Mashape and get info based on VIN -> determines if valid
     * Second it tries to add to database -> determines if already existing in Parse
     * Third it shows the Shops to choose from and add it to Parse.
     * Fourth uploads the DTCs to server (where server code will run to link to car)
     */
    private class CallMashapeAsync extends AsyncTask<String, Void,String>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.i(ACTIVITY_TAG, "on pre execute -- CallMashapeAsync");
            showLoading("Checking car scanner Id");
        }

        @Override
        protected void onPostExecute(String error) {

            Log.i(ACTIVITY_TAG, "On post execute");

            if(!error.equals("")) {
                if(isLoading) {
                    hideLoading();
                }
                makingCar = false;
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
                httpconn.addRequestProperty("X-Mashape-Key", msg[0]);
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
                    Log.i(ACTIVITY_TAG, "Call to mashape succeed");
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


    /**
     * Hide the loading screen
     */
    private void hideLoading(){
        Log.i(ACTIVITY_TAG, "hideLoading()--func");
        progressDialog.dismiss();
        isSearching = false ;
        isLoading = false;
    }

    /**
     * Show the loading screen
     */
    private void showLoading(String showText){
        Log.i(ACTIVITY_TAG, "show loading func----");
        isLoading = true;
        progressDialog.setMessage(showText);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void setDealership(String shopId) {
        shopSelected = shopId;
    }

    private String getDealership() {
        return shopSelected;
    }

    @AfterPermissionGranted(RC_LOCATION_PERM)
    private void beginSearchForCar() {
        abstractButton.performClick();
    }

    private void setupUIReferences() {
        Log.i(ACTIVITY_TAG,"Setting up ui...");
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

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (vinDecoderApi != null &&
                        vinDecoderApi.getStatus().equals(AsyncTask.Status.RUNNING)) {
                    vinDecoderApi.cancel(true);
                    vinDecoderApi = null;
                }

                isGettingVin = false;
                isSearching = false;
                timerHandler.removeCallbacks(runnable);
            }
        });
    }

    private void scannerIdCheck(final JSONObject carInfo) {
        Log.i(ACTIVITY_TAG,"ScannerIdCheck() -- func");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        query.whereEqualTo("scannerId",scannerID);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e!=null) {
                    if(isLoading) {
                        hideLoading();
                    }
                    Toast.makeText(AddCarActivity.this, e.getMessage(),Toast.LENGTH_SHORT).show();
                } else if(!objects.isEmpty() && !"".equals(scannerID) ) {
                    if(isLoading) {
                        hideLoading();
                    }
                    Toast.makeText(AddCarActivity.this, "The device with Id: "
                                    +scannerID+" is already linked with another car",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(ACTIVITY_TAG, "Calling vinCheck()--func");
                    vinCheck(carInfo);
                }
            }
        });
    }

    private void vinCheck(final JSONObject carInfo) {
        Log.i(ACTIVITY_TAG, "vinCheck()");
        //check if car already exists!
        showLoading("Checking car vin");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        query.whereEqualTo("VIN",VIN);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {
                if(e!=null) {
                    if(isLoading) {
                        hideLoading();
                    }
                    Toast.makeText(AddCarActivity.this, e.getMessage(),Toast.LENGTH_SHORT).show();
                } else if(!objects.isEmpty()) {
                    //see if car already exists!
                    if(isLoading) {
                        hideLoading();
                    }

                    Toast.makeText(AddCarActivity.this,"Car Already Exists!",
                            Toast.LENGTH_SHORT).show();

                    makingCar = false;
                    VIN="";
                    vinEditText.setText("");
                } else {
                    Log.i(ACTIVITY_TAG,"Calling save car to parse");
                    saveCarToServer(carInfo);
                }
            }
        });
    }

    private void saveCarToServer(JSONObject carInfo) {
        Log.i("shop selected:", getDealership());

        showLoading("Saving car details");

        try {
            //Make Car
            ParseObject newCar = new ParseObject("Car");
            newCar.put("VIN", VIN);
            newCar.put("year", carInfo.getInt("year"));
            newCar.put("model", carInfo.getString("model"));
            newCar.put("make", carInfo.getString("make"));
            newCar.put("tank_size", carInfo.getString("tank_size"));
            newCar.put("trim_level", carInfo.getString("trim_level"));
            newCar.put("engine", carInfo.getString("engine"));
            newCar.put("city_mileage", carInfo.getString("city_mileage"));
            newCar.put("highway_mileage", carInfo.getString("highway_mileage"));
            newCar.put("scannerId", scannerID == null ? "" : scannerID);
            newCar.put("owner", ParseUser.getCurrentUser().getObjectId());
            newCar.put("baseMileage", mileage.equals("") ? 0 : Integer.valueOf(mileage));
            newCar.put("dealership", shopSelected);
            newCar.put("currentCar",true);
            final Car addedCar = Car.createCar(newCar);
            newCar.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {

                    showLoading("Final touches");
                    if(e!=null){
                        hideLoading();
                        Toast.makeText(AddCarActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if(scannerID!=null) {
                        // upload the DTCs
                        ParseObject scansSave = new ParseObject("Scan");
                        scansSave.put("DTCs", dtcs);
                        scansSave.put("scannerId", scannerID);
                        scansSave.put("runAfterSave", true);
                        scansSave.saveEventually(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    //finished!
                                    returnToMainActivity(addedCar);
                                } else {
                                    hideLoading();
                                    makingCar = false;
                                    Toast.makeText(AddCarActivity.this, e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }else{
                        Log.i(ACTIVITY_TAG,"ScannerId is null -- MainActivity refresh");
                        returnToMainActivity(addedCar);
                    }
                }
            });
        } catch (JSONException e1) {
            e1.printStackTrace();
            makingCar = false;
        }
    }

    private void returnToMainActivity(Car addedCar) {
        Intent intent = getIntent();
        if(intent.getBooleanExtra(MainActivity.HAS_CAR_IN_DASHBOARD,false)) {
            //update the car object
            Car dashboardCar = (Car) intent.getSerializableExtra(MainActivity.CAR_EXTRA);
            ParseQuery<ParseObject> cars = ParseQuery.getQuery("Car");
            ParseObject car = null;
            try {
                car = cars.get(dashboardCar.getParseId());
                car.put("currentCar",false);
                car.saveEventually();
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, addedCar);
        setResult(ADD_CAR_SUCCESS, data);
        finish();
    }

}