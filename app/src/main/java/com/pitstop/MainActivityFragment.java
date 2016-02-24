package com.pitstop;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.amlcurran.showcaseview.ShowcaseView;
import com.github.amlcurran.showcaseview.SimpleShowcaseEventListener;
import com.github.amlcurran.showcaseview.targets.ViewTarget;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Shops;
import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.ToolbarActionItemTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.PitstopPushBroadcastReceiver.ACTION_UPDATE_MILEAGE;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_ACTION;

/**
 * A placeholder fragment containing a simple view.
 */
public class MainActivityFragment extends Fragment {
    private ParseApplication baseApplication;

    public static final String TAG = MainActivityFragment.class.getSimpleName();
    private static String currentGarage = "";
    private static String garagePhoneNumber = "";
    private static String garageEmailAddress ="";
    private static String garageAddress = "";

    TextView callGarageTextView;
    TextView messageGarageTextView;
    TextView directionsToGarageTextView;


    private ArrayList<DBModel> array;
    public MainActivityFragment() {
    }

    @Override
    public void onCreate (Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        baseApplication = (ParseApplication) getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        array=((MainActivity)getActivity()).array;
        setUp();
        showTutorial();
    }

    private void showTutorial() {


        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.pfName, Activity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        if(settings.getBoolean("FirstAppOpen",false)==false) {
            new ShowcaseView.Builder(getActivity())
                    .setTarget(new ViewTarget(getActivity().findViewById(R.id.button5)))
                    .setContentTitle("View Your Car Information")
                    .setContentText("Click this button to see more detailed view of your car")
                    .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                        @Override
                        public void onShowcaseViewDidHide(ShowcaseView v) {
                            final Toolbar toolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
                            new ShowcaseView.Builder(getActivity())
                                    .setTarget(new ViewTarget(getActivity().findViewById(R.id.message_garage_icon)))
                                    .setContentTitle("Your Dealership")
                                    .setContentText("Feel free to click these to message/call/get directions to your dealership. You can edit this in your settings.")
                                    .setShowcaseEventListener(new SimpleShowcaseEventListener() {
                                        @Override
                                        public void onShowcaseViewDidHide(ShowcaseView v) {
                                            new ShowcaseView.Builder(getActivity())
                                                    .setTarget(new ToolbarActionItemTarget(toolbar, R.id.add))
                                                    .setContentTitle("Add Car")
                                                    .setStyle(R.style.CustomShowcaseTheme2)
                                                    .setContentText("Click here to add a new car to your library")
                                                    .withMaterialShowcase()
                                                    .build()
                                                    .show();

                                        }
                                    })
                                    .withMaterialShowcase()
                                    .setStyle(R.style.CustomShowcaseTheme2)
                                    .build()
                                    .show();
                        }
                    })
                    .setStyle(R.style.CustomShowcaseTheme2)
                    .withMaterialShowcase()
                    .build()
                    .show();
            editor.putBoolean("FirstAppOpen",true);
            editor.commit();
        }
    }

    public void setTextViews() {
        callGarageTextView = (TextView) getActivity().findViewById(R.id.call_garage);
        callGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseApplication.getMixpanelAPI().track("Car Call Garage Pressed - Single Car View");
                baseApplication.getMixpanelAPI().flush();
                Log.d(TAG, "phone number is " + garagePhoneNumber);
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + garagePhoneNumber));
                startActivity(intent);
            }
        });

        messageGarageTextView = (TextView) getActivity().findViewById(R.id.message_garage);
        messageGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseApplication.getMixpanelAPI().track("Car Msg Garage Pressed - Single Car View");
                baseApplication.getMixpanelAPI().flush();
                Log.d(TAG, "phone number is " + garagePhoneNumber);

                User.getCurrentUser().setFirstName(ParseUser.getCurrentUser().getString("name"));
                User.getCurrentUser().setEmail(ParseUser.getCurrentUser().getEmail());
                ConversationActivity.show(getContext());
            }
        });
        directionsToGarageTextView = (TextView) getActivity().findViewById(R.id.directions_to_garage);
        directionsToGarageTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                baseApplication.getMixpanelAPI().track("Car Map Garage Pressed - Single Car View");
                baseApplication.getMixpanelAPI().flush();
                Log.d(TAG, "address is " + garageAddress);
                String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%s", garageAddress);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                startActivity(intent);
            }
        });
    }


    public void getGarage() {
        final LocalDataRetriever ldr = new LocalDataRetriever(getContext());
        String shopId = array.get(0).getValue("dealership");
        Shops currShop = (Shops)ldr.getData("Shops", "ShopID", shopId);
        if (currShop!=null) {
            currentGarage = currShop.getValue("name");
            garagePhoneNumber = currShop.getValue("phoneNumber");
            garageEmailAddress = currShop.getValue("email");
            garageAddress = currShop.getValue("address");
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
                            garageEmailAddress = parseObject.get("email").toString();
                            garageAddress = parseObject.get("addressText").toString();
                            Shops shop = new Shops();
                            shop.setValue("ShopID", finalShopId);
                            shop.setValue("name", currentGarage);
                            shop.setValue("address", garageAddress);
                            shop.setValue("phoneNumber", garagePhoneNumber);
                            shop.setValue("email", garageEmailAddress);
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

    /**
     * Big setup for getting the display ready
     */
    public void setUp(){
        setTextViews();
        getGarage();
        final DBModel car = array.get(0);

        ((TextView) getActivity().findViewById(R.id.carName)).setText(car.getValue("make") + " " + car.getValue("model"));
        ((TextView) getActivity().findViewById(R.id.year)).setText(car.getValue("year"));

        int recallCount = car.getValue("numberOfRecalls") == null? 0 : Integer.valueOf(car.getValue("numberOfRecalls"));
        int serviceCount = car.getValue("numberOfServices") == null? 0 : Integer.valueOf(car.getValue("numberOfServices"));
        int totalServiceCount = recallCount + serviceCount;
        ((TextView) getActivity().findViewById(R.id.serviceCountTextSingleCar)).setText(String.valueOf(totalServiceCount));

        if (totalServiceCount > 0) {
            // set color to red
            (getActivity().findViewById(R.id.serviceCountBackgroundSingleCar)).setBackgroundColor(Color.rgb(203, 77, 69));
        }
        else {
            // set color to green
            (getActivity().findViewById(R.id.serviceCountBackgroundSingleCar)).setBackgroundColor(Color.rgb(93, 172, 129));
        }

        getActivity().findViewById(R.id.button5).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                baseApplication.getMixpanelAPI().track("Car Detail Button Clicked - Single Car View");
                baseApplication.getMixpanelAPI().flush();
                Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                if (getArguments() != null && ACTION_UPDATE_MILEAGE.equals(getArguments().getString(EXTRA_ACTION))) {
                    // clear the action so it's not repeated
                    getArguments().putString(EXTRA_ACTION, null);
                    intent.putExtra(EXTRA_ACTION, ACTION_UPDATE_MILEAGE);
                }
                intent.putExtra("title", car.getValue("make") + " " + car.getValue("model"));
                //edmund
                String temp = car.getValue("pendingEdmundServices");
                if (!temp.equals("")) {
                    intent.putExtra("edmund", temp.substring(1, temp.length() - 1).split(","));
                } else {
                    intent.putExtra("edmund", new String[]{});
                }
                //interval
                temp = car.getValue("pendingIntervalServices");
                if (!temp.equals("")) {
                    intent.putExtra("interval", temp.substring(1, temp.length() - 1).split(","));
                } else {
                    intent.putExtra("interval", new String[]{});
                }
                //fixed
                temp = car.getValue("pendingFixedServices");
                if (!temp.equals("")) {
                    intent.putExtra("fixed", temp.substring(1, temp.length() - 1).split(","));
                } else {
                    intent.putExtra("fixed", new String[]{});
                }
                //recalls
                temp = car.getValue("recalls");
                if (!temp.equals("")) {
                    intent.putExtra("pendingRecalls", temp.substring(1, temp.length() - 1).split(","));
                } else {
                    intent.putExtra("pendingRecalls", new String[]{});
                }
                //dtcs
                temp = car.getValue("dtcs");
                if (!temp.equals("")) {
                    intent.putExtra("dtcs", temp.substring(1, temp.length() - 1).split(","));
                } else {
                    intent.putExtra("dtcs", new String[]{});
                }
                intent.putExtra("CarID", car.getValue("CarID"));
                intent.putExtra("vin", car.getValue("VIN"));
                intent.putExtra("scannerId", car.getValue("scannerId"));
                intent.putExtra("make", car.getValue("make"));
                intent.putExtra("shopId", car.getValue("dealership"));
                intent.putExtra("model", car.getValue("model"));
                intent.putExtra("year", car.getValue("year"));
                intent.putExtra("baseMileage", car.getValue("baseMileage"));
                intent.putExtra("totalMileage", car.getValue("totalMileage"));
                startActivity(intent);
            }
        });
    }

    /**
     * Link car to device if device is new to user, and change colors of connected cars!
     * @param id
     */
    public void indicateConnected(final String id) {
        boolean found = false;
        Cars noDevice = null;
        for(DBModel a : array){
            if(a.getValue("scannerId").equals("")){
                noDevice = (Cars) a;
            }
            if(a.getValue("scannerId").equals(id)){
                found = true;
                getActivity().findViewById(R.id.button5).setBackgroundResource(R.drawable.color_button_car_connected);
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
                    objects.get(0).put("scannerId",id);
                    objects.get(0).saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            finalNoDevice.setValue("scannerId", id);
                            LocalDataRetriever ldr = new LocalDataRetriever(getContext());
                            HashMap<String,String> map = new HashMap<String,String>();
                            map.put("scannerId",id);
                            ldr.updateData("Cars", "VIN", finalNoDevice.getValue("VIN"), map);
                            Toast.makeText(getContext(),"Car successfully linked",Toast.LENGTH_SHORT).show();
                            ((MainActivity)getActivity()).service.getDTCs();
                        }
                    });
                }
            });
        }
    }
}
