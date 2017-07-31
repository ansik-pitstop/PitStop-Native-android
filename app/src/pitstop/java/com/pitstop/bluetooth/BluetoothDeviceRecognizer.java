package com.pitstop.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.castel.obd.bluetooth.ObdManager;
import com.pitstop.application.GlobalApplication;
import com.pitstop.ui.add_car.AddCarActivity;
import com.pitstop.utils.MixpanelHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class that identifies IDD bluetooth device
 */
public class BluetoothDeviceRecognizer {

    public interface Callback{
        void onDevice212Ready(BluetoothDevice device);
        void onDevice215Ready(BluetoothDevice device);
        void onNoDeviceFound();
    }

    private static final String TAG = BluetoothDeviceRecognizer.class.getSimpleName();
    private Callback callback;
    private static final int NOTIFICATION_ID = 9101; // arbitrary id

    public enum RecognizeResult {
        VALID, INVALID, BANNED
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

    private boolean rssiScanInProgress = false;
    private Map<BluetoothDevice,Short> deviceRssiMap = new HashMap<>();
    private final Short MIN_RSSI_THRESHOLD = -55;

    public void onStartRssiScan(Callback callback, Handler handler){
        Log.d(TAG,"onStartRssiScan() called, rssiScanInProgress? "+rssiScanInProgress);

        if (rssiScanInProgress) return;
        rssiScanInProgress = true;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                short strongestRssi = Short.MAX_VALUE;
                BluetoothDevice strongestRssiDevice = null;

                for (Map.Entry<BluetoothDevice,Short> device: deviceRssiMap.entrySet()){
                    if (device.getValue() != null && device.getValue() < strongestRssi){
                        strongestRssiDevice = device.getKey();
                        strongestRssi = device.getValue();
                    }
                }
                Log.d(TAG,"Strongest rssi found: "+strongestRssi+", device name: "
                        +strongestRssiDevice.getName());

                if (strongestRssiDevice == null || strongestRssi >= MIN_RSSI_THRESHOLD){
                    callback.onNoDeviceFound();
                }
                else if (strongestRssi < MIN_RSSI_THRESHOLD){
                    if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_212)) {
                        callback.onDevice212Ready(strongestRssiDevice);
                    } else if (strongestRssiDevice.getName().contains(ObdManager.BT_DEVICE_NAME_215)) {
                        callback.onDevice215Ready(strongestRssiDevice);
                    }
                }

                rssiScanInProgress = false;
            }
        },12000);
    }

    public RecognizeResult onDeviceFound(BluetoothDevice device, short rssi) {
        if (device == null || device.getName() == null) return RecognizeResult.INVALID;

        Log.d(TAG,"onDeviceFound, device: "+device.getName() +" "+ device.getAddress());

        if (device.getName() == null || !device.getName().contains(ObdManager.BT_DEVICE_NAME)) {
            Log.d(TAG,"scanner name null or scanner name doesn't contain BT_DEVICE_NAME");
            return RecognizeResult.INVALID;
        }
        else if (isBanned(device) && !AddCarActivity.addingCar){
            Log.d(TAG,"Device banned");
            return RecognizeResult.BANNED;
        }
        else{
            Log.d(TAG,"Returning CONNECT");
            deviceRssiMap.put(device,rssi);
            return RecognizeResult.VALID;
        }

    }

}
