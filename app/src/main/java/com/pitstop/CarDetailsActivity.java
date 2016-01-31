package com.pitstop;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
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
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;
import com.pitstop.database.models.Cars;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.pitstop.PitstopPushBroadcastReceiver.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CarDetailsActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener{

    public static final String TAG = CarDetailsActivity.class.getSimpleName();
    private CustomAdapter customAdapter;
    private ArrayList<DBModel> arrayList = new ArrayList<>();
    private HashMap<String,Object> output = new HashMap<String, Object>();

    private boolean requestSent = false;

    private String carId, VIN, scannerID,make, model,year,baseMileage, totalMileage, shopId;
    private HashMap<String,Boolean> recallCodes;

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
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);
        setTitle(getIntent().getExtras().getString("title").toUpperCase());
        boolean recallsGet=false, dtcsGet = false;

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
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        Log.d("total mileage", totalMileage);
        Log.d("base mileage", baseMileage);

        //setup mileage
        if (totalMileage == null) { // total mileage was not calculated in backend yet
            ((TextView)findViewById(R.id.mileage)).setText(baseMileage);
        }
        else {
            ((TextView)findViewById(R.id.mileage)).setText(totalMileage);
        }


        findViewById(R.id.update_mileage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final EditText input = new EditText(getApplicationContext());
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

        LayoutInflater inflater = (LayoutInflater) getApplicationContext().getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.unconnected_car_display,((LinearLayout)findViewById(R.id.carStatus)), false);

        ((LinearLayout)findViewById(R.id.carStatus)).addView(view);

        final LocalDataRetriever ldr = new LocalDataRetriever(this);
        //--------------------------GET SERVICES--------------------------

        Bundle extras = getIntent().getExtras();
        getEdmundsServices(extras, ldr);
        getIntervalServices(extras, ldr);
        getFixedServices(extras,ldr);
        //--------------------------------GET RECALLS-------------------------------


        Object [] a = (Object[]) getIntent().getSerializableExtra("pendingRecalls");
        recallCodes = new HashMap<String,Boolean>();
        for (int i = 0; i<a.length; i++){
            recallCodes.put(a[i].toString(), false);
        }

        //check DB first
        for (String i : recallCodes.keySet()){
            Recalls service;
            service = (Recalls) ldr.getData("Recalls", "RecallID", i.trim());
            if(service ==null){
                recallsGet= true;//go get some missing services
            }else {
                if(service.getValue("state").equals("new")||service.getValue("state").equals("pending")) {
                    arrayList.add(service);
                }
                recallCodes.put(i, true);
            }
        }
        //see if need to get from online
        if (recallsGet) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("RecallEntry");
            query.whereContainedIn("objectId", recallCodes.keySet());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    for (ParseObject parseObject : objects) {
                        Recalls recall = new Recalls();

                        recall.setValue("RecallID",parseObject.getObjectId());
                        recall.setValue("name",parseObject.getString("name"));
                        recall.setValue("description",parseObject.getString("description"));
                        recall.setValue("remedy",parseObject.getString("remedy"));
                        recall.setValue("risk",""+parseObject.getNumber("risk"));
                        recall.setValue("effectiveDate",parseObject.getString("effectiveDate"));
                        recall.setValue("oemID",parseObject.getString("oemID"));
                        recall.setValue("reimbursement",""+parseObject.getNumber("reimbursement"));
                        recall.setValue("state",parseObject.getString("state"));
                        recall.setValue("riskRank",""+parseObject.getNumber("riskRank"));
                        if (!recallCodes.get(recall.getValue("RecallID"))){
                            ldr.saveData("Recalls", recall.getValues());
                            if(parseObject.getString("state").equals("new")||parseObject.getString("state").equals("pending"))
                                arrayList.add(recall);
                        }
                    }
                    customAdapter.dataList.clear();
                    customAdapter.dataList.addAll(arrayList);
                    customAdapter.notifyDataSetChanged();
                }
            });
        }

        //--------------------------------GET DTCS-------------------------------
        a = (Object[]) getIntent().getSerializableExtra("dtcs");
        final HashMap<String,Boolean> dtcList = new HashMap<String,Boolean>();
        for (int i = 0; i<a.length; i++){
            dtcList.put(a[i].toString(), false);
        }
        //check DB first
        for (String i : dtcList.keySet()){
            DTCs dtc;
            dtc = (DTCs) ldr.getData("DTCs", "dtcCode",i.trim());
            if(dtc ==null){
                dtcsGet= true;//go get some missing services
            }else {
                arrayList.add(dtc);
                dtcList.put(i, true);
            }
        }
        //see if need to get from online
        if (dtcsGet) {
            ParseQuery<ParseObject> query = ParseQuery.getQuery("DTC");
            query.whereContainedIn("dtcCode", dtcList.keySet());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    for (ParseObject parseObject : objects) {
                        DTCs dtc = new DTCs();
                        dtc.setValue("dtcCode", parseObject.getString("dtcCode"));
                        dtc.setValue("description", parseObject.getString("description"));
                        if (!dtcList.get(dtc.getValue("dtcCode"))){
                            ldr.saveData("DTCs",dtc.getValues());
                            arrayList.add(dtc);
                        }
                    }
                    customAdapter.dataList.clear();
                    customAdapter.dataList.addAll(arrayList);
                    customAdapter.notifyDataSetChanged();
                }
            });
        }

        // set up listview
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
                Intent intent = new Intent(CarDetailsActivity.this, EnviornmentalCheckActivity.class);
                startActivity(intent);
            }
        });
    }

    private void setSwipeDeleteListener(RecyclerView mRecyclerView){
        SwipeableRecyclerViewTouchListener swipeTouchListener =
                new SwipeableRecyclerViewTouchListener(mRecyclerView,
                        new SwipeableRecyclerViewTouchListener.SwipeListener() {
                            @Override
                            public boolean canSwipe(int position) {
                                if(customAdapter.dataList.get(position) instanceof DTCs){
                                    return false;
                                }
                                return true;
                            }

                            @Override
                            public void onDismissedBySwipeLeft(RecyclerView recyclerView, final int[] reverseSortedPositions) {
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
                                            //services
                                            if(customAdapter.dataList.get(i) instanceof Services) {
                                                final String type = customAdapter.dataList.get(i).getValue("serviceType");
                                                final int typeService = (type.equals("edmunds") ? 0 : (type.equals("fixed") ? 1 : 2));
//                                            Toast.makeText(getApplicationContext(), times[i], Toast.LENGTH_SHORT).show();
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
                                                        Toast.makeText(getApplicationContext(), "Updated Service History", Toast.LENGTH_SHORT).show();
                                                    }
                                                });

                                                ParseQuery updateObject = new ParseQuery("Car");
                                                try {
                                                    updateObject.get(carId);
                                                    updateObject.findInBackground(new FindCallback<ParseObject>() {

                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            String type = (typeService==0?"pendingEdmundServices":(typeService==1?"pendingFixedServices":"pendingIntervalServices"));
                                                            JSONArray original = objects.get(0).getJSONArray(type);
                                                            ArrayList<String> updatedTexts = new ArrayList<>();
                                                            for (int j =0 ; j< original.length(); j++){
                                                                try {
                                                                    if(!original.getString(j).equals(customAdapter.dataList.get(i).getValue("ParseID"))){
                                                                        updatedTexts.add(original.getString(j));
                                                                    }
                                                                } catch (JSONException e1) {
                                                                    e1.printStackTrace();
                                                                }
                                                            }
                                                            objects.get(0).put(type, updatedTexts);
                                                            ((ParseObject)objects.get(0)).saveEventually();
                                                            LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
                                                            HashMap<String,String> map = new HashMap<String,String>();
                                                            map.put(type,"["+ TextUtils.join(",",updatedTexts)+"]");
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
                                            }else{//if Recalls
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
                                                        Toast.makeText(getApplicationContext(), "Updated Service History", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                ParseQuery updateObject = new ParseQuery("RecallEntry");
                                                try {
                                                    updateObject.get(customAdapter.dataList.get(i).getValue("RecallID"));
                                                    updateObject.findInBackground(new FindCallback<ParseObject>() {

                                                        @Override
                                                        public void done(List<ParseObject> objects, ParseException e) {
                                                            objects.get(0).put("state","doneByUser");
                                                            ((ParseObject)objects.get(0)).saveEventually(new SaveCallback() {
                                                                @Override
                                                                public void done(ParseException e) {
                                                                }
                                                            });
                                                            LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
                                                            HashMap<String,String> map = new HashMap<String,String>();
                                                            recallCodes.remove(customAdapter.dataList.get(i).getValue("RecallID"));
                                                            map.put("recalls", "[" + TextUtils.join(",", recallCodes.keySet()) + "]");
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
                                            MainActivity.refresh=true;
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
                        customAdapter.dataList.clear();
                        customAdapter.dataList.addAll(arrayList);
                        customAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

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
                        for (ParseObject parseObject : objects) {
                            Services service = new Services();
                            service.setValue("item", parseObject.getString("item"));
                            service.setValue("serviceType", "interval");
                            service.setValue("itemDescription", parseObject.getString("itemDescription"));
                            service.setValue("action", parseObject.getString("action"));
                            service.setValue("ParseID", parseObject.getObjectId());
                            //end key, now custom
                            service.setValue("dealership", parseObject.getString("dealership"));
                            service.setValue("priority",""+ parseObject.getNumber("priority"));
                            service.setValue("intervalMileage",""+ parseObject.getNumber("mileage"));

                            ldr.saveData("Services", service.getValues());
                            arrayList.add(service);
                        }
                        customAdapter.dataList.clear();
                        customAdapter.dataList.addAll(arrayList);
                        customAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

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
                        for (ParseObject parseObject : objects) {
                            Services service = new Services();
                            service.setValue("item", parseObject.getString("item"));
                            service.setValue("serviceType", "edmunds");
                            service.setValue("itemDescription", parseObject.getString("itemDescription"));
                            service.setValue("action", parseObject.getString("action"));
                            service.setValue("ParseID", parseObject.getObjectId());
                            //end key, now custom
                            service.setValue("intervalMileage",""+ parseObject.getNumber("intervalMileage"));
                            service.setValue("intervalMonth",""+ parseObject.getNumber("intervalMonth"));

                            ldr.saveData("Services", service.getValues());
                            arrayList.add(service);
                        }
                        customAdapter.dataList.clear();
                        customAdapter.dataList.addAll(arrayList);
                        customAdapter.notifyDataSetChanged();
                    }
                });
            }
        }
    }

    private void updateMileage(CharSequence chsq) {
        String mileage = chsq.toString();

        // save to parse
        try {
            final HashMap<String, Object> params = new HashMap<String, Object>();

            params.put("carVin", VIN);
            params.put("mileage", Integer.valueOf(mileage));

            ParseCloud.callFunctionInBackground("carServicesUpdate", params, new FunctionCallback<Object>() {
                public void done(Object o, ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "mileage updated", Toast.LENGTH_SHORT).show();
                    }
                    else {
                        Toast.makeText(getApplicationContext(), "failed to update mileage", Toast.LENGTH_SHORT).show();
                    }
                }
            });


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
//        Toast.makeText(this, carId, Toast.LENGTH_SHORT).show();
        HashMap<String, String> hm = new HashMap<String, String>();
        hm.put("totalMileage", mileage);
        ldr.updateData("Cars", "CarID", carId, hm);

        // update the textview
        ((TextView)findViewById(R.id.mileage)).setText(mileage);

        // set mainactivity to refresh
        MainActivity.refresh = true;
    }

    public void requestServiceButton(String additional) {
        if(requestSent){
            Toast.makeText(getApplicationContext(), "Already Sent Request for Car!", Toast.LENGTH_SHORT).show();
            return;
        }
        requestSent = true;
        String userId = ParseUser.getCurrentUser().getObjectId();
        // Intent intent = getIntent();
        // String vin = Intent.getStringExtra("carVin")
        ArrayList<HashMap<String,String>> services = new ArrayList<>();
        for(DBModel model: arrayList){
            if(model instanceof Services){
                services.add(model.getValues());
            }
            if(model instanceof Recalls){
                Recalls recall = new Recalls();
                recall.setValue("item", model.getValue("name"));
                recall.setValue("action","Recall For");
                recall.setValue("itemDescription",model.getValue("description"));
                recall.setValue("priority",""+ 6); // high priority for recall
                services.add(recall.getValues());
            }
            if(model instanceof DTCs){
                DTCs dtc = new DTCs();
                dtc.setValue("item", model.getValue("dtcCode"));
                dtc.setValue("action","DTC/Engine Code");
                dtc.setValue("itemDescription",model.getValue("description"));
                dtc.setValue("priority",""+ 4); // high priority for recall
                services.add(dtc.getValues());
            }
        }
        output.put("services", services);
        output.put("carVin", VIN);
        output.put("userObjectId", userId);
        output.put("comments",additional);
        if(services.size()>0) {
            ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output, new FunctionCallback<Object>() {
                @Override
                public void done(Object object, ParseException e) {
                    if (e == null) {
                        Toast.makeText(getApplicationContext(), "Sent Successfully", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }else{
            Toast.makeText(getApplicationContext(), "Nothing to Send", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_car_details, menu);
        return true;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // unbind service to prevent memory leaks
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
            ServiceDialog dialog = new ServiceDialog();
            dialog.show(getSupportFragmentManager(),"sendSupportEmail");
        }
        if (id == R.id.history) {
            Intent intent = new Intent(CarDetailsActivity.this, CarHistoryActivity.class);
            intent.putExtra("carId",carId);
            startActivity(intent);
        }

        return super.onOptionsItemSelected(item);
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
        if(scannerID.equals(""+dataPackageInfo.deviceId)){
            ((LinearLayout)findViewById(R.id.carStatus)).removeAllViewsInLayout();
            LayoutInflater inflater = (LayoutInflater)getApplicationContext().getSystemService
                    (Context.LAYOUT_INFLATER_SERVICE);
            View view = inflater.inflate(R.layout.connected_car_display,((LinearLayout)findViewById(R.id.carStatus)), false);
            ((TextView)view.findViewById(R.id.make)).setText(make);
            ((TextView)view.findViewById(R.id.model)).setText(model);
            ((TextView)view.findViewById(R.id.year)).setText(year);
            ((LinearLayout)findViewById(R.id.carStatus)).addView(view);
        }
    }

    public class ServiceDialog extends DialogFragment {
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
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
        }
    }

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
            } else {
                holder.description.setText(dataList.get(i).getValue("itemDescription"));
                holder.title.setText(dataList.get(i).getValue("item"));
                holder.imageView.setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }
            holder.container.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(CarDetailsActivity.this, DisplayItemActivity.class);
                    intent.putExtra("Model", dataList.get(i));
                    startActivity(intent);
                }
            });
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
