package com.pitstop;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {
    public static Intent serviceIntent;


    public ArrayList<DBModel> array;

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    final static String pfShopName = "com.pitstop.shop.name";
    final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    public static boolean refresh = false;

    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
            service.setCallbacks(MainActivity.this); // register
            if (BluetoothAdapter.getDefaultAdapter()!=null&&BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                service.startBluetoothSearch();
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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        serviceIntent= new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        setContentView(R.layout.activity_main);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        array = new ArrayList<>();
        setUp();
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
            ArrayList<String> ids = new ArrayList<>(),cars = new ArrayList<>();
            for (DBModel car : array){
                cars.add(car.getValue("make") + " " + car.getValue("model"));
                ids.add(car.getValue("CarID"));
            }
            i.putStringArrayListExtra("cars", cars);
            i.putStringArrayListExtra("ids",ids);
            startActivity(i);
            return true;
        }
        if(id==R.id.refresh){
            refreshDatabase();
        }
        if(id==R.id.add){
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

    private void refreshDatabase() {
        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        String objectID = settings.getString(pfCodeForObjectID, "NA");
        ldr.deleteData("Cars", "owner", objectID);
        setUp();
//        if(array.size()>1){
//            ((MainActivityMultiFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main_multi)).setUp();
//        }else {
//            ((MainActivityFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_main)).setUp();
//        }
    }

    @Override
    public void onBackPressed() {
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(startMain);

    }

    public void addCar(View view) {
        Intent intent = new Intent(MainActivity.this, AddCarActivity.class);
        startActivity(intent);
    }

    public void setUp(){
        array.clear();
        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        SharedPreferences settings = getSharedPreferences(MainActivity.pfName, MODE_PRIVATE);
        String userId = settings.getString(MainActivity.pfCodeForObjectID, "NA");
        array = ldr.getDataSet("Cars", "owner", userId);
        if(array.size()>0){
            if(array.size()>1){
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                MainActivityMultiFragment fragment = new MainActivityMultiFragment();
                fragmentTransaction.replace(R.id.placeholder, fragment, "multi_view");
                fragmentTransaction.commit();
            }else {
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                MainActivityFragment fragment = new MainActivityFragment();
                fragmentTransaction.replace(R.id.placeholder, fragment, "single_view");
                fragmentTransaction.commit();
            }
        }else {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
            if (ParseUser.getCurrentUser() != null) {
                userId = ParseUser.getCurrentUser().getObjectId();
            }
            query.whereContains("owner", userId);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null){
                        for (final ParseObject car : objects) {
                            final Cars c = new Cars();
                            c.setValue("CarID", car.getObjectId());
                            c.setValue("owner", car.getString("owner"));
                            c.setValue("scannerId", car.getString("scannerId"));
                            c.setValue("VIN", car.getString("VIN"));
                            c.setValue("baseMileage", String.valueOf(car.getInt("baseMileage")));
                            c.setValue("cityMileage", car.getString("city_mileage"));
                            c.setValue("highwayMileage", car.getString("highway_mileage"));
                            c.setValue("engine", car.getString("engine"));
                            c.setValue("dealership", car.getString("dealership"));
                            c.setValue("make", car.getString("make"));
                            c.setValue("model", car.getString("model"));
                            c.setValue("year", ""+car.getInt("year"));
                            c.setValue("tank_size", car.getString("tank_size"));
                            c.setValue("totalMileage", car.getString("totalMileage"));
                            c.setValue("trimLevel", car.getString("trim_level"));
                            c.setValue("services", (car.get("servicesDue") == null ? "" : car.get("servicesDue").toString()));
                            c.setValue("dtcs", (car.get("storedDTCs") == null ? "" : car.get("storedDTCs").toString()));
                            ParseQuery<ParseObject> recalls = ParseQuery.getQuery("RecallMasters");
                            recalls.whereEqualTo("forCar", car);
                            recalls.findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> objects, ParseException e) {
                                    if (e == null) {
                                        String recalls = "[";
                                        if (objects.size()>0) {
                                            JSONArray arrayOfRecalls = objects.get(0).getJSONArray("recalls");
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
                                    }else{
                                        Log.d("Recalls", e.getMessage());
                                    }
                                    if(array.size()==1){
                                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                                        MainActivityFragment fragment = new MainActivityFragment();
                                        fragmentTransaction.replace(R.id.placeholder, fragment, "single_view");
                                        fragmentTransaction.commit();
                                    }else if (array.size()==0) {
                                        Intent intent = new Intent(MainActivity.this,AddCarActivity.class);
                                        startActivity(intent);
                                    }else{
                                        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                                        MainActivityMultiFragment fragment = new MainActivityMultiFragment();
                                        fragmentTransaction.replace(R.id.placeholder, fragment, "multi_view");
                                        fragmentTransaction.commit();
                                    }
                                }
                            });
                        }
                    }
                }
            });
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
        if(array.size()==1){
            ((MainActivityFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_main)).indicateConnected(dataPackageInfo.deviceId);
        }else if (array.size()>1){
            ((MainActivityMultiFragment)getSupportFragmentManager().findFragmentById(R.id.fragment_main)).indicateConnected(dataPackageInfo.deviceId);

        }
    }
}
