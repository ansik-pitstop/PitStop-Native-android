package com.pitstop;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
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

import com.afollestad.materialdialogs.MaterialDialog;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.parse.FindCallback;
import com.parse.FunctionCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.DTCs;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;
import com.pitstop.database.models.Cars;
import static com.pitstop.PitstopPushBroadcastReceiver.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CarDetailsActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener{

    public static final String TAG = CarDetailsActivity.class.getSimpleName();
    private CustomAdapter customAdapter;
    private ArrayList<DBModel> arrayList = new ArrayList<>();
    private HashMap<String,Object> output = new HashMap<String, Object>();

    private boolean requestSent = false;

    private String carId, VIN, scannerID,make, model,year,baseMileage;


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
        boolean serviceGet=false, recallsGet=false, dtcsGet = false;

        carId = getIntent().getStringExtra("CarID");
        VIN = getIntent().getStringExtra("vin");
        scannerID = getIntent().getStringExtra("scannerId");
        make = getIntent().getStringExtra("make");
        model = getIntent().getStringExtra("model");
        year = getIntent().getStringExtra("year");
        baseMileage = getIntent().getStringExtra("baseMileage");

        serviceIntent= new Intent(CarDetailsActivity.this, BluetoothAutoConnectService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        ((TextView)findViewById(R.id.mileage)).setText(baseMileage);
        findViewById(R.id.update_mileage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialDialog.Builder(CarDetailsActivity.this)
                        .title("Update Mileage")
                        .inputType(InputType.TYPE_CLASS_NUMBER)
                        .input("Enter Mileage", "", false /* allowEmptyInput */, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(MaterialDialog dialog, CharSequence input) {
                                updateMileage(input);
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
        Object[] a = (Object[]) getIntent().getSerializableExtra("servicesDue");
        final HashMap<Integer,Boolean> serviceCodes = new HashMap<Integer,Boolean>();
        for (int i = 0; i<a.length; i++){
            if(a[i].toString().trim().equals("")) {
                break;
            }
            serviceCodes.put(Integer.parseInt(a[i].toString().trim()), false);
        }
        //check DB first
        for (int i : serviceCodes.keySet()){
            Services service;
            service = (Services) ldr.getData("Services", "ServiceID", String.valueOf(i));
            if(service ==null){
                serviceGet = true;//go get some missing services
            }else {
                arrayList.add(service);
                serviceCodes.put(i, true);
            }
        }
        //if need to get some services
        if(serviceGet){
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Service");
            query.whereContainedIn("serviceId", serviceCodes.keySet());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    for (ParseObject parseObject : objects) {
                        Services service = new Services();
                        service.setValue("item", parseObject.getString("item"));
                        service.setValue("action", parseObject.getString("action"));
                        service.setValue("description", parseObject.getString("itemDescription"));
                        service.setValue("intervalMileage", parseObject.getString("intervalMileage"));
                        service.setValue("priority", parseObject.getString("priority"));
                        service.setValue("engineCode", parseObject.getString("engineCode"));
                        service.setValue("ServiceID", String.valueOf(parseObject.getInt("serviceId")));

                        if (!serviceCodes.get(parseObject.getInt("serviceId"))) {
                            ldr.saveData("Services", service.getValues());
                            arrayList.add(service);
                        }
                    }
                    customAdapter.dataList.clear();
                    customAdapter.dataList.addAll(arrayList);
                    customAdapter.notifyDataSetChanged();
                }
            });
        }

        //--------------------------------GET RECALLS-------------------------------
        a = (Object[]) getIntent().getSerializableExtra("pendingRecalls");
        final HashMap<String,Boolean> recallCodes = new HashMap<String,Boolean>();
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
                arrayList.add(service);
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
                            ldr.saveData("Recalls",recall.getValues());
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

        customAdapter = new CustomAdapter(this,arrayList);
        ((ListView) findViewById(R.id.car_event_listview)).setAdapter(customAdapter);
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

    private void updateMileage(CharSequence chsq) {
        String mileage = chsq.toString();

        // save to parse
        try {
            ParseQuery<ParseObject> cars = ParseQuery.getQuery("Car");
            ParseObject car = cars.get(carId);
            car.put("baseMileage", Integer.parseInt(mileage));
            car.saveEventually();

        } catch (ParseException e) {
            Log.e("CarDetailsActivity", "parse exception: ", e);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid mileage", Toast.LENGTH_SHORT).show();
        }

        // save to local DB
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Toast.makeText(this, carId, Toast.LENGTH_SHORT).show();
        Cars c = (Cars)ldr.getData("Cars", "CarID", carId);
        c.setValue("baseMileage", mileage);
        ldr.saveData("Cars", c.getValues());

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
                services.add(model.getValues());
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

    class CustomAdapter extends BaseAdapter {

        ArrayList<DBModel> dataList;
        Context context;
        public CustomAdapter(Context c, ArrayList<DBModel> list){
            context = c;
            dataList = new ArrayList<>(list);
        }

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Object getItem(int i) {
            return dataList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            RelativeLayout convertview = (RelativeLayout)view;
            LayoutInflater inflater = LayoutInflater.from(context);
            convertview = (RelativeLayout)inflater.inflate(R.layout.car_details_list_item, null);
            ((TextView)convertview.findViewById(R.id.description)).setText(dataList.get(i).getValue("description"));
            if(dataList.get(i) instanceof Recalls) {
                ((TextView)convertview.findViewById(R.id.title)).setText(dataList.get(i).getValue("name"));
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getResources().getDrawable(R.drawable.ic_error_red_600_24dp));
            }else if(dataList.get(i) instanceof DTCs) {
                ((TextView) convertview.findViewById(R.id.title)).setText(dataList.get(i).getValue("dtcCode"));
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getResources().getDrawable(R.drawable.ic_announcement_blue_600_24dp));
            }else{
                ((TextView)convertview.findViewById(R.id.title)).setText(dataList.get(i).getValue("action"));
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getResources().getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }
            convertview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(CarDetailsActivity.this,DisplayItemActivity.class);
                    intent.putExtra("Model",dataList.get(i));
                    startActivity(intent);
                }
            });
            return convertview;
        }
    }
}
