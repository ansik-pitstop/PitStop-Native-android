package com.pitstop.ui;

import android.app.AlertDialog;
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
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
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
import com.pitstop.R;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.IntentProxyObject;
import com.pitstop.models.User;
import com.pitstop.database.LocalCarAdapter;
import com.pitstop.database.LocalShopAdapter;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.ui.mainFragments.MainDashboardFragment;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity{

    public static final String TAG = SettingsActivity.class.getSimpleName();

    private ArrayList<String> cars = new ArrayList<>();
    private ArrayList<Integer> ids = new ArrayList<>();
    private ArrayList<String> dealers = new ArrayList<>();

    private MixpanelHelper mixpanelHelper;

    private Car dashboardCar;
    private boolean localUpdatePerformed = false;
    private LocalCarAdapter localCarAdapter;
    private List<Car> carList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());

        localCarAdapter = new LocalCarAdapter(this);
        populateCarNamesAndIdList();

        SettingsFragment settingsFragment = new SettingsFragment();
        settingsFragment.setOnInfoUpdatedListener(new SettingsFragment.OnInfoUpdated() {
            @Override
            public void localUpdatePerformed() {
                localUpdatePerformed = true;
            }
        });

        Bundle bundle = new Bundle();
        bundle.putStringArrayList("cars", cars);
        bundle.putIntegerArrayList("ids", ids);
        bundle.putStringArrayList("dealers", dealers);
        bundle.putParcelable("mainCar", dashboardCar);

        IntentProxyObject intentProxyObject = new IntentProxyObject();
        intentProxyObject.setCarList(carList);
        bundle.putParcelable("carList", intentProxyObject);

        settingsFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, settingsFragment).commit();
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
    public void onBackPressed() {
        try {
            mixpanelHelper.trackButtonTapped("Back", MixpanelHelper.SETTINGS_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        finish();
    }

    @Override
    public void finish() {
        Intent intent = new Intent();
        intent.putExtra(MainActivity.REFRESH_FROM_SERVER, localUpdatePerformed);
        setResult(MainActivity.RESULT_OK, intent);
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
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

    private void populateCarNamesAndIdList() {
        carList = localCarAdapter.getAllCars();

        for (Car car : carList) {
            if (car.getId() == PreferenceManager.getDefaultSharedPreferences(this).getInt(MainDashboardFragment.pfCurrentCar, -1)) {
                dashboardCar = car;
            }

            cars.add(car.getMake() + " " + car.getModel());
            ids.add(car.getId());
            dealers.add(String.valueOf(car.getShopId()));
        }
    }

    /**
     * Create and show an snackbar that is used to show users some information.<br>
     * The purpose of this method is to display message that requires user's confirm to be dismissed.
     *
     * @param content snack bar message
     */
    public void showSimpleMessage(@NonNull String content, boolean isSuccess) {
        Snackbar snackbar = Snackbar.make(findViewById(android.R.id.content), content, Snackbar.LENGTH_LONG)
                .setAction("OK", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // DO nothing
                    }
                })
                .setActionTextColor(Color.WHITE);
        View snackBarView = snackbar.getView();
        if (isSuccess) {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_success));
        } else {
            snackBarView.setBackgroundColor(ContextCompat.getColor(this, R.color.message_failure));
        }
        TextView textView = (TextView) snackBarView.findViewById(android.support.design.R.id.snackbar_text);
        textView.setTextColor(ContextCompat.getColor(this, R.color.white_text));

        snackbar.show();

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

        private GlobalApplication application;
        private LocalCarAdapter localCarAdapter;
        private LocalShopAdapter shopAdapter;

        private User currentUser;

        private MixpanelHelper mixpanelHelper;

        private NetworkHelper networkHelper;

        public SettingsFragment() {
        }

        public void setOnInfoUpdatedListener(OnInfoUpdated listener) {
            this.listener = listener;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            networkHelper = new NetworkHelper(getActivity().getApplicationContext());

            mixpanelHelper = new MixpanelHelper((GlobalApplication) getActivity().getApplicationContext());

            try {
                mixpanelHelper.trackViewAppeared(MixpanelHelper.SETTINGS_VIEW);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            localCarAdapter = new LocalCarAdapter(getActivity());
            shopAdapter = new LocalShopAdapter(getActivity());

            Bundle bundle = getArguments();
            cars = bundle.getStringArrayList("cars");
            ids = bundle.getIntegerArrayList("ids");
            dealers = bundle.getStringArrayList("dealers");

            IntentProxyObject listObject = bundle.getParcelable("carList");
            if (listObject != null) {
                carList = listObject.getCarList();
            } else {
                carList = localCarAdapter.getAllCars();
            }

            (getPreferenceManager().findPreference("AppInfo")).setTitle(BuildConfig.VERSION_NAME);

            populateCarListPreference();
        }

        private void populateCarListPreference() {
            final List<Dealership> dealerships = shopAdapter.getAllDealerships();
            final List<String> shops = new ArrayList<>();
            final List<String> shopIds = new ArrayList<>();

            // Try local store for dealerships
            if (dealerships.isEmpty()) {
                Log.i(TAG, "Local store has no dealerships");

                networkHelper.getShops(new RequestCallback() {
                    @Override
                    public void done(String response, RequestError requestError) {
                        if (requestError == null) {
                            try {
                                List<Dealership> dealers = Dealership.createDealershipList(response);
                                shopAdapter.deleteAllDealerships();
                                shopAdapter.storeDealerships(dealers);

                                // Test this:
                                for (Dealership dealership : dealers) {
                                    shops.add(dealership.getName());
                                    shopIds.add(String.valueOf(dealership.getId()));
                                }
                                setUpCarListPreference(shops, shopIds);

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
                VehiclePreference vehiclePreference = new VehiclePreference(getActivity(), carList.get(i));
                vehiclePreference.setEntries(shops.toArray(new CharSequence[shops.size()]));
                vehiclePreference.setEntryValues(shopIds.toArray(new CharSequence[shopIds.size()]));
                vehiclePreference.setValue(dealers.get(i));
                vehiclePreference.setDialogTitle("Choose Shop for: " + cars.get(i));
                final int index = i;
                vehiclePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        final String shopSelected = (String) newValue;

                        // Update car in local database
                        final Car itemCar = localCarAdapter.getCar(ids.get(index));
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

                        networkHelper.getCarsByUserId(currentUser.getId(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if (requestError == null) {
                                    networkHelper.updateCarShop(itemCar.getId(), shopId,
                                            new RequestCallback() {
                                                @Override
                                                public void done(String response, RequestError requestError) {
                                                    if (requestError == null) {
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
                        .addPreference(vehiclePreference);
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
                    try {
                        mixpanelHelper.trackButtonTapped("Privacy Policy", MixpanelHelper.SETTINGS_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://getpitstop.io/privacypolicy/PrivacyPolicy.pdf")));
                    return true;
                }
            });

            //Term of use
            findPreference(getString(R.string.pref_term_of_use)).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    try {
                        mixpanelHelper.trackButtonTapped("Terms of Use", MixpanelHelper.SETTINGS_VIEW);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
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
                mixpanelHelper.trackButtonTapped("Logout", MixpanelHelper.SETTINGS_VIEW);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent(this.getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.i(TAG, "navigateToLogin ran");
            startActivity(intent);
        }

        private void updateUserName(final String firstName, final String lastName, final Preference namePreference) {
            try {
                mixpanelHelper.trackButtonTapped("First Name", MixpanelHelper.SETTINGS_VIEW);
                mixpanelHelper.trackButtonTapped("Last Name", MixpanelHelper.SETTINGS_VIEW);
            } catch (JSONException e1) {
                e1.printStackTrace();
            }

            networkHelper.updateFirstName(application.getCurrentUserId(), firstName, lastName, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        namePreference.setTitle(String.format("%s %s", firstName, lastName));

                        Toast.makeText(getActivity(), "Name successfully updated", Toast.LENGTH_SHORT).show();

                        currentUser.setFirstName(firstName);
                        currentUser.setLastName(lastName);
                        application.setCurrentUser(currentUser);
                        application.modifyMixpanelSettings("$name", firstName + (lastName == null ? "" : " " + lastName));

                    } else {
                        Toast.makeText(getActivity(), "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        private void updateUserPhone(final String phoneNumber, final Preference phonePreference) {
            try {
                mixpanelHelper.trackButtonTapped("Phone", MixpanelHelper.SETTINGS_VIEW);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            networkHelper.updateUserPhone(application.getCurrentUserId(), phoneNumber, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if (requestError == null) {
                        phonePreference.setTitle(phoneNumber);

                        Toast.makeText(getActivity(), "Phone successfully updated", Toast.LENGTH_SHORT).show();

                        currentUser.setPhone(phoneNumber);
                        application.setCurrentUser(currentUser);
                        application.modifyMixpanelSettings("$phone", phoneNumber);

                    } else {
                        Toast.makeText(getActivity(), "An error occurred, please try again", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        List<Car> selectedCar = new ArrayList<>();

        private class VehiclePreference extends ListPreference {

            private Car vehicle;
            private View root;
            private TextView vehicleName;

            public VehiclePreference(Context context, Car vehicle) {
                super(context);
                this.vehicle = vehicle;
                setLayoutResource(R.layout.preference_vehicle_item);
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
                                new AnimatedDialogBuilder(getContext())
                                        .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                                        .setTitle("Delete car")
                                        .setMessage("Are you sure you want to delete the car from your account? " +
                                                "Your can add it back later on.")
                                        .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                networkHelper.deleteUserCar(vehicle.getId(), new RequestCallback() {
                                                    @Override
                                                    public void done(String response, RequestError requestError) {
                                                        if (requestError == null) {
                                                            ((SettingsActivity) getActivity())
                                                                    .showSimpleMessage("Delete successfully!", true);
                                                            listener.localUpdatePerformed();
                                                            localCarAdapter.deleteCar(vehicle);
                                                            ((PreferenceCategory) getPreferenceManager()
                                                                    .findPreference(getString(R.string.pref_vehicles)))
                                                                    .removePreference(VehiclePreference.this);
                                                        } else {
                                                            Log.e(TAG, requestError.getMessage());
                                                            ((SettingsActivity) getActivity())
                                                                    .showSimpleMessage("Delete failed, please retry later.", false);
                                                        }
                                                    }
                                                });
                                            }
                                        })
                                        .setNegativeButton("CANCEL", null).show();
                            }
                        });
                root.findViewById(R.id.preference_vehicle_modify_button)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showDialog(null);
                            }
                        });
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