package com.pitstop.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.pitstop.ui.MainActivity;
import com.pitstop.ui.BSAbstractedFragmentActivity;

/**
 * Created by david on 7/21/2016.
 */
public class BluetoothServiceConnection implements ServiceConnection {
    private static final String TAG = BluetoothServiceConnection.class.getSimpleName();
    Context context;
    BSAbstractedFragmentActivity callbackActivity;

    private static final int RC_LOCATION_PERM = 101;

    public BluetoothServiceConnection(Context context,BSAbstractedFragmentActivity activity){
        this.context = context;
        callbackActivity = activity;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
        // cast the IBinder and get MyService instance
        callbackActivity.serviceIsBound = true;
        callbackActivity.autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
        callbackActivity.autoConnectService.setCallbacks(callbackActivity); // register
        Log.i(TAG, "connecting: onServiceConnection");

        if (BluetoothAdapter.getDefaultAdapter()!=null) {

            if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                callbackActivity.startActivityForResult(enableBtIntent, MainActivity.RC_ENABLE_BT);
                return;
            }

            if(ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(context, MainActivity.LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(callbackActivity, MainActivity.LOC_PERMS, RC_LOCATION_PERM);
            }
        }

        callbackActivity.autoConnectService.removeSyncedDevice();

    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
        callbackActivity.serviceIsBound = false;
        callbackActivity.autoConnectService = null;
        Log.i("Disconnecting","onServiceConnection");
    }
}
