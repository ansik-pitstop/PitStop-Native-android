package com.pitstop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.Preference;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Shops;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    public static final String TAG = MainActivityFragment.class.getSimpleName();
    private static String currentGarage = "";
    private static String garagePhoneNumber = "";
    private static String garageAddress = "";

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";

    final static String pfShopName = "com.pitstop.shop.name";
    final static String pfCodeForShopObjectID = "com.pitstop.shop.objectID";

    TextView callGarageTextView;
    TextView messageGarageTextView;
    TextView directionsToGarageTextView;


    private ArrayList<DBModel> array;
    public MainActivityFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setTextViews();
        setUp();
        getGarage();
    }

    public void setTextViews() {
        callGarageTextView = (TextView) getView().findViewById(R.id.call_garage);
        callGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "phone number is " + garagePhoneNumber);
                Intent intent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + garagePhoneNumber));
                startActivity(intent);
            }
        });

        messageGarageTextView = (TextView) getView().findViewById(R.id.message_garage);
        messageGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "phone number is " + garagePhoneNumber);
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setData(Uri.parse("sms:" + garagePhoneNumber));
                startActivity(sendIntent);
            }
        });
        directionsToGarageTextView = (TextView) getView().findViewById(R.id.directions_to_garage);
        directionsToGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "address is " + garageAddress);
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%s", garageAddress);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });
    }


    public void getGarage() {
        final LocalDataRetriever ldr = new LocalDataRetriever(getContext());
        SharedPreferences settings = getActivity().getSharedPreferences(pfShopName, getContext().MODE_PRIVATE);
        String shopId = settings.getString(pfCodeForShopObjectID, "NA");
        if (shopId.equals("NA")) {
            SharedPreferences.Editor editor = settings.edit();
            if(ParseUser.getCurrentUser().getParseObject("subscribedShopPointer")!=null) {
                editor.putString(MainActivityFragment.pfCodeForShopObjectID,ParseUser.getCurrentUser().getParseObject("subscribedShopPointer").getObjectId());
                shopId  = ParseUser.getCurrentUser().getParseObject("subscribedShopPointer").getObjectId();
            }else{
                callGarageTextView.setText("Shop not set up");
                messageGarageTextView.setText("Shop not set up");
                directionsToGarageTextView.setText("Shop not set up");
                return;
            }
        }
        Shops currShop = (Shops)ldr.getData("Shops", "ShopID", shopId);
        if (currShop!=null) {
            currentGarage = currShop.getValue("name");
            garagePhoneNumber = currShop.getValue("phoneNumber");
            garageAddress = currShop.getValue("addressText");
            callGarageTextView.setText("Call " + currentGarage);
            messageGarageTextView.setText("Message " + currentGarage);
            directionsToGarageTextView.setText("Directions to " + currentGarage);
        } else {
            final String finalShopId = shopId;
            ParseQuery<ParseObject> query = ParseQuery.getQuery("Shop");
            query.whereEqualTo("objectId", shopId);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, com.parse.ParseException e) {
                    if (e == null) {
                        for (ParseObject parseObject : parseObjects) {
                            currentGarage = parseObject.get("name").toString();
                            garagePhoneNumber = parseObject.get("phoneNumber").toString();
                            garageAddress = parseObject.get("addressText").toString();
                            Shops shop = new Shops();
                            shop.setValue("ShopID", finalShopId);
                            shop.setValue("name", currentGarage);
                            shop.setValue("address", garageAddress);
                            shop.setValue("phoneNumber", garagePhoneNumber);
                            shop.setValue("email", parseObject.get("email").toString());
                            ldr.saveData("Shops",shop.getValues());
                        }
                    } else {
                        Log.d("ERROR:", "" + e.getMessage());
                    }
                    callGarageTextView.setText("Call " + currentGarage);
                    messageGarageTextView.setText("Message " + currentGarage);
                    directionsToGarageTextView.setText("Directions to " + currentGarage);
                }
            });
        }
    }

    public void setUp(){
        for(int i = 0; i<((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildCount()-1; i++) {
            ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).removeViewAt(0);
        }
        final LocalDataRetriever ldr = new LocalDataRetriever(getContext());
        SharedPreferences settings = getActivity().getSharedPreferences(pfName, getContext().MODE_PRIVATE);
        String userId = settings.getString(pfCodeForObjectID, "NA");
        array = ldr.getDataSet("Cars", "owner", userId);
        if(array.size()>0){
            for (final DBModel car : array) {
                LayoutInflater inflater =
                        (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final LinearLayout itemBox = (LinearLayout) inflater.inflate(R.layout.car_button, null);

                ((TextView) itemBox.findViewById(R.id.car_title)).setText(car.getValue("make") + " " + car.getValue("model"));
                itemBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                        intent.putExtra("title", car.getValue("make") + " " + car.getValue("model"));
                        String temp = car.getValue("services");
                        if(!temp.equals("")) {
                            intent.putExtra("servicesDue", temp.substring(1, temp.length() - 1).split(","));
                        }else{
                            intent.putExtra("servicesDue",new String[]{});
                        }
                        temp = car.getValue("recalls");
                        if(!temp.equals("")) {
                            intent.putExtra("pendingRecalls", temp.substring(1, temp.length() - 1).split(","));
                        }else{
                            intent.putExtra("pendingRecalls",new String[]{});
                        }

                        temp = car.getValue("dtcs");
                        if(!temp.equals("")) {
                            intent.putExtra("dtcs", temp.substring(1, temp.length() - 1).split(","));
                        }else{
                            intent.putExtra("dtcs",new String[]{});
                        }
                        intent.putExtra("vin",car.getValue("VIN"));
                        startActivity(intent);
                    }
                });
                itemBox.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.button_width), getResources().getDimensionPixelSize(R.dimen.button_height));
                params.rightMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).addView(itemBox, 0, params);
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
                            LayoutInflater inflater =
                                    (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                            final LinearLayout itemBox = (LinearLayout) inflater.inflate(R.layout.car_button, null);
                            Cars c = new Cars();

                            c.setValue("CarID", car.getString("objectId"));
                            c.setValue("owner", car.getString("owner"));
                            c.setValue("scannerId", car.getString("scannerId"));
                            c.setValue("VIN", car.getString("VIN"));
                            c.setValue("baseMileage", car.getString("baseMileage"));
                            c.setValue("cityMileage", car.getString("city_mileage"));
                            c.setValue("highwayMileage", car.getString("highway_mileage"));
                            c.setValue("engine", car.getString("engine"));
                            c.setValue("make", car.getString("make"));
                            c.setValue("model", car.getString("model"));
                            c.setValue("year", car.getString("year"));
                            c.setValue("tank_size", car.getString("tank_size"));
                            c.setValue("totalMileage", car.getString("totalMileage"));
                            c.setValue("trimLevel", car.getString("trim_level"));
                            c.setValue("services", (car.get("servicesDue")==null?"":car.get("servicesDue").toString()));
                            c.setValue("dtcs", (car.get("storedDTCs")==null?"":car.get("storedDTCs").toString()));
                            c.setValue("recalls", (car.get("pendingRecalls")==null?"":car.get("pendingRecalls").toString()));
                            ldr.saveData("Cars", c.getValues());
                            ((TextView) itemBox.findViewById(R.id.car_title)).setText(car.getString("make") + " " + car.getString("model"));
                            itemBox.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                                    intent.putExtra("title", car.getString("make") + " " + car.getString("model"));
                                    intent.putExtra("servicesDue", (car.getList("servicesDue")==null?new String[]{}:car.getList("servicesDue").toArray()));
                                    intent.putExtra("pendingRecalls", (car.getList("pendingRecalls")==null?new String[]{}:car.getList("pendingRecalls").toArray()));
                                    intent.putExtra("dtcs", (car.getList("storedDTCs")==null?new String[]{}:car.getList("storedDTCs").toArray()));
                                    intent.putExtra("vin", car.getString("VIN"));
                                    startActivity(intent);
                                }
                            });
                            itemBox.setGravity(Gravity.CENTER);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(getResources().getDimensionPixelSize(R.dimen.button_width), getResources().getDimensionPixelSize(R.dimen.button_height));
                            params.rightMargin = getResources().getDimensionPixelSize(R.dimen.activity_horizontal_margin);
                            ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).addView(itemBox, 0, params);
                        }
                    }
                }
            });
        }
    }

    public void indicateConnected(final String id) {
        boolean found = false;
        Cars noDevice = null;
        for(DBModel a : array){
            if(a.getValue("scannerId").equals("")){
                noDevice = (Cars) a;
            }
            if(a.getValue("scannerId").equals(id)){
                found = true;
                for(int i = 0; i<((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildCount()-1; i++) {
                    TextView tv = (TextView) ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildAt(i).findViewById(R.id.car_title);
                    if(tv.getText().toString().contains(a.getValue("make"))&&tv.getText().toString().contains(a.getValue("model"))){
                        ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildAt(i).setBackgroundResource(R.drawable.color_button_car_connected);
                    }
                }
            }
        }
        // add device if a car has no linked device
        if(!found&&noDevice!=null){
            final Cars finalNoDevice = noDevice;
            ParseQuery query = new ParseQuery("Car");
            query.whereEqualTo("VIN",noDevice.getValue("VIN"));
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    objects.get(0).add("scannerId",id);
                    objects.get(0).saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            finalNoDevice.setValue("scannerId", id);
                            LocalDataRetriever ldr = new LocalDataRetriever(getContext());
                            HashMap<String,String> map = new HashMap<String,String>();
                            map.put("scannerId",id);
                            ldr.updateData("Cars", "VIN", finalNoDevice.getValue("VIN"), map);
                            Toast.makeText(getContext(),"Car successfully linked",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }
    }
}
