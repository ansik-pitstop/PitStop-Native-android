package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneNumberFormattingTextWatcher;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.EventBus.EventType;
import com.pitstop.EventBus.EventTypeImpl;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.GetCarsByUserIdUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.User;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity implements ILoadingActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();
    public static final EventSource EVENT_SOURCE
            = new EventSourceImpl(EventSource.SOURCE_SETTINGS);

    private MixpanelHelper mixpanelHelper;
    private boolean localUpdatePerformed = false;

    private ProgressDialog progressDialog;
    SettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        settingsFragment = new SettingsFragment();
        settingsFragment.setOnInfoUpdatedListener(new SettingsFragment.OnInfoUpdated() {
            @Override
            public void localUpdatePerformed() {
                localUpdatePerformed = true;
            }
        });
        settingsFragment.setLoadingCallback(this);
        getFragmentManager().beginTransaction().replace(android.R.id.content, settingsFragment).commit();

        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
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

        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        settingsFragment.setLoadingCallback(null);
    }

    @Override
    protected void onStart() {
        super.onStart();
        settingsFragment.setLoadingCallback(this);
    }

    @Override
    public void onBackPressed() {
        mixpanelHelper.trackButtonTapped("Back", MixpanelHelper.SETTINGS_VIEW);
        finish();
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, localUpdatePerformed);
        setResult(MainActivity.RESULT_OK, intent);
        super.finish();
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

    @Override
    public void hideLoading(@Nullable String string) {
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (string != null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(@NonNull String string) {
        progressDialog.setMessage(string);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        public interface OnInfoUpdated {
            void localUpdatePerformed();
        }

        private OnInfoUpdated listener;
        private ILoadingActivity loadingCallback;

        private GlobalApplication application;
        private LocalCarAdapter localCarAdapter;
        private LocalShopAdapter shopAdapter;
        private LocalScannerAdapter localScannerAdapter;

        private User currentUser;

        private MixpanelHelper mixpanelHelper;
        private List<Car> carList;
        private NetworkHelper networkHelper;

        private VehiclePreference currentCarVehiclePreference;

        private UseCaseComponent useCaseComponent;

        public SettingsFragment() {}

        public void setOnInfoUpdatedListener(OnInfoUpdated listener) {
            this.listener = listener;
        }

        public void setLoadingCallback(ILoadingActivity callback) {
            loadingCallback = callback;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                    .contextModule(new ContextModule(getActivity()))
                    .build();

            networkHelper = tempNetworkComponent.networkHelper();

            mixpanelHelper = new MixpanelHelper((GlobalApplication) getActivity().getApplicationContext());

            mixpanelHelper.trackViewAppeared(MixpanelHelper.SETTINGS_VIEW);


            localCarAdapter = new LocalCarAdapter(getActivity());
            shopAdapter = new LocalShopAdapter(getActivity());
            localScannerAdapter = new LocalScannerAdapter(getActivity());

            useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(getActivity().getApplicationContext()))
                    .build();

            (getPreferenceManager().findPreference("AppInfo")).setTitle(BuildConfig.VERSION_NAME);

            showLoading("Loading...");
            useCaseComponent.getCarsByUserIdUseCase().execute(new GetCarsByUserIdUseCase.Callback() {
                @Override
                public void onCarsRetrieved(List<Car> cars) {
                    carList = cars;
                    localCarAdapter.deleteAllCars();
                    localCarAdapter.storeCars(cars);
                    populateCarListPreference(cars);
                }

                @Override
                public void onError() {
                    hideLoading(null);
                    Toast.makeText(getActivity().getApplicationContext()
                            ,"Error loading cars from network",Toast.LENGTH_LONG);
                }
            });

        }

        private void populateCarListPreference(List<Car> cars) {
            final List<String> shops = new ArrayList<>();
            final List<String> shopIds = new ArrayList<>();

            showLoading("Loading...");
            networkHelper.getShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        try {
                            List<Dealership> dealers = Dealership.createDealershipList(response);
                            shopAdapter.deleteAllDealerships();
                            shopAdapter.storeDealerships(dealers);

                            for (Dealership dealership : dealers) {
                                shops.add(dealership.getName());
                                shopIds.add(String.valueOf(dealership.getId()));
                            }
                            for (int i = 0; i < cars.size(); i++) {
                                setUpCarPreference(shops,shopIds,cars.get(i));
                            }

                            hideLoading(null);
                        } catch (JSONException e) {
                            hideLoading("Error");
                            e.printStackTrace();
                        }
                    } else {
                        hideLoading("Error");
                        Log.e(TAG, "Get shops: " + requestError.getMessage());
                    }
                }
            });
        }

        //Begin AddCarActivity if add car button is pressed
        private void setupAddCarButtonListener(){
            Preference addCarButton = (Preference)getPreferenceManager().findPreference("add_car_button");
            addCarButton.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mixpanelHelper.trackButtonTapped("Add Car",MixpanelHelper.SETTINGS_VIEW);
                    Intent intent = new Intent(getActivity(), AddCarActivity.class);
                    //Don't allow user to come back to tabs without first setting a car
                    startActivityForResult(intent, MainActivity.RC_ADD_CAR);
                    return false;
                }
            });
        }

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            super.onActivityResult(requestCode, resultCode, data);

            //Check for Add car finished, and whether it happened successfully, if so updated preferences view
            if (data != null) {

                if (requestCode == MainActivity.RC_ADD_CAR) {
                    if (resultCode == AddCarActivity.ADD_CAR_SUCCESS || resultCode == AddCarActivity.ADD_CAR_NO_DEALER_SUCCESS) {
                        listener.localUpdatePerformed();

                        Car addedCar = data.getParcelableExtra(MainActivity.CAR_EXTRA);

                        //Update current car to the one that was clicked
                        for (Car c: localCarAdapter.getAllCars()){
                            c.setCurrentCar(false);
                            localCarAdapter.updateCar(c);
                        }

                        //Check if inside local adapter, if not then add it
                        addedCar.setCurrentCar(true);
                        if (localCarAdapter.getCar(addedCar.getId()) == null){
                            localCarAdapter.storeCarData(addedCar);
                        }

                        List<String> shops = new ArrayList<>();
                        List<String> shopIds = new ArrayList<>();
                        List<Dealership> dealerships = shopAdapter.getAllDealerships();
                        for (Dealership dealership : dealerships) {
                            shops.add(dealership.getName());
                            shopIds.add(String.valueOf(dealership.getId()));
                        }

                        setUpCarPreference(shops,shopIds,addedCar);

                    }

                }

            }
        }

        private void showLoading(String message){
            if (loadingCallback != null){
                loadingCallback.showLoading(message);
            }
        }

        private void hideLoading(String message){
            if (loadingCallback != null){
                loadingCallback.hideLoading(message);
            }
        }

        private void setUpCarPreference(List<String> shops, List<String> shopIds,final Car car){
            final VehiclePreference vehiclePreference = new VehiclePreference(getActivity(), car);
            vehiclePreference.setEntries(shops.toArray(new CharSequence[shops.size()]));
            vehiclePreference.setEntryValues(shopIds.toArray(new CharSequence[shopIds.size()]));
            vehiclePreference.setValue(String.valueOf(car.getShopId()));
            vehiclePreference.setDialogTitle("Choose Shop for: "
                    + car.getMake()+" "+car.getModel());

            //If is a current car add checkmark and set to current car pref. for later editing
            if (car.isCurrentCar()){

                //Update shared preferences
                PreferenceManager.getDefaultSharedPreferences(application).edit()
                        .putInt(MainDashboardFragment.pfCurrentCar, car.getId()).apply();

                vehiclePreference.setCurrentCarPreference(true);

                //if its null will get exception here, first time being set
                if (currentCarVehiclePreference != null){
                    currentCarVehiclePreference.setCurrentCarPreference(false);
                }
                currentCarVehiclePreference = vehiclePreference;
            }

            //Change current car if preference is clicked
            vehiclePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    mixpanelHelper.trackButtonTapped("CurrentCarButton",MixpanelHelper.SETTINGS_VIEW);

                    //Check if the vehicle preference is already a current, if so return
                    if (car.getId() == PreferenceManager.getDefaultSharedPreferences(application)
                            .getInt(MainDashboardFragment.pfCurrentCar,-1)){

                        return false;
                    }

                    //Get most recent version of car, since the parameter may be outdated
                    Car recentCar = localCarAdapter.getCar(car.getId());

                    //Update current car to the one that was clicked
                    for (Car c: localCarAdapter.getAllCars()){
                        c.setCurrentCar(false);
                        localCarAdapter.updateCar(c);
                    }
                    recentCar.setCurrentCar(true);
                    localCarAdapter.updateCar(recentCar);

                    //Send updateCarIssue to network
                    networkHelper.setMainCar(currentUser.getId(), car.getId(), new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null){
                                //Notify the car changed
                                PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext()).edit()
                                        .putInt(MainDashboardFragment.pfCurrentCar,car.getId());
                                EventType type = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                EventBus.getDefault()
                                        .post(new CarDataChangedEvent(type,EVENT_SOURCE));
                            }
                        }
                    });
                    listener.localUpdatePerformed();

                    //Update shared preferences
                    PreferenceManager.getDefaultSharedPreferences(application).edit()
                            .putInt(MainDashboardFragment.pfCurrentCar, car.getId()).apply();

                    //Add checkmark
                    vehiclePreference.setCurrentCarPreference(true);
                    if (currentCarVehiclePreference != null){
                        currentCarVehiclePreference.setCurrentCarPreference(false);
                    }
                    currentCarVehiclePreference = vehiclePreference;

                    return false;
                }
            });

            vehiclePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    final String shopSelected = (String) newValue;

                    // Update car in local database
                    final Car itemCar = localCarAdapter.getCar(car.getId());
                    final int shopId = Integer.parseInt(shopSelected);
                    itemCar.setShopId(shopId);
                    int result = localCarAdapter.updateCar(itemCar);

                    try {
                        mixpanelHelper.trackCustom("Button Tapped",
                                new JSONObject(String.format("{'Button':'Select Dealership', 'View':'%s', 'Make':'%s', 'Model':'%s'}",
                                        MixpanelHelper.SETTINGS_VIEW, itemCar.getMake(), itemCar.getModel())));
                    } catch (JSONException e1) {
                        e1.printStackTrace();
                    }

                    if (result != 0) {
                        // Car shop was updated
                        listener.localUpdatePerformed();
                    }

                    showLoading("Updating");
                    networkHelper.getCarsByUserId(currentUser.getId(), new RequestCallback() {
                        @Override
                        public void done(String response, RequestError requestError) {
                            if (requestError == null) {
                                networkHelper.updateCarShop(itemCar.getId(), shopId,
                                        new RequestCallback() {
                                            @Override
                                            public void done(String response, RequestError requestError) {
                                                if (requestError == null) {
                                                    hideLoading("Car dealership updated");
                                                    Log.i(TAG, "Dealership updated - carId: " + itemCar.getId() + ", dealerId: " + shopId);
                                                    Car updatedCar = localCarAdapter.getCar(itemCar.getId());
                                                    updatedCar.setShopId(shopId);
                                                    localCarAdapter.updateCar(updatedCar);
                                                    listener.localUpdatePerformed();

                                                    EventType type = new EventTypeImpl(EventType.EVENT_CAR_DEALERSHIP);
                                                    EventBus.getDefault()
                                                            .post(new CarDataChangedEvent(type,EVENT_SOURCE));
                                                } else {
                                                    hideLoading("An error occurred, please try again.");
                                                    Log.e(TAG, "Dealership updateCarIssue error: " + requestError.getError());
                                                }
                                            }
                                        });
                            } else {
                                hideLoading("An error occurred, please try again.");
                                Log.e(TAG, "Get shops: " + requestError.getMessage());
                            }
                        }
                    });
                    return true;
                }
            });
            ((PreferenceCategory) getPreferenceManager()
                    .findPreference(getString(R.string.pref_vehicles)))
                    .addPreference(vehiclePreference);

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
                    currentUser.getLastName() == null || currentUser.getLastName().equals("null") ? "" : currentUser.getLastName()));
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
                    firstNameInput.setHint("First name");

                    final EditText lastNameInput = new EditText(getActivity());
                    lastNameInput.setText(
                            currentUser.getLastName() == null || currentUser.getLastName().equals("null") ? "" : currentUser.getLastName());
                    lastNameInput.setHint("Last name");

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

            final Preference phoneNumberPreference = findPreference(getString(R.string.pref_phone_number_key));
            phoneNumberPreference.setTitle(currentUser.getPhone());
            phoneNumberPreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {

                    final EditText phoneInput = new EditText(getActivity());
                    phoneInput.setText(currentUser.getPhone());
                    phoneInput.setHint("Phone number");
                    phoneInput.setInputType(InputType.TYPE_CLASS_PHONE);
                    phoneInput.addTextChangedListener(new PhoneNumberFormattingTextWatcher());

                    final LinearLayout changePhoneLayout = new LinearLayout(getActivity());
                    changePhoneLayout.setOrientation(LinearLayout.VERTICAL);
                    changePhoneLayout.setLayoutParams(new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT
                    ));
                    changePhoneLayout.addView(phoneInput);

                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(getActivity());
                    alertDialog.setTitle("Edit phone")
                            .setView(changePhoneLayout)
                            .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String phone = phoneInput.getText().toString();
                                    updateUserPhone(phone, phoneNumberPreference);
                                }
                            })
                            .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            }).create().show();

                    return true;
                }
            });

            findPreference(getString(R.string.pref_privacy_policy)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mixpanelHelper.trackButtonTapped("Privacy Policy", MixpanelHelper.SETTINGS_VIEW);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/PrivacyPolicy.pdf")));
                    return true;
                }
            });

            //Term of use
            findPreference(getString(R.string.pref_term_of_use)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    mixpanelHelper.trackButtonTapped("Terms of Use", MixpanelHelper.SETTINGS_VIEW);
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/AppAgreement.pdf")));
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

            setupAddCarButtonListener();

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
            mixpanelHelper.trackButtonTapped("Logout", MixpanelHelper.SETTINGS_VIEW);
            Intent intent = new Intent(this.getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.i(TAG, "navigateToLogin ran");
            startActivity(intent);
        }

        private void updateUserName(final String firstName, final String lastName, final Preference namePreference) {
            mixpanelHelper.trackButtonTapped("First Name", MixpanelHelper.SETTINGS_VIEW);
            mixpanelHelper.trackButtonTapped("Last Name", MixpanelHelper.SETTINGS_VIEW);
            showLoading("Updating..");
            networkHelper.updateFirstName(application.getCurrentUserId(), firstName, lastName, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        namePreference.setTitle(String.format("%s %s", firstName, lastName));

                        hideLoading("Name successfully updated");

                        currentUser.setFirstName(firstName);
                        currentUser.setLastName(lastName);
                        application.setCurrentUser(currentUser);
                        application.modifyMixpanelSettings("$name", firstName + (lastName == null ? "" : " " + lastName));
                    } else {
                        hideLoading("An error occurred, please try again");
                    }
                }
            });
        }

        private void updateUserPhone(final String phoneNumber, final Preference phonePreference) {
            mixpanelHelper.trackButtonTapped("Phone", MixpanelHelper.SETTINGS_VIEW);
            showLoading("Updating");
            networkHelper.updateUserPhone(application.getCurrentUserId(), phoneNumber, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        phonePreference.setTitle(phoneNumber);
                        hideLoading("Phone successfully updated");

                        currentUser.setPhone(phoneNumber);
                        application.setCurrentUser(currentUser);
                        application.modifyMixpanelSettings("$phone", phoneNumber);
                    } else {
                        hideLoading("An error occurred, please try again");
                    }
                }
            });
        }

        List<Car> selectedCar = new ArrayList<>();

        private class VehiclePreference extends ListPreference {

            private Car vehicle;
            private Color defaultColor;
            private View root;
            private TextView vehicleName;
            private boolean viewCreated = false;
            private Context context;

            public VehiclePreference(Context context, Car vehicle) {
                super(context);
                this.context = context;
                this.vehicle = vehicle;
                setLayoutResource(R.layout.preference_vehicle_item);
            }

            //Set color of vehicle preference depending on if it is the current vehicle
            public void setCurrentCarPreference(boolean isCurrentCar){
                vehicle.setCurrentCar(isCurrentCar);
                if (isCurrentCar && viewCreated){
                    root.findViewById(R.id.preference_vehicle_current_icon).setVisibility(View.VISIBLE);
                }
                else if (viewCreated){
                    root.findViewById(R.id.preference_vehicle_current_icon).setVisibility(View.INVISIBLE);
                }
            }

            @Override
            protected View onCreateView(ViewGroup parent) {
                root = super.onCreateView(parent);
                vehicleName = (TextView) root.findViewById(R.id.preference_vehicle_name);
                vehicleName.setText(vehicle.getMake() + " " + vehicle.getModel());
                root.findViewById(R.id.preference_vehicle_delete_button)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                mixpanelHelper.trackButtonTapped(MixpanelHelper.DELETE_CAR, MixpanelHelper.SETTINGS_VIEW);

                                new AnimatedDialogBuilder(getContext())
                                        .setTitle("Delete car")
                                        .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                        .setMessage("Are you sure you want to delete the car from your account? " +
                                                "Your can add it back later on.")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mixpanelHelper.trackButtonTapped(MixpanelHelper.DELETE_CAR_CONFIRM, MixpanelHelper.SETTINGS_VIEW);
                                                showLoading("Deleting");
                                                networkHelper.deleteUserCar(vehicle.getId(), new RequestCallback() {
                                                    @Override
                                                    public void done(String response, RequestError requestError) {
                                                        if (requestError == null) {
                                                            listener.localUpdatePerformed();
                                                            localCarAdapter.deleteCar(vehicle);
                                                            localScannerAdapter.deleteCar(vehicle);
                                                            ((PreferenceCategory) getPreferenceManager()
                                                                    .findPreference(getString(R.string.pref_vehicles)))
                                                                    .removePreference(VehiclePreference.this);
                                                        } else {
                                                            mixpanelHelper.trackButtonTapped(MixpanelHelper.DELETE_CAR_ERROR, MixpanelHelper.SETTINGS_VIEW);
                                                            Log.e(TAG, requestError.getMessage());
                                                            hideLoading("Delete failed!");
                                                        }

                                                        //Set MainCar to the following car in the list if it exists
                                                        for (Car c: carList){
                                                            if (c.getId() == vehicle.getId()){
                                                                carList.remove(c);
                                                                break;
                                                            }
                                                        }
                                                        //Check whether new main car needs to be set
                                                        if (vehicle.isCurrentCar() && !carList.isEmpty()){
                                                            Car newMainCar = carList.get(0);
                                                            networkHelper.setMainCar(currentUser.getId(), newMainCar.getId(), new RequestCallback() {
                                                                @Override
                                                                public void done(String response, RequestError requestError) {
                                                                    if (requestError == null){

                                                                        hideLoading("Car deleted");
                                                                        EventType type = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                                                        EventBus.getDefault()
                                                                                .post(new CarDataChangedEvent(type,EVENT_SOURCE));
                                                                    }
                                                                    else{
                                                                        hideLoading("Delete failed!");
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else if (carList.isEmpty()){
                                                            networkHelper.setNoMainCar(currentUser.getId(), new RequestCallback() {
                                                                @Override
                                                                public void done(String response, RequestError requestError) {
                                                                    if (requestError == null){
                                                                        EventType type = new EventTypeImpl(EventType.EVENT_CAR_ID);
                                                                        EventBus.getDefault()
                                                                                .post(new CarDataChangedEvent(type,EVENT_SOURCE));
                                                                        hideLoading("Car deleted");

                                                                    }
                                                                    else{
                                                                        hideLoading("Delete failed!");
                                                                    }
                                                                }
                                                            });
                                                        }
                                                        else{
                                                            hideLoading("Car deleted");
                                                        }
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                mixpanelHelper.trackButtonTapped(MixpanelHelper.DELETE_CAR_CANCEL, MixpanelHelper.SETTINGS_VIEW);
                                            }
                                        }).show();
                            }
                        });
                root.findViewById(R.id.preference_vehicle_modify_button)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showDialog(null);
                            }
                        });

                viewCreated = true;
                setCurrentCarPreference(vehicle.isCurrentCar());

                return root;
            }

            @Override
            protected View onCreateDialogView() {
                return super.onCreateDialogView();
            }

            @Override
            public void setDialogTitle(CharSequence dialogTitle) {
                super.setDialogTitle(dialogTitle);
            }

            @Override
            protected void onClick() {
                // super.onClick();
                // Do nothing :D
            }
        }

    }

}