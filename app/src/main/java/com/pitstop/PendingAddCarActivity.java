package com.pitstop;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import com.pitstop.parse.ParseApplication;
import com.pitstop.utils.InternetChecker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

/**
 * Created by David Liu on 2/21/2016.
 */
public class PendingAddCarActivity extends AppCompatActivity{

    // TODO: Transferring data through intents is safer than using global variables (bugs)
    public static String ADD_CAR_VIN = "PENDING_ADD_CAR_VIN";
    public static String ADD_CAR_SCANNER = "PENDING_ADD_CAR_SCANNER_ID";
    public static String ADD_CAR_DTCS = "PENDING_ADD_CAR_DTCS";
    public static String ADD_CAR_MILEAGE = "PENDING_ADD_CAR_MILEAGE";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_pending_add_car);
        // TODO: Transferring data through intents is safer than using global variables (bugs)
        SharedPreferences settings = getSharedPreferences(MainActivity.pfName, MODE_PRIVATE);
        if(AddCarActivity.VIN!=null&&!AddCarActivity.VIN.equals("")) {
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(ADD_CAR_DTCS, AddCarActivity.dtcs);
            editor.putString(ADD_CAR_SCANNER, AddCarActivity.scannerID);
            editor.putString(ADD_CAR_MILEAGE, AddCarActivity.mileage);
            editor.putString(ADD_CAR_VIN, AddCarActivity.VIN);
            editor.apply();
        }

        ((TextView)findViewById(R.id.vin)).setText("VIN: " + settings.getString(ADD_CAR_VIN,""));
        // Start the initial runnable task by posting through the handler
        handler.post(runnableCode);

        try {
            ParseApplication.mixpanelAPI.track("View Appeared", new JSONObject("{'View':'PendingAddCarActivity'}"));
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
            try {
                if (new InternetChecker(getBaseContext()).execute().get()){
                    goBackToAddCar();
                }else{
                    // Repeat this the same runnable code block again another 3 seconds
                    handler.postDelayed(runnableCode, 3000);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
    };

    private void goBackToAddCar() {
        SharedPreferences settings = getSharedPreferences(MainActivity.pfName, MODE_PRIVATE);
        AddCarActivity.dtcs = settings.getString(ADD_CAR_DTCS,"");
        AddCarActivity.VIN = settings.getString(ADD_CAR_VIN,"");
        AddCarActivity.scannerID = settings.getString(ADD_CAR_SCANNER,"");
        AddCarActivity.mileage = settings.getString(ADD_CAR_MILEAGE,"");
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ADD_CAR_DTCS, "");
        editor.putString(ADD_CAR_SCANNER, "");
        editor.putString(ADD_CAR_MILEAGE, "");
        editor.putString(ADD_CAR_VIN, "");
        editor.apply();
        Intent intent = new Intent(PendingAddCarActivity.this,AddCarActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }
}
