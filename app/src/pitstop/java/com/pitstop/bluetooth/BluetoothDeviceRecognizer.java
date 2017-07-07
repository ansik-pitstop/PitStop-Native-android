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
import com.pitstop.application.GlobalApplication;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.ObdScanner;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.List;

/**
 * Class that identifies IDD bluetooth device
 */
public class BluetoothDeviceRecognizer {

    private static final String TAG = BluetoothDeviceRecognizer.class.getSimpleName();

    private static final int NOTIFICATION_ID = 9101; // arbitrary id

    public enum RecognizeResult {
        IGNORE, CONNECT, DISCONNECT
    }

    private final LocalScannerAdapter mLocalScannerStore;
    private final MixpanelHelper mMixpanelHelper;
    private final Context mContext;

    public BluetoothDeviceRecognizer(Context context) {
        mLocalScannerStore = new LocalScannerAdapter(context);
        mMixpanelHelper = new MixpanelHelper((GlobalApplication) context.getApplicationContext());
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

        logScannerTable();

        //TEST REMOVE
        if (scannerName.endsWith("XXX")){
            return RecognizeResult.CONNECT;
        }
        else{
            return RecognizeResult.IGNORE;
        }
//        if (scannerName.endsWith("XXX") || AddCarActivity.addingCarWithDevice
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

    private void logScannerTable() {
        List<ObdScanner> scanners = mLocalScannerStore.getAllScanners();
        if (scanners.size() == 0) Log.d(TAG, "Scanner table is empty");
        for (ObdScanner scanner : scanners) {
            Log.d(TAG, "Scanner name: " + scanner.getDeviceName());
            Log.d(TAG, "Scanner ID: " + scanner.getScannerId());
            Log.d(TAG, "Car ID: " + scanner.getCarId());
        }
    }

}
