package com.pitstop;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.parse.ParsePushBroadcastReceiver;

import org.json.JSONException;
import org.json.JSONObject;

public class PitstopPushBroadcastReceiver extends ParsePushBroadcastReceiver {

    private static final String EXTRA_PARSE_DATA = "com.parse.Data";
    private static final String PARSE_DATA_ACTION = "action";
    private static final String PARSE_DATA_CAR_ID = "car_id";

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_CAR_ID = "carId";

    public static final String ACTION_UPDATE_MILEAGE = "update_mileage";

    @Override
    public void onPushOpen(Context context, Intent intent) {
        // if we don't open a specialized activity, delegate to "super" which will open the default
        // activity (i.e. MainActivity)
        boolean openedActivity = false;

        // get the data from parse to see if we need to perform a special action
        try {
            JSONObject data = new JSONObject(intent.getStringExtra(EXTRA_PARSE_DATA));
            String action = data.getString(PARSE_DATA_ACTION);

            if (ACTION_UPDATE_MILEAGE.equals(action)) {
                String carId = data.getString(PARSE_DATA_CAR_ID);
                Intent target = new Intent(context, MainActivity.class);
                target.putExtra(EXTRA_ACTION, ACTION_UPDATE_MILEAGE);
                target.putExtra(EXTRA_CAR_ID, carId);
                target.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(target);
                openedActivity = true;
            }

        } catch (JSONException e) {
            Log.e("PushBroadcastReceiver", "invalid parse json data", e);
        }

        if (!openedActivity) {
            super.onPushOpen(context, intent);
        }
    }
}
