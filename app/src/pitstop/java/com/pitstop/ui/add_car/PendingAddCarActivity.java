package com.pitstop.ui.add_car;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.castel.obd.util.Utils;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David Liu on 2/21/2016.
 */

public class PendingAddCarActivity extends AppCompatActivity{

    public final static String pfName = "com.pitstop.login.name";

    public static String ADD_CAR_VIN = "PENDING_ADD_CAR_VIN";
    public static String ADD_CAR_SCANNER = "PENDING_ADD_CAR_SCANNER_ID";
    public static String ADD_CAR_MILEAGE = "PENDING_ADD_CAR_MILEAGE";

    GlobalApplication application;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pending_add_car);
        application = (GlobalApplication) getApplicationContext();
        SharedPreferences settings =
                getSharedPreferences(pfName, MODE_PRIVATE);
        Intent intentFromMainActivity = getIntent();
        if(intentFromMainActivity != null) {
            String vin = intentFromMainActivity.getStringExtra(ADD_CAR_VIN);
            String scannerId = intentFromMainActivity.getStringExtra(ADD_CAR_SCANNER);
            String mileage = intentFromMainActivity.getStringExtra(ADD_CAR_MILEAGE);

            if(!Utils.isEmpty(vin)) {

                SharedPreferences.Editor editor = settings.edit();
                editor.putString(ADD_CAR_SCANNER, scannerId);
                editor.putString(ADD_CAR_MILEAGE, mileage);
                editor.putString(ADD_CAR_VIN, vin);
                editor.apply();
            }
        }

        ((TextView)findViewById(R.id.vin)).setText("VIN: " + settings.getString(ADD_CAR_VIN,""));
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);

        try {
            application.getMixpanelAPI().track("View Appeared",
                    new JSONObject("{'View':'PendingAddCarActivity'}"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Create the Handler object (on the main thread by default)
    Handler handler = new Handler();
    // Define the code block to be executed
    private Runnable runnableCode = new Runnable() {
        @Override
        public void run() {
            if (NetworkHelper.isConnected(PendingAddCarActivity.this)) {
                goBackToAddCar();
            } else {
                // Repeat this the same carConnectedRunnable code block again another 3 seconds
                handler.postDelayed(runnableCode, 3000);
            }
        }
    };

    private void goBackToAddCar() {
        SharedPreferences settings = getSharedPreferences(pfName, MODE_PRIVATE);
        // TODO: this stuff might not be needed
        Intent intent = new Intent(PendingAddCarActivity.this, com.pitstop.ui.add_car.AddCarActivity.class);
        intent.putExtra(ADD_CAR_VIN,settings.getString(ADD_CAR_VIN,""));
        intent.putExtra(ADD_CAR_SCANNER,settings.getString(ADD_CAR_SCANNER,""));
        intent.putExtra(ADD_CAR_MILEAGE,settings.getString(ADD_CAR_MILEAGE,"0"));
        setResult(MainActivity.Companion.getRESULT_OK(),intent);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ADD_CAR_SCANNER, "");
        editor.putString(ADD_CAR_MILEAGE, "0");
        editor.putString(ADD_CAR_VIN, "");
        editor.apply();
        finish();
    }

    @Override
    public void onBackPressed() {
        if (NetworkHelper.isConnected(this)){
            goBackToAddCar();
        }
    }
}
