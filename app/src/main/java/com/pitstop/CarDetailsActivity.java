package com.pitstop;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.github.brnunes.swipeablerecyclerview.SwipeableRecyclerViewTouchListener;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.ConnectedCarRecyclerAdapter;
import com.pitstop.utils.PIDParser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.pitstop.PitstopPushBroadcastReceiver.ACTION_UPDATE_MILEAGE;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_ACTION;

/**
 *  Not being used right now
 */
@Deprecated
public class CarDetailsActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    private GlobalApplication application;

    public static final String TAG = CarDetailsActivity.class.getSimpleName();
    private CustomAdapter customAdapter;
    private ConnectedCarRecyclerAdapter connectedAdapter;
    private ArrayList<DBModel> arrayList = new ArrayList<>();
    private HashMap<String,Object> output = new HashMap<String, Object>();

    private boolean requestSent = false;

    private String carId, VIN, scannerID,make, model,year,baseMileage, totalMileage, shopId;

    public static Intent serviceIntent;

    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
            service.setCallbacks(CarDetailsActivity.this); // register

            if (BluetoothAdapter.getDefaultAdapter()!=null&&BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                service.startBluetoothSearch();
            }
            //connectedCarStatusUpdate();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState)  {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);
        setTitle(getIntent().getExtras().getString("title").toUpperCase());

        application = (GlobalApplication) getApplicationContext();

        //------------------------------- setup constants
        carId = getIntent().getStringExtra("CarID");
        VIN = getIntent().getStringExtra("vin");
        scannerID = getIntent().getStringExtra("scannerId");
        make = getIntent().getStringExtra("make");
        model = getIntent().getStringExtra("model");
        year = getIntent().getStringExtra("year");
        shopId = getIntent().getStringExtra("shopId");
        baseMileage = getIntent().getStringExtra("baseMileage");
        totalMileage = getIntent().getStringExtra("totalMileage");
        serviceIntent= new Intent(CarDetailsActivity.this, BluetoothAutoConnectService.class);

        Log.d("total mileage", totalMileage);
        Log.d("base mileage", baseMileage);

        //------------------------------- set up mileage stuff
        if (totalMileage == null) { // total mileage was not calculated in backend yet
            ((TextView)findViewById(R.id.mileage)).setText(baseMileage);
        }
        else {
            ((TextView)findViewById(R.id.mileage)).setText(totalMileage);
        }


        //------------------------------- set up mileage stuff
        findViewById(R.id.update_mileage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(CarDetailsActivity.this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                input.setRawInputType(Configuration.KEYBOARD_12KEY);
                input.setTextColor(getResources().getColor(R.color.highlight));
                new AlertDialog.Builder(CarDetailsActivity.this)
                        .setTitle("Update Mileage")
                        .setView(input)
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                updateMileage(input.getText().toString());
                                dialogInterface.dismiss();
                            }
                        }).show();
            }
        });

        if (ACTION_UPDATE_MILEAGE.equals(getIntent().getStringExtra(EXTRA_ACTION))) {
            // clear the action so it's not repeated
            getIntent().putExtra(EXTRA_ACTION, (String)null);

            // update the mileage
            findViewById(R.id.update_mileage).performClick();
        }

        // Todo enable when PIDParser is fixed
       /* LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.unconnected_car_display,((LinearLayout)findViewById(R.id.carStatus)), false);

        ((LinearLayout)findViewById(R.id.carStatus)).addView(view);*/


        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        //--------------------------GET SERVICES + RECALLS + DTCS--------------------------

        Bundle extras = getIntent().getExtras();
        getEdmundsServices(extras, ldr);
        getIntervalServices(extras, ldr);
        getFixedServices(extras, ldr);
        getRecalls(extras, ldr);
        getDTCs(extras, ldr);

        //------------------------------- set up listview
        RecyclerView mRecyclerView = (RecyclerView) findViewById(R.id.car_event_listview);
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        customAdapter = new CustomAdapter(this,arrayList);
        mRecyclerView.setAdapter(customAdapter);
        setSwipeDeleteListener(mRecyclerView);
        //setup car connected
        findViewById(R.id.evcheck_layout).setBackgroundColor(getResources().getColor(R.color.evcheck));
        ((TextView) findViewById(R.id.evcheck)).setTextColor(Color.WHITE);
        findViewById(R.id.evcheck_layout).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(CarDetailsActivity.this, EnvironmentalCheckActivity.class);
                startActivity(intent);
            }
        });

        try {
            application.getMixpanelAPI().track("View Appeared", new JSONObject("{'View':'CarDetailActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /*private void connectedCarStatusUpdate() {
        if(service==null) {
            Log.i("SERVICE", "service is null");
        }


        if(service!=null && service.getCurrentCar()!=null) {
            Cars connectedCar = service.getCurrentCar();
            if(connectedCar.getValue("VIN").equals(VIN)) {
                //((LinearLayout)findViewById(R.id.carStatus)).removeAllViewsInLayout();
                LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.connected_car_display,
                        ((LinearLayout)findViewById(R.id.carStatus)), false);
                ((TextView)view.findViewById(R.id.make)).setText(make);
                ((TextView)view.findViewById(R.id.model)).setText(model);
                ((TextView)view.findViewById(R.id.year)).setText(year);
                ((LinearLayout)findViewById(R.id.carStatus)).addView(view);
            }
        } else {
            LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.unconnected_car_display,
                    ((LinearLayout)findViewById(R.id.carStatus)), false);

            ((LinearLayout)findViewById(R.id.carStatus)).addView(view);
        }
    }*/

    @Override
    protected void onResume() {
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
        //connectedCarStatusUpdate();
        super.onResume();
    }

    /**
     * Recieves all the Recalls
     * @param extras
     * @param ldr
     */
    private void getRecalls(Bundle extras, final LocalDataRetriever ldr) {
        final ArrayList<String> recalls = new ArrayList<String>(Arrays.asList(extras.getStringArray("pendingRecalls")));
        ArrayList<String> removes = new ArrayList<>();
        if(recalls.size()!=0) {
            //check DB first
            for (String i : recalls) {
                Recalls recall;
                HashMap<String,String> values = new HashMap<>();
                values.put("RecallID", i.trim());
                recalls.set(recalls.indexOf(i), i.trim());
                recall = (Recalls) ldr.getData("Recalls", values);
                if (recall != null) {
                    if(recall.getValue("state").equals("new")||recall.getValue("state").equals("pending")) {
                        arrayList.add(recall);
                    }
                    removes.add(i.trim());
                }
            }
            recalls.removeAll(removes);
            //if need to get some services
            if (recalls.size() > 0) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("RecallEntry");
                query.whereContainedIn("objectId",recalls);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if (e == null) {
                            for (ParseObject parseObject : objects) {
                                Recalls recall = new Recalls();

                                recall.setValue("RecallID", parseObject.getObjectId());
                                recall.setValue("name", parseObject.getString("name"));
                                recall.setValue("description", parseObject.getString("description"));
                                recall.setValue("remedy", parseObject.getString("remedy"));
                                recall.setValue("risk", "" + parseObject.getNumber("risk"));
                                recall.setValue("effectiveDate", parseObject.getString("effectiveDate"));
                                recall.setValue("oemID", parseObject.getString("oemID"));
                                recall.setValue("reimbursement", "" + parseObject.getNumber("reimbursement"));
                                recall.setValue("state", parseObject.getString("state"));
                                recall.setValue("riskRank", "" + parseObject.getNumber("riskRank"));
                                if (!recalls.contains(recall.getValue("RecallID"))) {
                                    ldr.saveData("Recalls", recall.getValues());
                                    if (parseObject.getString("state").equals("new") || parseObject.getString("state").equals("pending"))
                                        arrayList.add(recall);
                                }
                            }
                        }
                        updateAdapter();
                    }
                });
            }
        }
    }

    /**
     * Gets all the DTCs
     * @param extras
     * @param ldr
     */
    private void getDTCs(Bundle extras, final LocalDataRetriever ldr) {
        ArrayList<String> dtcs = new ArrayList<String>(Arrays.asList(extras.getStringArray("dtcs")));
        ArrayList<String> removes = new ArrayList<>();
        final HashMap<String,Boolean> dtcList = new HashMap<String,Boolean>();
        if(dtcs.size()!=0) {
            //check DB first
            for (String i : dtcs) {
                DTCs dtc;
                HashMap<String,String> values = new HashMap<>();
                values.put("dtcCode", i.trim());
                dtcs.set(dtcs.indexOf(i), i.trim());
                dtc = (DTCs) ldr.getData("DTCs", values);
                if (dtc != null) {
                    arrayList.add(dtc);
                    removes.add(i.trim());
                }
            }
            dtcs.removeAll(removes);
            //if need to get some services
            if (dtcs.size() > 0) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("DTC");
                query.whereContainedIn("dtcCode", dtcs);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null) {
                            for (ParseObject parseObject : objects) {
                                DTCs service = new DTCs();
                                service.setValue("dtcCode", parseObject.getString("dtcCode"));
                                service.setValue("description", parseObject.getString("description"));

                                ldr.saveData("DTCs", service.getValues());
                                arrayList.add(service);
                            }
                        }
                        updateAdapter();
                    }
                });
            }
        }
    }

    /**
     * Get Fixed Services
     * @param extras
     * @param ldr
     */
    private void getFixedServices(Bundle extras, final LocalDataRetriever ldr) {
        //-------Interval----------
        ArrayList<String> fixed = new ArrayList<String>(Arrays.asList(extras.getStringArray("fixed")));
        ArrayList<String> removes = new ArrayList<>();
        if(fixed.size()!=0) {
            //check DB first
            for (String i : fixed) {
                Services service;
                HashMap<String,String> values = new HashMap<>();
                values.put("ParseID",i.trim());
                fixed.set(fixed.indexOf(i),i.trim());
                values.put("serviceType","fixed");
                service = (Services) ldr.getData("Services", values);
                if (service != null) {
                    arrayList.add(service);
                    removes.add(i.trim());
                }
            }
            fixed.removeAll(removes);
            //if need to get some services
            if (fixed.size() > 0) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("ServiceFixed");
                query.whereContainedIn("objectId", fixed);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null) {
                            for (ParseObject parseObject : objects) {
                                Services service = new Services();
                                service.setValue("item", parseObject.getString("item"));
                                service.setValue("serviceType", "fixed");
                                service.setValue("itemDescription", parseObject.getString("itemDescription"));
                                service.setValue("action", parseObject.getString("action"));
                                service.setValue("ParseID", parseObject.getObjectId());
                                //end key, now custom
                                service.setValue("dealership", parseObject.getString("dealership"));
                                service.setValue("priority", "" + parseObject.getNumber("priority"));
                                service.setValue("intervalFixed", "" + parseObject.getNumber("mileage"));

                                ldr.saveData("Services", service.getValues());
                                arrayList.add(service);
                            }
                        }
                        updateAdapter();
                    }
                });
            }
        }
    }

    /**
     * Get Interval Services
     * @param extras
     * @param ldr
     */
    private void getIntervalServices(Bundle extras,final LocalDataRetriever ldr) {
        //-------Interval----------
        ArrayList<String> interval = new ArrayList<String>(Arrays.asList(extras.getStringArray("interval")));
        ArrayList<String> removes = new ArrayList<>();
        if(interval.size()!=0) {
            //check DB first
            for (String i : interval) {
                Services service;
                HashMap<String,String> values = new HashMap<>();
                values.put("ParseID", i.trim());
                interval.set(interval.indexOf(i), i.trim());
                values.put("serviceType","interval");
                service = (Services) ldr.getData("Services", values);
                if (service != null) {
                    arrayList.add(service);
                    removes.add(i.trim());
                }
            }
            interval.removeAll(removes);
            //if need to get some services
            if (interval.size()>0) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("ServiceInterval");
                query.whereContainedIn("objectId", interval);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null) {
                            for (ParseObject parseObject : objects) {
                                Services service = new Services();
                                service.setValue("item", parseObject.getString("item"));
                                service.setValue("serviceType", "interval");
                                service.setValue("itemDescription", parseObject.getString("itemDescription"));
                                service.setValue("action", parseObject.getString("action"));
                                service.setValue("ParseID", parseObject.getObjectId());
                                //end key, now custom
                                service.setValue("dealership", parseObject.getString("dealership"));
                                service.setValue("priority", "" + parseObject.getNumber("priority"));
                                service.setValue("intervalMileage", "" + parseObject.getNumber("mileage"));

                                ldr.saveData("Services", service.getValues());
                                arrayList.add(service);
                            }
                        }
                        updateAdapter();
                    }
                });
            }
        }
    }

    /**
     * Get Edmund Services
     * @param extras
     * @param ldr
     */
    private void getEdmundsServices(Bundle extras, final LocalDataRetriever ldr) {
        //-------EDMUND----------
        ArrayList<String> edmunds = new ArrayList<String>(Arrays.asList(extras.getStringArray("edmund")));
        ArrayList<String> removes = new ArrayList<>();
        if(edmunds.size()!=0) {
            //check DB first
            for (String i : edmunds) {
                Services service;
                HashMap<String,String> values = new HashMap<>();
                edmunds.set(edmunds.indexOf(i),i.trim());
                values.put("ParseID",i.trim());
                values.put("serviceType","edmunds");
                service = (Services) ldr.getData("Services", values);
                if (service != null) {
                    arrayList.add(service);
                    removes.add(i.trim());
                }
            }
            edmunds.removeAll(removes);
            //if need to get some services
            if (edmunds.size()>0) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("EdmundsService");
                query.whereContainedIn("objectId", edmunds);
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> objects, ParseException e) {
                        if(e==null) {
                            for (ParseObject parseObject : objects) {
                                Services service = new Services();
                                service.setValue("item", parseObject.getString("item"));
                                service.setValue("serviceType", "edmunds");
                                service.setValue("itemDescription", parseObject.getString("itemDescription"));
                                service.setValue("action", parseObject.getString("action"));
                                service.setValue("priority", "" + parseObject.getNumber("priority"));
                                service.setValue("ParseID", parseObject.getObjectId());
                                //end key, now custom
                                service.setValue("intervalMileage", "" + parseObject.getNumber("intervalMileage"));
                                service.setValue("intervalMonth", "" + parseObject.getNumber("intervalMonth"));

                                ldr.saveData("Services", service.getValues());
                                arrayList.add(service);
                            }
                        }
                        updateAdapter();
                    }
                });
            }
        }
    }


    public void updateAdapter(){
        customAdapter.dataList.clear();
        customAdapter.dataList.addAll(arrayList);
        if(arrayList.size()==0){
            Cars car = new Cars();
            car.setValue("itemDescription", "You have no pending Engine Code, Recalls or Services");
            car.setValue("item","Congrats!");
            customAdapter.dataList.add(car);
        }
        customAdapter.notifyDataSetChanged();
    }


    /**
     * Detect Swipes on each list item
     * @param mRecyclerView
     */
    private void setSwipeDeleteListener(RecyclerView mRecyclerView){
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(mRecyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                //------------------------------- DTCS are disabled still TODO add DTCS Response
                                if(customAdapter.dataList.get(position) instanceof DTCs || customAdapter.dataList.get(position) instanceof Cars){
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, final int[] reverseSortedPositions) {

                                /*try {
                                    GlobalApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Swiped Away Service/Recall','View':'CarDetailActivity'}"));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }*/
                                final CharSequence[] times = new CharSequence[]{
                                        "Recently", "2 Weeks Ago", "A Month Ago", "2 to 3 Months Ago", "3 to 6 Months Ago", "6 to 12 Months Ago"
                                };
                                final int[] estimate = new int[]{
                                        0,2,3,10,18,32
                                };
                                Calendar cal = Calendar.getInstance();
                                final Date currentLocalTime = cal.getTime();
                                final DateFormat date = new SimpleDateFormat("yyy-MM-dd HH:mm:ss z");
                                final int i = reverseSortedPositions[0];
                                AlertDialog setDoneDialog = new AlertDialog.Builder(CarDetailsActivity.this)
                                    .setItems(times, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, final int position) {
                                            int mileage = Integer.valueOf(((TextView) findViewById(R.id.mileage)).getText().toString());

                                            //------------------------------- services
                                            if(customAdapter.dataList.get(i) instanceof Services) {
                                                final String type = customAdapter.dataList.get(i).getValue("serviceType");
                                                final int typeService = (type.equals("edmunds") ? 0 : (type.equals("fixed") ? 1 : 2));
                                                //update service info only in Servie History first
                                                ParseObject saveCompletion = new ParseObject("ServiceHistory");
                                                saveCompletion.put("carId", carId);
                                                saveCompletion.put("mileageSetByUser", mileage);
                                                saveCompletion.put("mileage", mileage - estimate[position] * 375);
                                                saveCompletion.put("serviceObjectId", customAdapter.dataList.get(i).getValue("ParseID").toString());
                                                saveCompletion.put("shopId", shopId);
                                                saveCompletion.put("userMarkedDoneOn", times[position] + " from " + date.format(currentLocalTime));
                                                saveCompletion.put("type", Integer.valueOf(typeService));
                                                saveCompletion.saveEventually(new SaveCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        Toast.makeText(CarDetailsActivity.this,
                                                                "Updated Service History",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                //update the car object on the server next
                                                ParseQuery updateObject = new ParseQuery("Car");
                                                try {
                                                    updateObject.get(carId);
                                                    updateObject.findInBackground(new FindCallback<ParseObject>() {

                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            if(e==null) {
                                                                String type = (typeService == 0 ? "pendingEdmundServices" : (typeService == 1 ? "pendingFixedServices" : "pendingIntervalServices"));
                                                                JSONArray original = objects.get(0).getJSONArray(type);
                                                                ArrayList<String> updatedTexts = new ArrayList<>();
                                                                for (int j = 0; j < original.length(); j++) {
                                                                    try {
                                                                        if (!original.getString(j).equals(customAdapter.dataList.get(i).getValue("ParseID"))) {
                                                                            updatedTexts.add(original.getString(j));
                                                                        }
                                                                    } catch (JSONException e1) {
                                                                        e1.printStackTrace();
                                                                    }
                                                                }
                                                                //update object on server for Car
                                                                objects.get(0).put(type, updatedTexts);
                                                                objects.get(0).saveEventually();
                                                                // update local database!
                                                                LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
                                                                HashMap<String, String> map = new HashMap<String, String>();
                                                                map.put(type, "[" + TextUtils.join(",", updatedTexts) + "]");
                                                                ldr.updateData("Cars", "CarID", carId, map);

                                                                for (int position : reverseSortedPositions) {
                                                                    customAdapter.dataList.remove(position);
                                                                    customAdapter.notifyItemRemoved(position);
                                                                }
                                                                customAdapter.notifyDataSetChanged();
                                                            }
                                                        }
                                                    });
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                }
                                            }else{
                                                //------------------------------- Recalls
                                                //update service history
                                                ParseObject saveCompletion = new ParseObject("ServiceHistory");
                                                saveCompletion.put("carId", carId.toString());
                                                saveCompletion.put("mileageSetByUser", mileage);
                                                saveCompletion.put("mileage", mileage - estimate[position] * 375);
                                                saveCompletion.put("serviceObjectId", customAdapter.dataList.get(i).getValue("RecallID"));
                                                saveCompletion.put("shopId", shopId);
                                                saveCompletion.put("userMarkedDoneOn", times[position] + " from " + date.format(currentLocalTime));
                                                saveCompletion.saveEventually(new SaveCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        Toast.makeText(CarDetailsActivity.this,
                                                                "Updated Service History",
                                                                Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                //update recall object
                                                ParseQuery updateObject = new ParseQuery("RecallEntry");
                                                try {
                                                    updateObject.get(customAdapter.dataList.get(i).getValue("RecallID"));
                                                    updateObject.findInBackground(new FindCallback<ParseObject>() {

                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            objects.get(0).put("state","doneByUser");
                                                            ((ParseObject)objects.get(0)).saveEventually();
                                                            //update local database
                                                            LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
                                                            HashMap<String,String> map = new HashMap<String,String>();
                                                            ArrayList<Recalls> tempRecallList = new ArrayList<>();
                                                            for (DBModel a : arrayList){
                                                                if(a instanceof Recalls && !a.getValue("RecallID").equals(customAdapter.dataList.get(i).getValue("RecallID"))){
                                                                    tempRecallList.add((Recalls)a);
                                                                }
                                                            }
                                                            map.put("recalls", "[" + TextUtils.join(",", tempRecallList) + "]");
                                                            ldr.updateData("Cars", "CarID", carId, map);

                                                            for (int position : reverseSortedPositions) {
                                                                customAdapter.dataList.remove(position);
                                                                customAdapter.notifyItemRemoved(position);
                                                            }
                                                            customAdapter.notifyDataSetChanged();
                                                        }
                                                    });
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                            dialogInterface.dismiss();
                                        }
                                    })
                                    .setTitle("When did you complete this task?")
                                    .show();
                            }

                            @Override
                            public void onDismissedBySwipeRight(RecyclerView recyclerView, int[] reverseSortedPositions) {
                                onDismissedBySwipeLeft(recyclerView,reverseSortedPositions);
                            }
                        });

        mRecyclerView.addOnItemTouchListener(swipeTouchListener);
    }
    /**
     * Update the mileage
     * @param chsq
     */
    private void updateMileage(CharSequence chsq) {

        /*try {
            GlobalApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Update Mileage','View':'CarDetailActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        String mileage = chsq.toString();

        // save to parse
        try {
            final HashMap<String, Object> params = new HashMap<String, Object>();

            params.put("carVin", VIN);
            params.put("mileage", Integer.valueOf(mileage));
            // update the server information
            ParseCloud.callFunctionInBackground("carServicesUpdate", params, new FunctionCallback<Object>() {
                public void done(Object o, ParseException e) {
                    if (e == null) {
                        Toast.makeText(CarDetailsActivity.this,
                                "mileage updated", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(CarDetailsActivity.this,
                                "failed to update mileage", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            //update the car object
            ParseQuery<ParseObject> cars = ParseQuery.getQuery("Car");
            ParseObject car = cars.get(carId);
            car.put("totalMileage", Integer.parseInt(mileage));
            car.saveEventually();

        } catch (ParseException e) {
            Log.e("CarDetailsActivity", "parse exception: ", e);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid mileage", Toast.LENGTH_SHORT).show();
        }

        // save to local DB
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        // carId shouldn't be shown to user
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("totalMileage", mileage);
        ldr.updateData("Cars", "CarID", carId, hm);

        // update the textview
        ((TextView)findViewById(R.id.mileage)).setText(mileage);

    }

    /**
     * Request Service Button Response
     * @param additional
     */
    public void requestServiceButton(String additional) {

        /*try {
            GlobalApplication.mixpanelAPI.track("Button Clicked",
                    new JSONObject("{'Button':'Request Service','View':'CarDetailsActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }*/
        if(requestSent){
            Toast.makeText(CarDetailsActivity.this,
                    "Already Sent Request for Car!", Toast.LENGTH_SHORT).show();
            return;
        }
        requestSent = true;
        String userId = ParseUser.getCurrentUser().getObjectId();
        // Intent intent = getIntent();
        // String vin = Intent.getStringExtra("carVin")
        ArrayList<HashMap<String,String>> services = new ArrayList<>();
        ArrayList<String> recalls = new ArrayList<>();
        for(DBModel model: arrayList){
            //services dont need to be reformatted
            if(model instanceof Services){
                services.add(model.getValues());
            }
            //change recall structure to work
            if(model instanceof Recalls){
                LocalDataRetriever ldr  = new LocalDataRetriever(this);
                Recalls recall = new Recalls();
                recall.setValue("item", model.getValue("name"));
                recall.setValue("action","Recall For");
                recall.setValue("itemDescription",model.getValue("description"));
                recall.setValue("priority",""+ 6); // high priority for recall
                services.add(recall.getValues());
                recalls.add(model.getValue("RecallID"));
                //update db
                model.setValue("state","pending");
                HashMap<String,String> tmp = new HashMap<>();
                tmp.put("state","pending");
                ldr.updateData("Recalls","RecallID",model.getValue("RecallID"),tmp);
            }
            //change dtc object to work
            if(model instanceof DTCs){
                DTCs dtc = new DTCs();
                dtc.setValue("item", model.getValue("dtcCode"));
                dtc.setValue("action","Engine Issue: DTC Code");
                dtc.setValue("itemDescription",model.getValue("description"));
                dtc.setValue("priority",""+ 5); // must be 5
                services.add(dtc.getValues());
            }
        }
        //update the recall state to pending!
        if(recalls.size()>0) {
            ParseQuery query = new ParseQuery("RecallEntry");
            query.whereContainedIn("objectId", recalls);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null) {
                        for (ParseObject obj : objects) {
                            obj.put("state", "pending");
                            obj.saveEventually();
                        }
                    }
                }
            });
        }
        //update the object to be send to server with "additional" messages
        output.put("services", services);
        output.put("carVin", VIN);
        output.put("userObjectId", userId);
        output.put("comments",additional);
        if(services.size()>0) {
            //send email
            ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output, new FunctionCallback<Object>() {
                @Override
                public void done(Object object, ParseException e) {
                    if (e == null) {
                        Toast.makeText(CarDetailsActivity.this,
                                "Sent Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(CarDetailsActivity.this,
                                e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            //if there are no entries, say nothing to send!
            Toast.makeText(CarDetailsActivity.this,
                    "Nothing to Send", Toast.LENGTH_SHORT).show();
        }
    }

    private void setUpConnectedCarDisplay(List<PIDInfo> pids){
        ArrayList<PIDParser.Pair<String,String>> carDetails = new ArrayList<>();
        for (PIDInfo pid :pids){
            PIDParser.Pair<String,String> tmp = PIDParser.ParsePID(pid.pidType,pid.intValues,pid.value);
            if(tmp.value!=null){
                carDetails.add(new PIDParser.Pair<String,String>(pid.pidType,tmp.value));
            }
        }
        if(carDetails.size()>0) {
            if (connectedAdapter == null) {
            } else {
                connectedAdapter.dataList.clear();
                connectedAdapter.dataList.addAll(carDetails);
                connectedAdapter.notifyDataSetChanged();
            }
        }

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_car_details, menu);
        return true;
    }

    @Override
    protected void onPause() {
        // unbind service to prevent memory leaks
        unbindService(serviceConnection);
        //GlobalApplication.mixpanelAPI.flush();
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            ServiceDialog dialog = new ServiceDialog();
            dialog.show(getSupportFragmentManager(),"sendSupportEmail");
        }
        if (id == R.id.history) {
            /*try {
                GlobalApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Open History View','View':'CarDetailActivity'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
            Intent intent = new Intent(CarDetailsActivity.this, CarHistoryActivity.class);
            intent.putExtra("carId",carId);
            startActivity(intent);
        }

        if (id ==  android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void getBluetoothState(int state) {
        //connectedCarStatusUpdate();
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
        /*if(dataPackageInfo.deviceId.contains(scannerID)){
            if(connectedAdapter==null) {
                ((LinearLayout)findViewById(R.id.carStatus)).removeAllViewsInLayout();
                LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                        (Context.LAYOUT_INFLATER_SERVICE);
                View view = inflater.inflate(R.layout.fragment_connected_display,((LinearLayout)findViewById(R.id.carStatus)), false);
                ((TextView) view.findViewById(R.id.mileage)).setText(totalMileage);

                RecyclerView rv = (RecyclerView) view.findViewById(R.id.connected_recycler);
                connectedAdapter = new ConnectedCarRecyclerAdapter(this);
                // use a linear layout manager
                LinearLayoutManager a = new LinearLayoutManager(this);
                a.setOrientation(LinearLayoutManager.VERTICAL);
                rv.setLayoutManager(a);
                rv.setAdapter(connectedAdapter);
                ((LinearLayout) findViewById(R.id.carStatus)).addView(view);
            }
            if(dataPackageInfo.obdData.size()>0)
                setUpConnectedCarDisplay(dataPackageInfo.obdData);
        }*/
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    /**
     * The dialog to display for clicking service request button
     */
    public static class ServiceDialog extends DialogFragment {
        /*@Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(CarDetailsActivity.this);
            // Get the layout inflater
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            final View view = inflater.inflate(R.layout.dialog_request_service, null);
            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton("Send", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            requestServiceButton(((EditText) view.findViewById(R.id.additional_comments)).getText().toString());
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            ServiceDialog.this.getDialog().cancel();
                        }
                    });
            return builder.create();
        }*/
    }

    /**
     * Adapter for the list
     */
    class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.ViewHolder> {

        ArrayList<DBModel> dataList;
        Context context;
        public CustomAdapter(Context c, ArrayList<DBModel> list){
            context = c;
            dataList = new ArrayList<>(list);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.car_details_list_item, parent, false);
            ViewHolder vh = new ViewHolder(v);
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int i) {
            holder.description.setText(dataList.get(i).getValue("description"));
            if(dataList.get(i) instanceof Recalls) {
                holder.title.setText(dataList.get(i).getValue("name"));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_error_red_600_24dp));
            }else if(dataList.get(i) instanceof DTCs) {
                holder.title.setText(dataList.get(i).getValue("dtcCode"));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_announcement_blue_600_24dp));
            } else if(dataList.get(i) instanceof Services){
                holder.description.setText(dataList.get(i).getValue("itemDescription"));
                holder.title.setText(dataList.get(i).getValue("item"));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }else{
                holder.description.setText(dataList.get(i).getValue("itemDescription"));
                holder.title.setText(dataList.get(i).getValue("item"));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_check_circle_green_400_36dp));
            }

            if(dataList.get(i) instanceof Recalls || dataList.get(i) instanceof DTCs ||
                    dataList.get(i) instanceof  Services) {
                holder.container.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(CarDetailsActivity.this, DisplayItemActivity.class);
                        intent.putExtra("Model", dataList.get(i));
                        intent.putExtra("VIN",VIN);
                        startActivity(intent);
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return dataList.size();
        }



        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        public class ViewHolder extends RecyclerView.ViewHolder {
            // each data item is just a string in this case
            public TextView title;
            public TextView description;
            public ImageView imageView;
            public CardView container;
            public ViewHolder(View v) {
                super(v);
                title = (TextView) v.findViewById(R.id.title);
                description = (TextView) v.findViewById(R.id.description);
                imageView = (ImageView) v.findViewById(R.id.image_icon);
                container = (CardView) v.findViewById(R.id.list_car_item);
            }
        }
    }
}
