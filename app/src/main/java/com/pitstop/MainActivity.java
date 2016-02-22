package com.pitstop;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.utils.InternetChecker;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.pitstop.PitstopPushBroadcastReceiver.ACTION_UPDATE_MILEAGE;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_ACTION;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_CAR_ID;

public class MainActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {
    public static Intent serviceIntent;


    public ArrayList<DBModel> array;

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    final static String pfShopName = "com.pitstop.shop.name";
    final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public static boolean refresh = false;
    public static boolean refreshLocal = false;

    public boolean isRefresh = true;

    private boolean isUpdatingMileage = false;
    private boolean isAddCar = false;
    private String carId;

    public BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
            service.setCallbacks(MainActivity.this); // register
            if (BluetoothAdapter.getDefaultAdapter()!=null&&BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                service.startBluetoothSearch(isAddCar);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        if(refresh){
            refresh = false;
            refreshDatabase();
        }
        if(refreshLocal){
            refreshLocal = false;
            setUp();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent= new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        setContentView(R.layout.activity_main);

        // check the intent action
        if (ACTION_UPDATE_MILEAGE.equals(getIntent().getStringExtra(EXTRA_ACTION))) {
            isUpdatingMileage = true;
            carId = getIntent().getStringExtra(EXTRA_CAR_ID);

            // clear the action once we've recorded it
            getIntent().putExtra(EXTRA_ACTION, (String)null);
            getIntent().putExtra(EXTRA_CAR_ID, (String)null);
        }


        array = new ArrayList<>();
        refreshDatabase();


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setBackgroundColor(getResources().getColor(R.color.highlight));
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
            Intent i = new Intent(MainActivity.this, SettingsActivity.class);
            ArrayList<String> ids = new ArrayList<>(),cars = new ArrayList<>(),origDealers = new ArrayList<>();
            for (DBModel car : array){
                cars.add(car.getValue("make") + " " + car.getValue("model"));
                ids.add(car.getValue("CarID"));
                origDealers.add(car.getValue("dealership"));
            }
            i.putStringArrayListExtra("cars", cars);
            i.putStringArrayListExtra("ids",ids);
            i.putStringArrayListExtra("dealerships",origDealers);
            startActivity(i);
            return true;
        }
        if(id==R.id.refresh&&!isRefresh){
            refreshDatabase();
        }
        if(id==R.id.add&&!isRefresh){
            addCar(null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode==AddCarActivity.RESULT_ADDED){
            refreshDatabase();
        }
    }

    @Override
    protected void onPause() {
        unbindService(serviceConnection);
        super.onPause();
    }

    /**
     * Clears and refreshes the whole database
     */
    private void refreshDatabase() {
        findViewById(R.id.loading_section).setVisibility(View.VISIBLE);

        // if wifi is on
        boolean hasWifi = false;
        try {
            hasWifi = new InternetChecker(this).execute().get();
            if (hasWifi) {
                isRefresh = true;
                final LocalDataRetriever ldr = new LocalDataRetriever(this);
                SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
                String objectID = settings.getString(pfCodeForObjectID, "NA");
                ldr.deleteData("Cars", "owner", objectID);
            }else{
                Snackbar snackbar = Snackbar.make(findViewById(R.id.fragment_main),"No internet connection to update.",Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        setUp();
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

    }

    /**
     * Add new Car
     * @param view
     */
    public void addCar(View view) {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        startActivity(intent);
    }

    /**
     * Reload screen withou clearing database, using network when it exists!
     */
    public void setUp(){
        findViewById(R.id.loading_section).setVisibility(View.VISIBLE);
        array.clear();
        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        SharedPreferences settings = getSharedPreferences(MainActivity.pfName, MODE_PRIVATE);
        String userId = settings.getString(MainActivity.pfCodeForObjectID, "NA");
        array = ldr.getDataSet("Cars", "owner", userId);

        // if wifi is on
        boolean hasWifi = false;
        try {
            hasWifi = new InternetChecker(this).execute().get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        //load from database if possible
        if(array.size()>0){
            openFragment();
        }else if(hasWifi){
            //search database
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
            if (ParseUser.getCurrentUser() != null) {
                userId = ParseUser.getCurrentUser().getObjectId();
            }
            query.whereContains("owner", userId);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(final List<ParseObject> objects, ParseException e) {
                    if(e==null){
                        for (final ParseObject car : objects) {
                            final Cars c = new Cars();
                            c.setValue("CarID", car.getObjectId());
                            c.setValue("owner", car.getString("owner"));
                            c.setValue("scannerId", car.getString("scannerId"));
                            c.setValue("VIN", car.getString("VIN"));
                            c.setValue("baseMileage", String.valueOf(car.getInt("baseMileage")));
                            c.setValue("totalMileage", String.valueOf(car.getInt("totalMileage")));
                            c.setValue("cityMileage", car.getString("city_mileage"));
                            c.setValue("highwayMileage", car.getString("highway_mileage"));
                            c.setValue("engine", car.getString("engine"));
                            c.setValue("dealership", car.getString("dealership"));
                            c.setValue("make", car.getString("make"));
                            c.setValue("model", car.getString("model"));
                            c.setValue("year", ""+car.getInt("year"));
                            c.setValue("tank_size", car.getString("tank_size"));
                            c.setValue("trimLevel", car.getString("trim_level"));
                            c.setValue("pendingEdmundServices",
                                    (car.get("pendingEdmundServices") == null ? "" : car.get("pendingEdmundServices").toString()));
                            c.setValue("pendingIntervalServices",
                                    (car.get("pendingIntervalServices") == null ? "" : car.get("pendingIntervalServices").toString()));
                            c.setValue("pendingFixedServices",
                                    (car.get("pendingFixedServices") == null ? "" : car.get("pendingFixedServices").toString()));
                            c.setValue("dtcs", (car.get("storedDTCs") == null ? "" : car.get("storedDTCs").toString()));
                            c.setValue("numberOfRecalls", String.valueOf(car.getInt("numberOfRecalls")));
                            c.setValue("numberOfServices",String.valueOf(car.getInt("numberOfServices")));

                            //custom call for recall entry
                            ParseQuery<ParseObject> recalls = ParseQuery.getQuery("RecallMasters");
                            recalls.whereEqualTo("forCar", car);
                            recalls.findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> recallsList, ParseException e) {
                                    if (e == null) {
                                        String recalls = "[";
                                        if (recallsList.size()>0) {
                                            JSONArray arrayOfRecalls = recallsList.get(0).getJSONArray("recalls");
                                            for (int i = 0; i < arrayOfRecalls.length(); i++) {
                                                try {
                                                    recalls += arrayOfRecalls.getJSONObject(i).getString("objectId") + ",";
                                                } catch (JSONException e1) {
                                                    e1.printStackTrace();
                                                }
                                            }
                                            if (recalls.length() != 1) {
                                                recalls = recalls.substring(0, recalls.length() - 1);
                                            }
                                        }
                                        recalls += "]";
                                        c.setValue("recalls", recalls);
                                        ldr.saveData("Cars", c.getValues());
                                        array.add(c);
                                        if(array.size()==objects.size()){
                                            openFragment();
                                        }
                                    }else{
                                        Log.d("Recalls", e.getMessage());
                                    }
                                }
                            });
                        }
                        if(objects.size()==0){
                            openFragment();
                        }
                    }
                }
            });
        }else{
            openFragment();
        }
        //if no bluetooth on, ask to turn it on
        if (BluetoothAdapter.getDefaultAdapter()!=null&&!BluetoothAdapter.getDefaultAdapter().isEnabled()) {

            Snackbar snackbar = Snackbar.make(findViewById(R.id.fragment_main),"Turn Bluetooth on to connect to car?",Snackbar.LENGTH_LONG);
            snackbar.setActionTextColor(getResources().getColor(R.color.highlight));
            snackbar.setAction("TURN ON", new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    service.startBluetoothSearch(isAddCar);

                }
            });
            snackbar.show();
        }
    }

    /**
     * Determine the fragment to open
     */
    private void openFragment() {
        findViewById(R.id.loading_section).setVisibility(View.GONE);
        if (array.size() == 0) {
            //if no car, go to add car screen
            Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
            isRefresh= false;
            startActivity(intent);
        } else {
            //choose between single and multi view

            final Fragment fragment;
            final String tag;

            if (array.size() == 1) {
                fragment = new MainActivityFragment();
                tag = "single_view";
            } else {
                fragment = new MainActivityMultiFragment();
                tag = "multi_view";
            }

            final Bundle args = new Bundle();
            if (isUpdatingMileage) {
                args.putString(EXTRA_ACTION, ACTION_UPDATE_MILEAGE);
                args.putString(EXTRA_CAR_ID, carId);

                // clear the data so that we don't update mileage again
                isUpdatingMileage = false;
                carId = null;
            }

            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragment.setArguments(args);
            fragmentTransaction.replace(R.id.fragment_main, fragment, tag);
            fragmentTransaction.commit();
            isRefresh = false;
        }
    }

    @Override
    public void getBluetoothState(int state) {

    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        //update the front end if data being received from a device!
        if(getSupportFragmentManager()!=null&&getSupportFragmentManager().getFragments()!=null&&getSupportFragmentManager().getFragments().size()>0) {
            if (array.size() == 1 && getSupportFragmentManager().findFragmentById(R.id.fragment_main) instanceof MainActivityFragment) {
                ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main)).indicateConnected(dataPackageInfo.deviceId);
            } else if (array.size() > 1&& getSupportFragmentManager().findFragmentById(R.id.fragment_main) instanceof MainActivityMultiFragment) {
                ((MainActivityMultiFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main)).indicateConnected(dataPackageInfo.deviceId);

            }
        }
    }
}
