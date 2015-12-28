package com.pitstop;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {

    final static String pfName = "com.pitstop.login.name";
    final static String pfCodeForObjectID = "com.pitstop.login.objectID";
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
        setUp();
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

    public void indicateConnected(String id) {
        for(DBModel a : array){
            if(a.getValue("scannerId").equals(id)){
                for(int i = 0; i<((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildCount()-1; i++) {
                    TextView tv = (TextView) ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildAt(i).findViewById(R.id.car_title);
                    if(tv.getText().toString().contains(a.getValue("make"))&&tv.getText().toString().contains(a.getValue("model"))){
                        ((LinearLayout) getActivity().findViewById(R.id.horizontalScrollView)).getChildAt(i).setBackgroundColor(getResources().getColor(R.color.evcheck));
                    }
                }
            }
        }
    }
}
