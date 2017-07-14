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
import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalScannerAdapter;
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

    private final LocalScannerAdapter mLocalScannerStore;
    private final MixpanelHelper mMixpanelHelper;
    private final Context mContext;

    private List<BluetoothDevice> bannedDeviceList = new ArrayList<>();

    public BluetoothDeviceRecognizer(Context context) {
        mLocalScannerStore = new LocalScannerAdapter(context);
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
        if (device == null || device.getName() == null) return RecognizeResult.DISCONNECT;

        //Don't connect to Nitish's car
        if (BuildConfig.DEBUG && device.getName().equals("IDD-215B 003028")){
            return RecognizeResult.DISCONNECT;
        }
        //Connect to test device
        if (BuildConfig.DEBUG && device.getName().endsWith("XXX")){
            return RecognizeResult.CONNECT;
        }


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

//        //ONLY CONNECT TO THIS DEVICE FOR TESTING
//        if (BuildConfig.DEBUG){
//            //if (scannerName.endsWith("XXX")){
//                return RecognizeResult.CONNECT;
//            //}
//            //else{
//            //    return RecognizeResult.IGNORE;
//            //}
//        }
//
//        if (AddCarActivity.addingCarWithDevice
//                || mLocalScannerStore.anyScannerLackName()
//                || mLocalScannerStore.deviceNameExists(scannerName)) {
//            return RecognizeResult.CONNECT;
//        } else if (mLocalScannerStore.anyCarLackScanner()) {
//            //notifyOnUnrecognizedDeviceFound(scannerName); REMOVE PUSH NOTIFICATIONS THEY ARE ANNOYING
//            mMixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_FOUND);
//            return RecognizeResult.IGNORE;
//        } else { // this part should never be reached.... but whatever
//            return RecognizeResult.IGNORE;
//        }
    }

    public void onDeviceConnected(String scannerName, String scannerId){
        if (scannerName == null || scannerId == null) return; // just to be fault-tolerant
        if (mLocalScannerStore.scannerIdExists(scannerId)) {
            mLocalScannerStore.updateScannerName(scannerId, scannerName);
        } else { // Scanner Id does not exist locally
            if (AddCarActivity.addingCarWithDevice) {
                // 1. Adding car

            } else {
                // 2. Connected to a wrong device most likely

            }
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
