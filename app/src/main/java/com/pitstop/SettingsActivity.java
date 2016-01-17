package com.pitstop;

import android.app.AlertDialog;
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

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.pitstop.database.LocalDataRetriever;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    public static final String TAG = SettingsActivity.class.getSimpleName();

    ArrayList<String> cars;
    ArrayList<String> ids;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_settings);
        cars = getIntent().getExtras().getStringArrayList("cars");
        ids = getIntent().getStringArrayListExtra("ids");
        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment(cars,ids)).commit();
    }

//    @Override
//    protected void onPostCreate(Bundle savedInstanceState) {
//        super.onPostCreate(savedInstanceState);
//        Toolbar bar;
//
//        //setStatusBarColor(findViewById(R.id.statusBarBackground), getResources().getColor(R.color.primary_dark));
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
//            LinearLayout root = (LinearLayout) findViewById(android.R.id.list).getParent().getParent().getParent();
//            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
//            root.addView(bar, 0); // insert at top
//        } else {
//            ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
//            ListView content = (ListView) root.getChildAt(0);
//
//            root.removeAllViews();
//
//            bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
//
//
//            int height;
//            TypedValue tv = new TypedValue();
//            if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
//                height = TypedValue.complexToDimensionPixelSize(tv.data, getResources().getDisplayMetrics());
//            }else{
//                height = bar.getHeight();
//            }
//
//            content.setPadding(0, height, 0, 0);
//
//            root.addView(content);
//            root.addView(bar);
//        }
//
//        bar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                finish();
//            }
//        });
//    }
//
//    public void setStatusBarColor(View statusBar,int color){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            Window w = getWindow();
//            w.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
//            //status bar height
//            int actionBarHeight = getActionBarHeight();
//            int statusBarHeight = getStatusBarHeight();
//            //action bar height
//            statusBar.getLayoutParams().height = actionBarHeight + statusBarHeight;
//            statusBar.setBackgroundColor(color);
//        }
//    }
//
//    public int getActionBarHeight() {
//        int actionBarHeight = 0;
//        TypedValue tv = new TypedValue();
//        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
//        {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
//        }
//        return actionBarHeight;
//    }
//
//    public int getStatusBarHeight() {
//        int result = 0;
//        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
//        if (resourceId > 0) {
//            result = getResources().getDimensionPixelSize(resourceId);
//        }
//        return result;
//    }

    public class SettingsFragment extends PreferenceFragment {

        ArrayList<ListPreference> preferenceList;
        ArrayList<String> cars;
        ArrayList<String> ids;
        public SettingsFragment(ArrayList<String> cars, ArrayList<String> ids){
            this.cars  =cars;
            this.ids = ids;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            preferenceList = new ArrayList<>();
            cars = ((SettingsActivity)getActivity()).cars;
            ids = ((SettingsActivity)getActivity()).ids;

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Shop");
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
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
                        listPreference.setDialogTitle("Choose Shop for: " + cars.get(i));
                        final int index = i;
                        listPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                            @Override
                            public boolean onPreferenceChange(Preference preference, Object newValue) {
                                final String shopSelected = (String) newValue;
                                LocalDataRetriever ldr = new LocalDataRetriever(getActivity());
                                HashMap<String, String> hm = new HashMap<String, String>();
                                hm.put("dealership", shopSelected);
                                ldr.updateData("Cars", "CarID", ids.get(index), hm);
                                //updateParse
                                ParseQuery<ParseObject> query = ParseQuery.getQuery("Car");
                                query.getInBackground(ids.get(index), new GetCallback<ParseObject>() {
                                    public void done(ParseObject car, ParseException e) {
                                        if (e == null) {
                                            // Now let's update it with some new data. In this case, only cheatMode and score
                                            // will get sent to the Parse Cloud. playerName hasn't changed.
                                            car.put("dealership", shopSelected);
                                            car.saveInBackground();
                                        }
                                    }
                                });
                                return true;
                            }
                        });
                        ((PreferenceCategory)  getPreferenceManager().findPreference(getString(R.string.pref_vehicles))).addPreference(listPreference);
                    }
                }
            });
        }

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            addPreferencesFromResource(R.xml.preferences);

            Preference namePreference = findPreference(getString(R.string.pref_username_key));
            namePreference.setTitle(ParseUser.getCurrentUser().getUsername());

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

        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            return super.onCreateView(inflater, container, savedInstanceState);
        }

        private void navigateToLogin() {
            Intent intent = new Intent(this.getActivity(), SplashScreen.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            Log.i(TAG, "navigateToLogin ran");
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent i = new Intent(SettingsActivity.this, ReceiveDebugActivity.class);
            startActivity(i);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}