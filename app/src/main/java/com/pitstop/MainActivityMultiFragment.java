package com.pitstop;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Shops;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


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
                    ((ListView) getActivity().findViewById(R.id.listView)).setAdapter(new CarsListAdapter(array,shopList));
                }
            });
        }else{
            ((ListView) getActivity().findViewById(R.id.listView)).setAdapter(new CarsListAdapter(array, shopList));
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
            convertview = (LinearLayout)inflater.inflate(R.layout.car_list_item, null);

            ((TextView)convertview.findViewById(R.id.name)).setText(car.getValue("make") + " " + car.getValue("model"));
            ((TextView)convertview.findViewById(R.id.desc)).setText(car.getValue("year"));
            DBModel shop = shopList.get(car.getValue("dealership"));
            if(shop!=null) {
                ((TextView) convertview.findViewById(R.id.phone)).setText(shop.getValue("phoneNumber"));
                ((TextView) convertview.findViewById(R.id.message)).setText(shop.getValue("email"));
                ((TextView) convertview.findViewById(R.id.location)).setText(shop.getValue("address"));
                ((TextView) convertview.findViewById(R.id.shopName)).setText(shop.getValue("name"));
            }

            convertview.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getActivity(), CarDetailsActivity.class);
                    intent.putExtra("title", car.getValue("make") + " " + car.getValue("model"));
                    String temp = car.getValue("services");
                    if (!temp.equals("")) {
                        intent.putExtra("servicesDue", temp.substring(1, temp.length() - 1).split(","));
                    } else {
                        intent.putExtra("servicesDue", new String[]{});
                    }
                    temp = car.getValue("recalls");
                    if (!temp.equals("")) {
                        intent.putExtra("pendingRecalls", temp.substring(1, temp.length() - 1).split(","));
                    } else {
                        intent.putExtra("pendingRecalls", new String[]{});
                    }

                    temp = car.getValue("dtcs");
                    if (!temp.equals("")) {
                        intent.putExtra("dtcs", temp.substring(1, temp.length() - 1).split(","));
                    } else {
                        intent.putExtra("dtcs", new String[]{});
                    }
                    intent.putExtra("vin", car.getValue("VIN"));
                    intent.putExtra("scannerId", car.getValue("scannerId"));
                    intent.putExtra("make", car.getValue("make"));
                    intent.putExtra("model", car.getValue("model"));
                    intent.putExtra("year", car.getValue("year"));
                    startActivity(intent);
                }
            });
            return convertview;
        }
    }

}
