package com.pitstop;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
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

import com.pitstop.DataAccessLayer.DTOs.Car;
import com.pitstop.DataAccessLayer.DTOs.Dealership;
import com.pitstop.DataAccessLayer.DTOs.IntentProxyObject;
import com.pitstop.DataAccessLayer.DTOs.User;
import com.pitstop.DataAccessLayer.DataAdapters.LocalCarAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.LocalShopAdapter;
import com.pitstop.DataAccessLayer.DataAdapters.UserAdapter;
import com.pitstop.DataAccessLayer.ServerAccess.RequestCallback;
import com.pitstop.DataAccessLayer.ServerAccess.RequestError;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private ArrayList<String> cars = new ArrayList<>();
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> dealers = new ArrayList<>();

    private Car dashboardCar;
    private boolean localUpdatePerformed = false;
    private LocalCarAdapter localCarAdapter;
    private List<Car> carList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        localCarAdapter = new LocalCarAdapter(this);
        populateCarNamesAndIdList();

        SettingsFragment settingsFragment =  new SettingsFragment();
        settingsFragment.setOnInfoUpdatedListener(new SettingsFragment.OnInfoUpdated() {
            @Override
            public void localUpdatePerformed() {
                localUpdatePerformed = true;
            }
        });

        Bundle bundle = new Bundle();
        bundle.putStringArrayList("cars", cars);
        bundle.putIntegerArrayList("ids", ids);
        bundle.putStringArrayList("dealers",dealers);
        bundle.putSerializable("mainCar",dashboardCar);

        IntentProxyObject intentProxyObject = new IntentProxyObject();
        intentProxyObject.setCarList(carList);
        bundle.putSerializable("carList", intentProxyObject);

        settingsFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment ).commit();
    }

    private void populateCarNamesAndIdList() {
        carList = localCarAdapter.getAllCars();

        for(Car car : carList) {
            if(car.getId() == PreferenceManager.getDefaultSharedPreferences(this).getInt(MainActivity.pfCurrentCar, -1)) {
                dashboardCar = car;
            }

            cars.add(car.getMake() + " " + car.getModel());
            ids.add(car.getId());
            dealers.add(String.valueOf(car.getShopId()));
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
        try {
            new MixpanelHelper((GlobalApplication) getApplicationContext()).trackButtonTapped("Back", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, localUpdatePerformed);
        setResult(MainActivity.RESULT_OK,intent);
        super.finish();
    }

    public static class SettingsFragment extends PreferenceFragment {

        public interface OnInfoUpdated {
            void localUpdatePerformed();
        }

        private OnInfoUpdated listener;
        private ArrayList<ListPreference> preferenceList;
        private ArrayList<String> cars;
        private ArrayList<Integer> ids;
        private ArrayList<String> dealers;

        private List<Car> carList;
        private CarListAdapter listAdapter;

        private GlobalApplication application;
        private Car mainCar;
        private LocalCarAdapter localCarAdapter;
        private LocalShopAdapter shopAdapter;

        private User currentUser;

        private MixpanelHelper mixpanelHelper;

        public SettingsFragment() {}

        public void setOnInfoUpdatedListener (OnInfoUpdated listener) {
            this.listener = listener;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            mixpanelHelper = new MixpanelHelper((GlobalApplication) getActivity().getApplicationContext());

            try {
                mixpanelHelper.trackViewAppeared(TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            localCarAdapter = new LocalCarAdapter(getActivity().getApplicationContext());
            shopAdapter = new LocalShopAdapter(getActivity().getApplicationContext());

            Bundle bundle = getArguments();
            cars = bundle.getStringArrayList("cars");
            ids = bundle.getIntegerArrayList("ids");
            dealers = bundle.getStringArrayList("dealers");
            mainCar = (Car) bundle.getSerializable("mainCar");

            IntentProxyObject listObject = (IntentProxyObject) bundle.getSerializable("carList");
            if(listObject != null) {
                carList = listObject.getCarList();
            } else {
                carList = localCarAdapter.getAllCars();
            }

            listAdapter = new CarListAdapter(carList);

            try {
                (getPreferenceManager()
                        .findPreference("AppInfo"))
                        .setTitle(getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
            } catch(Exception e) {
                (getPreferenceManager().findPreference("AppInfo")).setTitle(getString(R.string.app_build_no));
            }

            if(mainCar != null) {
                final Preference mainCarPreference = findPreference("current_car");
                mainCarPreference.setSummary("Tap to switch car");
                mainCarPreference.setTitle(mainCar.getMake() +" "+ mainCar.getModel());
                mainCarPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        if(listAdapter.getCount() == 1) {
                            Toast.makeText((getActivity()).getApplicationContext(),
                                    "You have only one added vehicle.", Toast.LENGTH_SHORT).show();
                            return true;
                        }

                        switchCarDialog(mainCar, mainCarPreference);
                        return true;
                    }
                });
            }

            List<Dealership> dealerships = shopAdapter.getAllDealerships();
            final List<String> shops = new ArrayList<>();
            final List<String> shopIds = new ArrayList<>();

            // Try local store for dealerships
            if(dealerships.isEmpty()) {
                Log.i(TAG, "Local store has no dealerships");

                NetworkHelper.getShops(new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if(requestError == null) {
                            try {
                                shopAdapter.deleteAllDealerships();
                                shopAdapter.storeDealerships(Dealership.createDealershipList(response));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(getActivity(), "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Log.e(TAG, "Get shops: " + requestError.getMessage());
                            Toast.makeText(getActivity(), "An error occured, please try again", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                for (Dealership shop : dealerships) {
                    shops.add(shop.getName());
                    shopIds.add(String.valueOf(shop.getId()));
                }

                setUpCarListPreference(shops, shopIds);
            }

        }

        private void setUpCarListPreference(List<String> shops, List<String> shopIds) {
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
                    public boolean onPreferenceChange(Preference preference, final Object newValue) {
                        final String shopSelected = (String) newValue;

                        // Update car in local database
                        final Car itemCar = localCarAdapter.getCar(ids.get(index));
                        final int shopId = Integer.parseInt(shopSelected);
                        itemCar.setShopId(shopId);
                        int result = localCarAdapter.updateCar(itemCar);

                        try {
                            ((GlobalApplication) getActivity().getApplicationContext()).getMixpanelAPI().track("Button Tapped",
                                    new JSONObject(String.format("{'Button':'Select Car', 'View':'%s', 'Device':'Android', 'Make':'%s', 'Model':'%s'}",
                                            TAG, itemCar.getMake(), itemCar.getModel())));
                        } catch (JSONException e1) {
                            e1.printStackTrace();
                        }

                        if(result != 0) {
                            // Car shop was updated
                            listener.localUpdatePerformed();
                        }

                        NetworkHelper.getCarsByUserId(currentUser.getId(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if(requestError == null) {
                                    NetworkHelper.updateCarShop(itemCar.getId(), shopId,
                                            new RequestCallback() {
                                                @Override
                                                public void done(String response, RequestError requestError) {
                                                    if(requestError == null) {
                                                        Log.i(TAG, "Dealership updated - carId: " + itemCar.getId() + ", dealerId: " + shopId);
                                                        Toast.makeText(getActivity(), "Car dealership updated", Toast.LENGTH_SHORT).show();
                                                        listener.localUpdatePerformed();
                                                    } else {
                                                        Log.e(TAG, "Dealership update error: " + requestError.getError());
                                                        Toast.makeText(getActivity(), "There was an error, please try again", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                } else {
                                    Log.e(TAG, "Get shops: " + requestError.getMessage());
                                    Toast.makeText(getActivity(), "An error occured, please try again", Toast.LENGTH_SHORT).show();
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

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            application = (GlobalApplication) getActivity().getApplicationContext();

            currentUser = application.getCurrentUser();

            addPreferencesFromResource(R.xml.preferences);
            final Preference namePreference = findPreference(getString(R.string.pref_username_key));
            namePreference.setTitle(String.format("%s %s",
                    currentUser.getFirstName(),
                    currentUser.getLastName() == null ? "" : currentUser.getLastName()));
            namePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());

                    alertDialog.setTitle("Edit name");
                    final LinearLayout changeNameLayout = new LinearLayout(getActivity());
                    changeNameLayout.setOrientation(LinearLayout.VERTICAL);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.MATCH_PARENT
                    );
                    changeNameLayout.setLayoutParams(lp);

                    final EditText firstNameInput = new EditText(getActivity());
                    firstNameInput.setText(currentUser.getFirstName());

                    final EditText lastNameInput = new EditText(getActivity());
                    lastNameInput.setText(currentUser.getLastName());

                    changeNameLayout.addView(firstNameInput);
                    changeNameLayout.addView(lastNameInput);

                    alertDialog.setView(changeNameLayout);

                    alertDialog.setPositiveButton("Save", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String firstName = firstNameInput.getText().toString();
                            String lastName = lastNameInput.getText().toString();
                            updateUserName(firstName, lastName, namePreference);
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
            emailPreference.setTitle(currentUser.getEmail());

            Preference phoneNumberPreference = findPreference(getString(R.string.pref_phone_number_key));
            phoneNumberPreference.setTitle(currentUser.getPhone());

            findPreference(getString(R.string.pref_privacy_policy)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        mixpanelHelper.trackButtonTapped("Privacy Policy", TAG);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/PrivacyPolicy.pdf")));
                    return true;
                }
            });


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
                            application.logOutUser();
                            navigateToLogin();
                        }
                    });

                    // Setting Negative "NO" Button
                    alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    // Showing Alert Message
                    alertDialog.show();
                    return true;
                }
            });

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
            try {
                mixpanelHelper.trackButtonTapped("Logout", TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(this.getActivity(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.i(TAG, "navigateToLogin ran");
            startActivity(intent);
        }

        private void updateUserName(final String firstName, final String lastName, final Preference namePreference) {
            try {
                mixpanelHelper.trackButtonTapped("Name", TAG);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

            NetworkHelper.updateFirstName(application.getCurrentUserId(), firstName, lastName, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        namePreference.setTitle(String.format("%s %s", firstName, lastName));

                        Toast.makeText(getActivity(), "Name successfully updated", Toast.LENGTH_SHORT).show();

                        currentUser.setFirstName(firstName);
                        currentUser.setLastName(lastName);
                        application.setCurrentUser(currentUser);
                    } else {
                        Toast.makeText(getActivity(), "An error occurred, please try again", Toast.LENGTH_SHORT).show();
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

                    if(formerDashboardCar.getVin().equals(newDashboardCar.getVin())) {
                        return;
                    }

                    mainCar = newDashboardCar;
                    mainPreference.setTitle(mainCar.getMake() + " " + mainCar.getModel());
                    listener.localUpdatePerformed();

                    try {
                        ((GlobalApplication) getActivity().getApplicationContext()).getMixpanelAPI().track("Button Tapped",
                                new JSONObject(String.format("{'Button':'Select Car', 'View':'%s', 'Device':'Android', 'Make':'%s', 'Model':'%s'}",
                                        TAG, newDashboardCar.getMake(), newDashboardCar.getModel())));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putInt(MainActivity.pfCurrentCar, newDashboardCar.getId()).apply();

                }
            });
            dialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            dialog.show();
        }

        private class CarListAdapter extends BaseAdapter {
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
                carName.setText(String.format("%s %s", ownedCar.getMake(), ownedCar.getModel()));
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