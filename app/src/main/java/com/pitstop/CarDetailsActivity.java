package com.pitstop;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Recalls;
import com.pitstop.database.models.Services;
import com.pitstop.database.models.Cars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CarDetailsActivity extends AppCompatActivity {

    public static final String TAG = CarDetailsActivity.class.getSimpleName();
    private static String vin="";
    private CustomAdapter customAdapter;
    private ArrayList<DBModel> arrayList = new ArrayList<>();
    private ArrayList<HashMap<String,String>> servicesAppend = new ArrayList<HashMap<String,String>>();
    //to append services & recalls in one place.
    //private HashMap<String,String> services = new HashMap<String, String>();
    //output hashmap to aggregate services,VIN,userObjectId,Comments
    private HashMap<String,Object> output = new HashMap<String, Object>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_car_details);
        setTitle(getIntent().getExtras().getString("title").toUpperCase());
        boolean serviceGet=false, recallsGet=false;
        requestServiceButton();
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

                        //HashMap definitions
                        HashMap<String,String> services = new HashMap<String,String>();
                        services.put("item", parseObject.getString("item"));
                        services.put("action", parseObject.getString("action"));
                        services.put("itemDescription", parseObject.getString("itemDescription"));
                        services.put("priority", parseObject.getString("priority"));
                        services.put("serviceId", parseObject.getString("serviceId"));
                        services.put("serviceObjectId", parseObject.getString("serviceObjectId"));

                        servicesAppend.add(services);

                        if (!serviceCodes.get(parseObject.getInt("serviceId"))) {
                            ldr.saveData("Services",service.getValues());
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
            ParseQuery<ParseObject> query = ParseQuery.getQuery("EdmundsRecall");
            query.whereContainedIn("objectId", recallCodes.keySet());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    for (ParseObject parseObject : objects) {
                        Recalls recall = new Recalls();
                        recall.setValue("consequences", parseObject.getString("defectConsequence"));
                        recall.setValue("action", parseObject.getString("defectCorrectiveAction"));
                        recall.setValue("name", parseObject.getString("componentDescription"));
                        recall.setValue("description", parseObject.getString("defectDescription"));
                        recall.setValue("make", parseObject.getString("make"));
                        recall.setValue("model", parseObject.getString("model"));
                        recall.setValue("year", parseObject.getString("year"));
                        recall.setValue("recallNumber", parseObject.getString("recallNumber"));
                        recall.setValue("numberAffected", parseObject.getString("numberOfVehiclesAffected"));
                        recall.setValue("RecallID", parseObject.getObjectId());
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

    public void requestServiceButton() {
        String userId = ParseUser.getCurrentUser().getObjectId();
        ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
        query.whereEqualTo("owner", userId);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override

            public void done(List<ParseObject> parseObjects, com.parse.ParseException e) {
                if (e == null) {
                    for (ParseObject parseObject : parseObjects) {
                        vin = parseObject.get("VIN").toString();
                        //output.put("carVin", vin);
                        Log.i(TAG, "current VIN is " + vin);
                    }
                } else {
                    Log.d("ERROR:", "" + e.getMessage());
                }
            }
        });
        // Intent intent = getIntent();
        // String vin = Intent.getStringExtra("carVin")

        output.put("services", servicesAppend);
        output.put("carVin", vin);
        Log.i(TAG, "current VIN2 is " + vin);
        output.put("userObjectId", userId);
        output.put("comments","");

//        Button button = (Button) findViewById(R.id.button5);
//        button.setOnClickListener(new View.OnClickListener() {
//            public void onClick (View v){
//                ParseCloud.callFunctionInBackground("sendServiceRequestEmail", output);
//                // Log.d("test", temp);
//
//            }
//            // request services from PF object in PARSE
//        });

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
            return true;
        }

        return super.onOptionsItemSelected(item);
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
            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT);
            //params.setMargins(10, 10, 10, 10);
            if(dataList.get(i) instanceof Recalls) {
                ((TextView)convertview.findViewById(R.id.title)).setText(dataList.get(i).getValue("name"));
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getDrawable(R.drawable.ic_error_red_600_24dp));
            }else{
                ((TextView)convertview.findViewById(R.id.title)).setText(dataList.get(i).getValue("action"));
                ((ImageView) convertview.findViewById(R.id.image_icon)).setImageDrawable(getDrawable(R.drawable.ic_warning_amber_300_24dp));
            }
            convertview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(CarDetailsActivity.this,DisplayItemActivity.class);
                    intent.putExtra("Model",dataList.get(i));
                    startActivity(intent);
                }
            });
            convertview.setLayoutParams(params);
            return convertview;
        }
    }
}
