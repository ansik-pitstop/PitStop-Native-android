package com.pitstop.bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that identifies IDD bluetooth device
 */
public class BluetoothDeviceRecognizer {

    private static final String TAG = BluetoothDeviceRecognizer.class.getSimpleName();

    private static final int NOTIFICATION_ID = 9101; // arbitrary id

    public enum RecognizeResult {
        IGNORE, CONNECT, DISCONNECT, BANNED
    }

    private final MixpanelHelper mMixpanelHelper;
    private final Context mContext;

    private List<BluetoothDevice> bannedDeviceList = new ArrayList<>();

    public BluetoothDeviceRecognizer(Context context) {
        mMixpanelHelper = new MixpanelHelper((GlobalApplication) context.getApplicationContext());
        mContext = context;
    }

    public void banDevice(BluetoothDevice device){
        bannedDeviceList.add(device);
    }

    private boolean isBanned(BluetoothDevice device){
        for (BluetoothDevice d: bannedDeviceList){
            if (d.getAddress().equals(device.getAddress())
                    && d.getName().equals(device.getName())){
                return true;
            }
        }
        return false;
    }

    public RecognizeResult onDeviceFound(BluetoothDevice device) {
        if (device != null && device.getName() != null && device.getName().contains("2382")) return RecognizeResult.DISCONNECT; //TEST
        if (device == null || device.getName() == null) return RecognizeResult.DISCONNECT;

        Log.d(TAG,"onDeviceFound, device: "+device.getName() +" "+ device.getAddress());

        if (device.getName() == null || !device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
            Log.d(TAG,"scanner name null or scanner name doesn't contain BT_DEVICE_NAME");
            return RecognizeResult.IGNORE;
        }
        else if (isBanned(device) && !AddCarActivity.addingCarWithDevice){
            Log.d(TAG,"Device banned");
            return RecognizeResult.BANNED;
        }
        else{
            Log.d(TAG,"Returning CONNECT");
            return RecognizeResult.CONNECT;
        }

    }

    private void notifyOnUnrecognizedDeviceFound(String scannerName) {

        final NotificationManager notificationManager = (NotificationManager)
                mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        Bitmap icon = BitmapFactory.decodeResource(mContext.getResources(),
                R.mipmap.ic_push);

        final Intent addCarIntent = new Intent(mContext, AddCarActivity.class);
        addCarIntent.putExtra(AddCarActivity.EXTRA_PAIR_PENDING, true);

        final PendingIntent click = PendingIntent.getActivity(mContext, 0,
                addCarIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        final NotificationCompat.Builder builder =
                new NotificationCompat.Builder(mContext)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setTicker("Unrecognized Pitstop device found!")
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(click)
                        .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                        .setLargeIcon(icon)
                        .setColor(ContextCompat.getColor(mContext, R.color.highlight))
                        .setContentTitle("Unrecognized Pitstop device found")
                        .setContentText("Tap to pair with " + scannerName);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}
