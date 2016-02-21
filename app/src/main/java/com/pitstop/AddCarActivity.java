package com.pitstop;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.log.LogCatHelper;
import com.castel.obd.util.LogUtil;
import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.parse.ConfigCallback;
import com.parse.FindCallback;
import com.parse.ParseConfig;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.Debug.PrintDebugThread;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.background.BluetoothAutoConnectService.BluetoothBinder;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class AddCarActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener, View.OnClickListener {
    public static int RESULT_ADDED = 10;
    private String VIN = "", scannerID = "", mileage = "", shopSelected = "", dtcs ="";
    private PrintDebugThread mLogDumper;
    private boolean bound;
    boolean makingCar = false;
    private BluetoothAutoConnectService service;
    private boolean askForDTC = false;
    private ArrayList<String> DTCData= new ArrayList<>();

    private ToggleButton yesButton;
    private ToggleButton noButton;
    /** is true when bluetooth has failed enough that we want to show the manual VIN entry UI */
    private boolean hasBluetoothVinEntryFailed = false;

    int counter =0;

    //debugging storing
    LogCatHelper mLogStore;

    ArrayList<String> shops = new ArrayList<String>();
    ArrayList<String> shopIds = new ArrayList<String>();

    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothBinder binder = (BluetoothBinder) service1;
            service = binder.getService();
            bound = true;
            service.setCallbacks(AddCarActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car);

        yesButton = (ToggleButton)findViewById(R.id.yes_i_do_button);
        noButton = (ToggleButton)findViewById(R.id.no_i_dont_button);
        yesButton.setOnClickListener(this);
        noButton.setOnClickListener(this);

        bindService(MainActivity.serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
//        mLogDumper = new PrintDebugThread(
//                String.valueOf(android.os.Process.myPid()),
//                ((TextView) findViewById(R.id.debug_log_print)),this);
//      mLogDumper.start();

        mLogStore = LogCatHelper.getInstance(this);

        //watch the number of characters in the textbox for VIN
        ((EditText) findViewById(R.id.VIN)).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                Editable vin = ((EditText)findViewById(R.id.VIN)).getText();
                findViewById(R.id.button).setEnabled(isValidVin(vin));
            }
        });

        // find shops
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> objects, ParseException e) {

                if (e != null) {
                    Toast.makeText(getApplicationContext(),"Failed to get dealership info", Toast.LENGTH_SHORT).show();
                }
                else {
                    for (ParseObject object : objects) {
                        Log.d("dealer name", object.getString("name"));
                        shops.add(object.getString("name"));
                        shopIds.add(object.getObjectId());
                    }
                }
            }
        });

        setUpTutorialScreen();

    }

    private void setUpTutorialScreen() {
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
        mLogStore.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLogStore.stop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
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

        return super.onOptionsItemSelected(item);
    }

    /**
     * Button clicked for getting VIN
     * @param view
     */
    public void getVIN(View view) {
        if(!((EditText) findViewById(R.id.mileage)).getText().toString().equals("")) {
            mileage = ((EditText) findViewById(R.id.mileage)).getText().toString();
            if (((EditText) findViewById(R.id.VIN)).getText().toString().length()==17){
                showLoading();
                makeCar();
            }
            if(BluetoothAdapter.getDefaultAdapter()==null){
                hideLoading();
                findViewById(R.id.VIN_SECTION).setVisibility(View.VISIBLE);
            }else {
                if (service.getState() != BluetoothManage.CONNECTED) {
                    showLoading();
                    service.startBluetoothSearch();
                } else {
                    service.getCarVIN();
                }
            }
            showLoading();
            ((TextView) findViewById(R.id.loading_details)).setText("Searching for Car");

        }else{
            Toast.makeText(this,"Please enter Mileage",Toast.LENGTH_SHORT).show();
        }
        //VIN = "YS3FD75Y746007819";
    }

    /**
     * Create a new Car
     */
    private void makeCar() {
        if(!((EditText) findViewById(R.id.VIN)).getText().toString().equals("")&&!makingCar) {
            makingCar = true;
            VIN = ((EditText) findViewById(R.id.VIN)).getText().toString();
            final String[] mashapeKey = {""};
            showLoading();
            if(service.getState()==BluetoothManage.CONNECTED){
                ((TextView) findViewById(R.id.loading_details)).setText("Loading Car Engine Code");
                askForDTC=true;
                service.getDTCs();
            }else {
                ParseConfig.getInBackground(new ConfigCallback() {
                    @Override
                    public void done(ParseConfig config, ParseException e) {
                        ((TextView) findViewById(R.id.loading_details)).setText("Checking VIN");
                        new CallMashapeAsync().execute(config.getString("MashapeAPIKey"));
                    }
                });
            }
        }
    }

    @Override
    public void getBluetoothState(int state) {
        if(state!=BluetoothManage.BLUETOOTH_CONNECT_SUCCESS){
            hideLoading();
            service.startBluetoothSearch();
        }else{
            ((TextView) findViewById(R.id.loading_details)).setText("Linking with Device, give it a few seconds");
            service.getCarVIN();
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
        hideLoading();
        LogUtil.i("parameterPackage.size():"
                + parameterPackageInfo.value.size());

        List<ParameterInfo> parameterValues = parameterPackageInfo.value;
        VIN = parameterValues.get(0).value;
        if (VIN != null && VIN.length() == 17) {
            ((EditText) findViewById(R.id.VIN)).setText(VIN);
            ((TextView) findViewById(R.id.loading_details)).setText("Loaded VIN");
        } else {
            findViewById(R.id.VIN_SECTION).setVisibility(View.VISIBLE);
            findViewById(R.id.VIN_hint).setVisibility(View.VISIBLE);
        }
        scannerID = parameterPackageInfo.deviceId;
        makeCar();
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        if (dataPackageInfo.result != 5&&dataPackageInfo.result!=4&&askForDTC) {
            showLoading();
            dtcs = "";
            if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for(String dtc : DTCs) {
                    dtcs+=service.parseDTCs(dtc)+",";
                }
            }
            ParseConfig.getInBackground(new ConfigCallback() {
                @Override
                public void done(ParseConfig config, ParseException e) {
                    ((TextView) findViewById(R.id.loading_details)).setText("Checking VIN");
                    new CallMashapeAsync().execute(config.getString("MashapeAPIKey"));
                }
            });
            askForDTC=false;
        }else{
            service.getCarVIN();
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

    boolean isValidVin(Editable vin) {
        return vin != null && vin.length() == 17;
    }

    /**
     * show Manual VIN UI
     */
    void showManualEntryUI() {
        findViewById(R.id.VIN_SECTION).setVisibility(View.VISIBLE);
        ((TextView)findViewById(R.id.textView6)).setText(getString(R.string.add_car_manual));
        ((Button) findViewById(R.id.button)).setText("ADD CAR");

        Editable vin = ((EditText)findViewById(R.id.VIN)).getText();
        findViewById(R.id.button).setEnabled(isValidVin(vin));
    }

    /**
     * show Bluetooth auto UI
     */
    void showBluetoothEntryUI() {
        findViewById(R.id.button).setEnabled(true);
        findViewById(R.id.VIN_SECTION).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.textView6)).setText(getString(R.string.add_car_bluetooth));
        ((Button) findViewById(R.id.button)).setText("SEARCH FOR CAR");
    }

    /**
     * Complex call in adding car.
     * First it goes to Mashape and get info based on VIN -> determines if valid
     * Second it tries to add to database -> determines if already existing in Parse
     * Third it shows the Shops to choose from and add it to Parse.
     * Fourth uploads the DTCs to server (where server code will run to link to car)
     */
    private class CallMashapeAsync extends AsyncTask<String, Void,Void>{
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoading();
        }

        protected Void doInBackground(String... msg) {

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
                    BufferedReader input = new BufferedReader(new InputStreamReader(httpconn.getInputStream()),8192);
                    String strLine = null;
                    while ((strLine = input.readLine()) != null)
                    {
                        response.append(strLine);
                    }
                    input.close();
                }
                if(new JSONObject(response.toString()).getBoolean("success")) {
                    //load mashape info
                    final JSONObject jsonObject = new JSONObject(response.toString()).getJSONObject("specification");

                    //check if car already exists!
                    ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
                    query.whereEqualTo("VIN",VIN);
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            ((TextView) findViewById(R.id.loading_details)).setText("Adding Car");
                            if(objects.size()>0){
                                //see if car already exists!
                                Toast.makeText(getApplicationContext(),"Car Already Exist for Another User!", Toast.LENGTH_SHORT).show();
                                hideLoading();
                                makingCar = false;
                                return;
                            }
                            else {
                                //choose the Shop to link with
                                new AlertDialog.Builder(AddCarActivity.this)
                                    .setSingleChoiceItems(shops.toArray(new CharSequence[shops.size()]), 0, null)
                                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            dialog.dismiss();
                                            int selectedPosition = ((AlertDialog)dialog).getListView().getCheckedItemPosition();
                                            // Do something useful withe the position of the selected radio button
                                            setDealership(shopIds.get(selectedPosition));
                                            Log.d("shop selected:", getDealership());

                                            try {
                                                //Make Car
                                                ParseObject newCar = new ParseObject("Car");
                                                newCar.put("VIN", VIN);
                                                newCar.put("year", jsonObject.getInt("year"));
                                                newCar.put("model", jsonObject.getString("model"));
                                                newCar.put("make", jsonObject.getString("make"));
                                                newCar.put("tank_size", jsonObject.getString("tank_size"));
                                                newCar.put("trim_level", jsonObject.getString("trim_level"));
                                                newCar.put("engine", jsonObject.getString("engine"));
                                                newCar.put("city_mileage", jsonObject.getString("city_mileage"));
                                                newCar.put("highway_mileage", jsonObject.getString("highway_mileage"));
                                                newCar.put("scannerId", scannerID == null ? "" : scannerID);
                                                newCar.put("owner", ParseUser.getCurrentUser().getObjectId());
                                                newCar.put("baseMileage", Integer.valueOf(mileage));
                                                newCar.put("dealership", shopSelected);
                                                newCar.saveEventually(new SaveCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        ((TextView) findViewById(R.id.loading_details)).setText("Final Touches");
                                                        if(e!=null){
                                                            hideLoading();
                                                            Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
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
                                                                        MainActivity.refresh = true;
                                                                        finish();
                                                                    } else {
                                                                        hideLoading();
                                                                        makingCar = false;
                                                                        Toast.makeText(getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
                                                                    }
                                                                }
                                                            });
                                                        }else{
                                                            MainActivity.refresh = true;
                                                            finish();
                                                        }
                                                    }
                                                });
                                            } catch (JSONException e1) {
                                                e1.printStackTrace();
                                                makingCar = false;
                                            }
                                        }
                                    })
                                    .show();
                            }
                        }
                    });
                }else{
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //show vin already exists
                            Toast.makeText(getApplicationContext(), "Failed to find by VIN, may be invalid", Toast.LENGTH_SHORT).show();
                            hideLoading();
                            makingCar = false;
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Errored Out", Toast.LENGTH_SHORT).show();
                        hideLoading();
                        makingCar = false;
                    }
                });
                e.printStackTrace();
            }
            return null;
        }
    }


    /**
     * Hide the loading screen
     */
    private void hideLoading(){
        findViewById(R.id.loading).setVisibility(View.GONE);
//        findViewById(R.id.VIN_SECTION).setVisibility(View.VISIBLE);
        findViewById(R.id.mileage).setEnabled(true);
        findViewById(R.id.VIN).setEnabled(true);
        findViewById(R.id.button).setEnabled(true);

    }

    public void hideLoading(View view){
        hideLoading();
    }

    /**
     * Show the loading screen
     */
    private void showLoading(){
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        findViewById(R.id.mileage).setEnabled(false);
        findViewById(R.id.VIN).setEnabled(false);
        findViewById(R.id.button).setEnabled(false);
    }

    private void setDealership(String shopId) {
        shopSelected = shopId;
    }

    private String getDealership() {
        return shopSelected;
    }

}
