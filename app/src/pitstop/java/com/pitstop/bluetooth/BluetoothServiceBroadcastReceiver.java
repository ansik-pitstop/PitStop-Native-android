package com.pitstop.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;

import com.pitstop.observer.ConnectionStatusObserver;
import com.pitstop.utils.NetworkHelper;

/**
 * BroadcastReceiver made specifically for BluetoothAutoConnectService
 */
final class BluetoothServiceBroadcastReceiver extends BroadcastReceiver {

    private final String TAG = getClass().getSimpleName();

    private ConnectionStatusObserver connectionStatusObserver;

    public BluetoothServiceBroadcastReceiver(ConnectionStatusObserver connectionStatusObserver) {
        this.connectionStatusObserver = connectionStatusObserver;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {

            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
            Log.i(TAG, "Bluetooth adapter state changed: " + state);

            if(state == BluetoothAdapter.STATE_OFF) {
                connectionStatusObserver.onBluetoothOff();

            } else if(state == BluetoothAdapter.STATE_ON && BluetoothAdapter.getDefaultAdapter() != null) {
                connectionStatusObserver.onBluetoothOn();
            }
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) { // internet connectivity listener
            if (NetworkHelper.isConnected(context)) {
                connectionStatusObserver.onConnectedToInternet();
            }
        }
    }
}
