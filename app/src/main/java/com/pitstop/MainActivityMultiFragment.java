package com.pitstop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import io.smooch.core.User;
import io.smooch.ui.ConversationActivity;

import static com.pitstop.PitstopPushBroadcastReceiver.ACTION_UPDATE_MILEAGE;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_ACTION;
import static com.pitstop.PitstopPushBroadcastReceiver.EXTRA_CAR_ID;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainActivityMultiFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainActivityMultiFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainActivityMultiFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    private ArrayList<DBModel> array;
    private HashMap<String,DBModel> shopList;

    private OnFragmentInteractionListener mListener;
    private CarListAdapter listAdapter;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainActivityMultiFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainActivityMultiFragment newInstance(String param1, String param2) {
        MainActivityMultiFragment fragment = new MainActivityMultiFragment();
        return fragment;
    }

    public MainActivityMultiFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        LinearLayout layout = (LinearLayout) inflater.inflate(R.layout.fragment_main_activity_multi, container, false);
        return layout;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        array=((MainActivity)getActivity()).array;
        setUp();
        listAdapter = new CarListAdapter(array);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        public void onFragmentInteraction(Uri uri);
    }


    public void setUp(){
        getGarage();
    }


    public void getGarage() {
        final LocalDataRetriever ldr = new LocalDataRetriever(getContext());
        shopList = new HashMap<>();
        HashSet<String> shops = new HashSet<>(), searchSet = new HashSet<>();
        for(DBModel car : array){
            if(car.getValue("dealership")!=null) {
                shops.add(car.getValue("dealership"));
            }
        }
        for (String dealershipID : shops){
            DBModel shop = ldr.getData("Shops","ShopID",dealershipID);
            if(shop!=null){
                shopList.put(dealershipID, shop);
            }else{
                searchSet.add(dealershipID);
            }
        }
        if(searchSet.size()>0){
            ParseQuery query = new ParseQuery("Shop");
            query.whereContainedIn("objectId",searchSet);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null){
                        for (ParseObject parseObject : objects) {
                            String currentGarage = parseObject.get("name").toString();
                            String garagePhoneNumber = parseObject.get("phoneNumber").toString();
                            String garageAddress = parseObject.get("addressText").toString();
                            Shops shop = new Shops();
                            shop.setValue("ShopID", parseObject.getObjectId());
                            shop.setValue("name", currentGarage);
                            shop.setValue("address", garageAddress);
                            shop.setValue("phoneNumber", garagePhoneNumber);
                            shop.setValue("email", parseObject.get("email").toString());
                            ldr.saveData("Shops", shop.getValues());
                            shopList.put(parseObject.getObjectId(),shop);
                        }
                    }else{
                        Toast.makeText(getContext(),e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                    onDataReady();
                }
            });
        }else{
            onDataReady();
        }
    }

    private void onDataReady() {
        if (getArguments() != null && ACTION_UPDATE_MILEAGE.equals(getArguments().getString(EXTRA_ACTION))) {
            // clear the action so it's not repeated
            getArguments().putString(EXTRA_ACTION, null);

            // find the car
            String carId = getArguments().getString(EXTRA_CAR_ID);
            Cars car = null;
            for (DBModel model : array) {
                if (model instanceof Cars && carId != null && carId.equals(model.getValue("CarID"))) {
                    car = (Cars)model;
                    break;
                }
            }

            if (car != null) {
                openCar(car, true /* update_mileage */);
            }
        }

        ((ListView) getActivity().findViewById(R.id.listView)).setAdapter(new CarsListAdapter(array, shopList));
    }

    private void openCar(Cars car, boolean updateMileage) {

        try {
            ParseApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Open Details for Car','View':'MainActivityMultiFragment'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
        if (updateMileage) {
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
        intent.putExtra("shopId", car.getValue("dealership"));
        intent.putExtra("scannerId", car.getValue("scannerId"));
        intent.putExtra("make", car.getValue("make"));
        intent.putExtra("model", car.getValue("model"));
        intent.putExtra("year", car.getValue("year"));
        intent.putExtra("baseMileage", car.getValue("baseMileage"));
        intent.putExtra("totalMileage", car.getValue("totalMileage"));
        startActivity(intent);
    }

    /*
    *
    *
    * */
    private final List<DBModel> car = new ArrayList<>();
    private void indicateCurrentCarDialog(final String deviceId) {
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

        dialog.setTitle("Select the car you are currently sitting in");
        dialog.setCancelable(false);

        dialog.setSingleChoiceItems(listAdapter, 0, new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                car.clear();
                car.add(array.get(which));
            }
        });
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick (DialogInterface dialog, int which) {
                dialog.dismiss();

                final Cars selectedCar = (Cars)car.get(0);
                final ParseQuery query = new ParseQuery("Car");
                query.whereEqualTo("VIN",selectedCar.getValue("VIN"));
                query.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done (List<ParseObject> objects, ParseException e) {
                        objects.get(0).put("scannerId", deviceId);
                        objects.get(0).saveEventually(new SaveCallback() {
                            @Override
                            public void done (ParseException e) {
                                selectedCar.setValue("scannerId", deviceId);
                                LocalDataRetriever ldr = new LocalDataRetriever(getContext());
                                HashMap<String, String> map = new HashMap<String, String>();
                                map.put("scannerId", deviceId);
                                ldr.updateData("Cars", "VIN", selectedCar.getValue("VIN"), map);
                                Toast.makeText(getContext(), "Car successfully linked", Toast.LENGTH_SHORT).show();
                                ((MainActivity)getActivity()).setCurrentConnectedCar(selectedCar);
                                ((MainActivity) getActivity()).service.getDTCs();
                            }
                        });
                    }
                });
            }
        });
        dialog.show();
    }


    /**
     * Link car to device if device is new to user, and change colors of connected cars!
     * @param deviceId
     */
    public void indicateConnected(final String deviceId) {
        boolean found = false;
        Cars noDevice = null;
        int i = 0;
        for(DBModel a : array){
            if(a.getValue("scannerId").isEmpty()){
                noDevice = (Cars) a;
            }
            if(a.getValue("scannerId").equals(deviceId)&&
                    ((ListView) getActivity().findViewById(R.id.listView)).getChildAt(i)!=null){
                found = true;
                TextView tv = (TextView) ((ListView) getActivity().findViewById(R.id.listView))
                        .getChildAt(i).findViewById(R.id.car_title);
                ((LinearLayout) ((ListView) getActivity().findViewById(R.id.listView))
                        .getChildAt(i)).findViewById(R.id.color)
                        .setBackgroundColor(getResources().getColor(R.color.evcheck));
            }
            i++;
        }
        // add device if a car has no linked device
        if(!found&&noDevice!=null) {
            indicateCurrentCarDialog(deviceId);
        }
    }

    String tag = "ConnectedCar";
    /**
     * Link car to device if device is new to user, and change colors of connected cars!
     * @param deviceId
     */
    public void linkDevice(final String deviceId) {
        Log.i(tag,"Linking device");
        int noIdCount = 0;
        int associationCount = 0;
        int carPosition = 0;
        int foundPosition = 0;
        boolean found = false;

        Cars currentCar = ((MainActivity)getActivity()).getCurrentConnectedCar();
        if(currentCar==null) {
            for(DBModel car : array) {
                if(car.getValue("scannerId").equals(deviceId)) {
                    Log.i(tag,"Car name(equal): "+car.getValue("make"));
                    associationCount++;
                } else if(car.getValue("scannerId").isEmpty()) {
                    Log.i(tag,"Car name (non)"+car.getValue("make"));
                    noIdCount++;
                }

                if(associationCount==1 && !found) {
                    found = true;
                    foundPosition = carPosition;
                }
                carPosition++;
            }

            Log.i(tag,"association count: "+associationCount+" no id count: "+noIdCount);

            /**
             * If device is associated with more than one car in the dashboard or
             * */

            if(associationCount > 1 || (noIdCount > 1) && associationCount==0 ||
                    noIdCount==array.size()) {
                indicateCurrentCarDialog(deviceId);
            } else if(associationCount==1) {
                ((MainActivity)getActivity()).setCurrentConnectedCar((Cars)array.get(foundPosition));
            }
            Log.i(tag,"found position: "+foundPosition);
        }
    }

    public class CarsListAdapter extends BaseAdapter{
        private ArrayList<DBModel> array;
        private HashMap<String,DBModel> shops;
        public CarsListAdapter(ArrayList<DBModel> cars,HashMap<String,DBModel> shops){
            array = cars;
            this.shops = shops;
        }

        @Override
        public int getCount() {
            return array.size();
        }

        @Override
        public Object getItem(int i) {
            return array.get(i);
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            LinearLayout convertview = (LinearLayout)view;
            LayoutInflater inflater = LayoutInflater.from(getContext());
            final Cars car = (Cars) array.get(i);

            int recallCount = car.getValue("numberOfRecalls") == null? 0 : Integer.valueOf(car.getValue("numberOfRecalls"));
            int serviceCount = car.getValue("numberOfServices") == null? 0 : Integer.valueOf(car.getValue("numberOfServices"));
            int totalServiceCount = recallCount + serviceCount;

            convertview = (LinearLayout)inflater.inflate(R.layout.car_list_item, null);
            RelativeLayout openCarDetialsView = (RelativeLayout)
                    convertview.findViewById(R.id.open_car_details);

            ((TextView)convertview.findViewById(R.id.name)).setText(car.getValue("make") + " " + car.getValue("model"));
            ((TextView)convertview.findViewById(R.id.desc)).setText(car.getValue("year"));
            ((TextView)convertview.findViewById(R.id.serviceCountText)).setText(String.valueOf(totalServiceCount));
            if (totalServiceCount > 0) {
                // set color to red
                (convertview.findViewById(R.id.serviceCountBackground)).setBackgroundColor(Color.rgb(203, 77, 69));
            }
            else {
                // set color to green
                (convertview.findViewById(R.id.serviceCountBackground)).setBackgroundColor(Color.rgb(93, 172, 129));
            }
            DBModel shop = shopList.get(car.getValue("dealership"));
            if(shop!=null) {
                ((TextView) convertview.findViewById(R.id.phone)).setText(shop.getValue("phoneNumber"));
                ((TextView) convertview.findViewById(R.id.message)).setText(shop.getValue("email"));
                ((TextView) convertview.findViewById(R.id.location)).setText(shop.getValue("address"));
                ((TextView) convertview.findViewById(R.id.shopName)).setText(shop.getValue("name"));
            }

            openCarDetialsView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openCar(car, false /* update_mileage */);
                }
            });

            if(shop!=null) {
				setOnclickListenersForViews(shop,convertview);
			}

            // Set connectedCar indicator
            Cars currentCar = ((MainActivity)getActivity()).getCurrentConnectedCar();
            if(currentCar!=null) {
                if(car.getValue("VIN").equals(currentCar.getValue("VIN"))) {
                    convertview.findViewById(R.id.color)
                            .setBackgroundColor(getResources().getColor(R.color.evcheck));
                }
            }
            return convertview;
        }

        private void setOnclickListenersForViews(DBModel shop, LinearLayout convertView) {

            final String garagePhoneNumber = shop.getValue("phoneNumber");
            final String garageAddress = shop.getValue("address");

            LinearLayout callGarageTextView = (LinearLayout) convertView.findViewById(R.id.dial_garage);
            callGarageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ParseApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Call Garage','View':'MainActivityMultiFragment'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + garagePhoneNumber));
                    startActivity(intent);
                }
            });                
            LinearLayout messageGarageTextView =
                    (LinearLayout) convertView.findViewById(R.id.chat_message_garage);
            messageGarageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ParseApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':'Message Garage','View':'MainActivityMultiFragment'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    User.getCurrentUser().setFirstName(ParseUser.getCurrentUser().getString("name"));
                    User.getCurrentUser().setEmail(ParseUser.getCurrentUser().getEmail());
                    ConversationActivity.show(getContext());
                }
            });                
            LinearLayout locateGarageTextView = 
                    (LinearLayout) convertView.findViewById(R.id.locate_garage);
            locateGarageTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        ParseApplication.mixpanelAPI.track("Button Clicked", new JSONObject("{'Button':Directions Garage','View':'MainActivityMultiFragment'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    String uri = String.format(Locale.ENGLISH, "http://maps.google.com/maps?daddr=%s", garageAddress);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                    startActivity(intent);
                }
            });
        }

    }

    class CarListAdapter extends BaseAdapter {
        private List<DBModel> ownedCars;

        public CarListAdapter(List<DBModel> cars) {
            ownedCars = cars;
        }

        @Override
        public int getCount () {
            return ownedCars.size();
        }

        @Override
        public Object getItem (int position) {
            return ownedCars.get(position);
        }

        @Override
        public long getItemId (int position) {
            return 0;
        }

        @Override
        public View getView (int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getActivity()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View rowView = inflater
                    .inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            Cars ownedCar = (Cars) getItem(position);

            TextView carName = (TextView) rowView.findViewById(android.R.id.text1);
            carName.setText(ownedCar.getValue("make") + " " + ownedCar.getValue("model"));
            return rowView;
        }
    }

}
