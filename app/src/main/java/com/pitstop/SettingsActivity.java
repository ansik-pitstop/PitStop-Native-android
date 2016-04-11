package com.pitstop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.IntentProxyObject;
import com.pitstop.parse.ParseApplication;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private ArrayList<String> cars = new ArrayList<>();
    private ArrayList<String> ids = new ArrayList<>();
    private ArrayList<String> dealers = new ArrayList<>();

    private Car dashboardCar;
    private boolean updatePerformed = false;

    private List<Car> carList = new ArrayList<>();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings);

        if(getIntent().getExtras()!=null) {
            populateCarNamesAndIdList();
        }

        SettingsFragment settingsFragment =  new SettingsFragment();
        settingsFragment.setOnInfoUpdatedListener(new SettingsFragment.OnInfoUpdated() {
            @Override
            public void updatePerformed() {
                updatePerformed = true;
            }
        });

        IntentProxyObject carListProxyObject = (IntentProxyObject) getIntent()
                .getSerializableExtra(MainActivity.CAR_LIST_EXTRA);

        Bundle bundle = new Bundle();
        bundle.putStringArrayList("cars", cars);
        bundle.putStringArrayList("ids", ids);
        bundle.putStringArrayList("dealers",dealers);
        bundle.putSerializable("mainCar",dashboardCar);
        bundle.putSerializable("carList", carListProxyObject);

        settingsFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment ).commit();
    }

    private void populateCarNamesAndIdList() {
        IntentProxyObject proxyObject = (IntentProxyObject) getIntent()
                .getSerializableExtra(MainActivity.CAR_LIST_EXTRA);
        carList = proxyObject.getCarList();

        for(Car car : carList) {
            if(car.isCurrentCar()) {
                dashboardCar = car;
            }

            cars.add(car.getMake() + " " + car.getModel());
            ids.add(car.getParseId());
            dealers.add(car.getShopId());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_connect) {
            Intent i = new Intent(SettingsActivity.this, ReceiveDebugActivity.class);
            startActivity(i);
            return true;
        }


        if (id ==  android.R.id.home){
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public void onBackPressed() {

        finish();
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_LOCAL, updatePerformed);
        setResult(MainActivity.RESULT_OK,intent);
        super.finish();
    }

    public static class SettingsFragment extends PreferenceFragment {

        public interface OnInfoUpdated {
            public void updatePerformed();
        }

        private OnInfoUpdated listener;
        private ArrayList<ListPreference> preferenceList;
        private ArrayList<String> cars;
        private ArrayList<String> ids;
        private ArrayList<String> dealers;

        private List<Car> carList;
        private CarListAdapter listAdapter;

        private ParseApplication application;
        private Car mainCar;

        public SettingsFragment() {}

        public void setOnInfoUpdatedListener (OnInfoUpdated listener) {
            this.listener = listener;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            Bundle bundle = getArguments();
            preferenceList = new ArrayList<>();
            cars = bundle.getStringArrayList("cars");
            ids = bundle.getStringArrayList("ids");
            dealers = bundle.getStringArrayList("dealers");
            mainCar = (Car) bundle.getSerializable("mainCar");

            IntentProxyObject listObject = (IntentProxyObject) bundle.getSerializable("carList");
            if(listObject != null) {
                carList = listObject.getCarList();
            }

            listAdapter = new CarListAdapter(carList);

            (getPreferenceManager()
                    .findPreference("AppInfo")).setTitle(getString(R.string.app_build_no));

            if(mainCar != null) {
                final Preference mainCarPreference = findPreference("current_car");
                mainCarPreference.setSummary("Tap to switch car");
                mainCarPreference.setTitle(mainCar.getMake() +" "+ mainCar.getModel());
                mainCarPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if(listAdapter.getCount() == 1) {
                            Toast.makeText(((SettingsActivity)getActivity()).getApplicationContext(),
                                    "You have only one added vehicle.", Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        switchCarDialog(mainCar, mainCarPreference);
                        return true;
                    }
                });
            }

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Shop");
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if (e != null) {
                        return;
                    }
                    ArrayList<String> shops = new ArrayList<String>(), shopIds = new ArrayList<String>();
                    for (ParseObject object : objects) {
                        shops.add(object.getString("name"));
                        shopIds.add(object.getObjectId());
                    }

                    for (int i = 0; i < cars.size(); i++) {
                        ListPreference listPreference = new ListPreference(getActivity());
                        listPreference.setTitle(cars.get(i));
                        listPreference.setEntries(shops.toArray(new CharSequence[shops.size()]));
                        listPreference.setEntryValues(shopIds.toArray(new CharSequence[shopIds.size()]));
                        listPreference.setValue(dealers.get(i));
                        listPreference.setDialogTitle("Choose Shop for: " + cars.get(i));
                        final int index = i;
                        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                final String shopSelected = (String) newValue;
                                /*LocalDataRetriever ldr = new LocalDataRetriever(getActivity());
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("dealership", shopSelected);
                                ldr.updateData("Cars", "CarID", ids.get(index), hm);*/
                                //updateParse
                                ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
                                query.getInBackground(ids.get(index), new GetCallback<ParseObject>() {
                                    public void done(ParseObject car, ParseException e) {
                                        if (e == null) {
                                            // Now let's update it with some new data. In this case, only cheatMode and score
                                            // will get sent to the Parse Cloud. playerName hasn't changed.
                                            car.put("dealership", shopSelected);
                                            car.saveInBackground(new SaveCallback() {
                                                @Override
                                                public void done(ParseException e) {
                                                    if(e == null) {
                                                        listener.updatePerformed();
                                                    }
                                                }
                                            });
                                        }
                                    }
                                });
                                return true;
                            }
                        });
                        ((PreferenceCategory) getPreferenceManager()
                                .findPreference(getString(R.string.pref_vehicles)))
                                .addPreference(listPreference);
                    }
                }
            });
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            application = (ParseApplication) getActivity().getApplicationContext();

            addPreferencesFromResource(R.xml.preferences);
            final Preference namePreference = findPreference(getString(R.string.pref_username_key));
            namePreference.setTitle(ParseUser.getCurrentUser().getString("name"));
            namePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

                    alertDialog.setTitle("Edit name");
                    final EditText nameInput = new EditText(getActivity());
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    nameInput.setLayoutParams(lp);
                    alertDialog.setView(nameInput);

                    alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String updatedName = nameInput.getText().toString();
                            updateUsersName(updatedName,namePreference);
                        }
                    });

                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    alertDialog.show();
                    return true;
                }
            });

            Preference emailPreference = findPreference(getString(R.string.pref_email_key));
            emailPreference.setTitle(ParseUser.getCurrentUser().getEmail());

            Preference phoneNumberPreference = findPreference(getString(R.string.pref_phone_number_key));
            phoneNumberPreference.setTitle(ParseUser.getCurrentUser().getString("phoneNumber"));


            //logging out
            Preference logoutPref = findPreference(getString(R.string.pref_logout_key));
            logoutPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

                    // Setting Dialog Title
                    alertDialog.setTitle("Confirm Logout");

                    // Setting Dialog Message
                    alertDialog.setMessage("Are you sure you want to logout?");

                    // Setting Positive "Yes" Button
                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Write your code here to invoke YES event
                            //Toast.makeText(getActivity().getApplication(), "You clicked on YES", Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            ParseUser.logOut();
                            navigateToLogin();
                        }
                    });

                    // Setting Negative "NO" Button
                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // Write your code here to invoke NO event
                            //Toast.makeText(getActivity().getApplication(), "You clicked on NO", Toast.LENGTH_SHORT).show();
                            dialog.cancel();
                        }
                    });

                    // Showing Alert Message
                    alertDialog.show();
                    return true;
                }
            });

            try {
                application.getMixpanelAPI().track("View Appeared",
                        new JSONObject("{'View':'SettingsActivity'}"));
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        @Override
        public void onPause() {
            application.getMixpanelAPI().flush();
            super.onPause();
        }

        private void navigateToLogin() {
            Intent intent = new Intent(this.getActivity(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.i(TAG, "navigateToLogin ran");
            startActivity(intent);
        }

        private void updateUsersName(final String updatedName, final Preference namePreference) {
            ParseUser currentUser = ParseUser.getCurrentUser();
            currentUser.put("name", updatedName);
            currentUser.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        namePreference.setTitle(updatedName);
                        listener.updatePerformed();
                        Toast.makeText(getActivity(), "Name successfully updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        List<Car> selectedCar = new ArrayList<>();
        private void switchCarDialog(final Car formerDashboardCar, final Preference mainPreference) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity());

            dialog.setTitle("Switch Car");

            dialog.setSingleChoiceItems(listAdapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    selectedCar.clear();
                    //selectedCar.add(carList.get(which));
                    selectedCar.add((Car) listAdapter.getItem(which));
                }
            });
            dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();

                    if (selectedCar.isEmpty()) {
                        return;
                    }

                    final Car newDashboardCar = selectedCar.get(0);
                    final ParseQuery query = new ParseQuery("Car");
                    query.whereEqualTo("VIN", newDashboardCar.getVin());
                    query.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> objects, ParseException e) {
                            if (e == null) {
                                objects.get(0).put("currentCar", true);
                                objects.get(0).saveEventually(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        mainCar = newDashboardCar;
                                        mainPreference.setTitle(mainCar.getMake()
                                                +" "+mainCar.getModel());
                                        listener.updatePerformed();
                                    }
                                });

                                //update the car object
                                ParseQuery<ParseObject> cars = ParseQuery.getQuery("Car");
                                ParseObject car = null;
                                try {
                                    car = cars.get(formerDashboardCar.getParseId());
                                    car.put("currentCar", false);
                                    car.saveEventually();
                                } catch (ParseException error) {
                                    error.printStackTrace();
                                }
                            } else {
                                Log.i(TAG,e.getMessage());
                            }
                        }
                    });
                }
            });
            dialog.show();
        }

        class CarListAdapter extends BaseAdapter {
            private List<Car> ownedCars;

            public CarListAdapter(List<Car> cars) {
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
                Car ownedCar = (Car) getItem(position);

                TextView carName = (TextView) rowView.findViewById(android.R.id.text1);
                carName.setText(ownedCar.getMake() + " " + ownedCar.getModel());
                return rowView;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
}