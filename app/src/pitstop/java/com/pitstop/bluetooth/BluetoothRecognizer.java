package com.pitstop.bluetooth;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.R;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.ui.AddCarActivity;

/**
 * Created by yifan on 16/11/4.
 */

public class BluetoothRecognizer {

    private static final String TAG = BluetoothRecognizer.class.getSimpleName();

    private static final int NOTIFICATION_ID = 9101; // arbitrary id

    public enum RecognizeResult {
        IGNORE, CONNECT, DISCONNECT, YOLO
    }

    private final LocalScannerAdapter mLocalScannerStore;
    private final Context mContext;

    public BluetoothRecognizer(Context context) {
        mLocalScannerStore = new LocalScannerAdapter(context);
        mContext = context;
    }

    public RecognizeResult onDeviceFound(String scannerName) {
        if (scannerName == null || !scannerName.contains(ObdManager.BT_DEVICE_NAME)) {
            return RecognizeResult.IGNORE;
        }

        Log.d(TAG, "Adding car with device: " + AddCarActivity.addingCarWithDevice);
        Log.d(TAG, "Any scanner lack name: " + mLocalScannerStore.anyScannerLackName());
        Log.d(TAG, "Device name exists: " + mLocalScannerStore.deviceNameExists(scannerName));
        Log.d(TAG, "Any car lack scanner: " + mLocalScannerStore.anyCarLackScanner());

        if (AddCarActivity.addingCarWithDevice
                || mLocalScannerStore.anyScannerLackName()
                || mLocalScannerStore.deviceNameExists(scannerName)) {
            return RecognizeResult.CONNECT;
        } else if (mLocalScannerStore.anyCarLackScanner()) {
            notifyOnUnrecognizedDeviceFound(scannerName);
            return RecognizeResult.IGNORE;
        } else {
            // this part should never be reached.... but whatever
            return RecognizeResult.YOLO;
        }
    }

    public void onDeviceLogin(String scannerName, String scannerId){
        if (mLocalScannerStore.scannerIdExists(scannerId)) {
            mLocalScannerStore.updateScannerName(scannerId, scannerName);
        } else {
            // TODO: 16/11/4 if scannerId does not exists
            if (AddCarActivity.addingCarWithDevice) {
                // 1. Adding car

            } else {
                // 2. Auto connect because of B

            }
        }
    }

    public void onDeviceConnected(String scannerName, String scannerId) {
        if (mLocalScannerStore.scannerIdExists(scannerId)) {
            mLocalScannerStore.updateScannerName(scannerId, scannerName);
        } else {
            // TODO: 16/11/4 Scanner id does not exist locally
            if (AddCarActivity.addingCarWithDevice) {
                // 1. Adding car

            } else {
                // 2. Auto connect because of B

            }
        }
    }

    /**
     * When device is paired
     * @param scannerName
     * @param scannerId
     * @param carId
     */
    public void onDevicePaired(String scannerName, String scannerId, String carId){
        // TODO: 16/11/4 create a new row in local scanner table

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
                        .setTicker("Unrecognized OBD device found!")
                        .setAutoCancel(true)
                        .setOnlyAlertOnce(true)
                        .setContentIntent(click)
                        .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                        .setLargeIcon(icon)
                        .setColor(ContextCompat.getColor(mContext, R.color.highlight))
                        .setContentTitle("Unrecognized OBD device found")
                        .setContentText("Tap to pair with " + scannerName);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

}
