package com.pitstop;

import android.content.Context;
import android.content.Intent;

import com.parse.ParsePushBroadcastReceiver;

public class PitstopPushBroadcastReceiver extends ParsePushBroadcastReceiver {

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_CAR_ID = "carId";

    public static final String ACTION_UPDATE_MILEAGE = "update_mileage";

    @Override
    public void onPushOpen(Context context, Intent intent) {
        String type = intent.getStringExtra("action");

        if (ACTION_UPDATE_MILEAGE.equals(type)) {
            final String carId = intent.getStringExtra("car_id");
            Intent target = new Intent(context, MainActivity.class);
            target.putExtra(EXTRA_ACTION, ACTION_UPDATE_MILEAGE);
            target.putExtra(EXTRA_CAR_ID, carId);
            context.startActivity(target);

        } else {
            super.onPushOpen(context, intent);
        }
    }
}
