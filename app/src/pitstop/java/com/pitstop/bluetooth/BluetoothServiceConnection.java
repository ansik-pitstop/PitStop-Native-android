package com.pitstop.bluetooth;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import com.pitstop.ui.BluetoothPresenter;
import com.pitstop.ui.IBluetoothServiceActivity;

/**
 * Created by david on 7/21/2016.
 */
public class BluetoothServiceConnection implements ServiceConnection {
    private static final String TAG = BluetoothServiceConnection.class.getSimpleName();
    private Context context;
    private IBluetoothServiceActivity activity;
    private BluetoothPresenter presenter;

    public static final int RC_LOCATION_PERM = 101;
    public static final int RC_ENABLE_BT = 102;

    public BluetoothServiceConnection(Context context, IBluetoothServiceActivity activity, BluetoothPresenter presenter){
        this.context = context;
        this.activity = activity;
        this.presenter = presenter;
    }

    @Override
    public void onServiceConnected(ComponentName className, IBinder service) {
//        // cast the IBinder and get MyService instance
//        activity.autoConnectService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
//
//        if (BluetoothAdapter.getDefaultAdapter()!=null) {
//
//            if(!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
//                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                activity.startActivityForResult(enableBtIntent, RC_ENABLE_BT);
//                return;
//            }
//
//            String[] locationPermissions = activity.getResources().getStringArray(R.array.permissions_location);
//            for (String permission : locationPermissions) {
//                if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
//                    activity.requestPermission(activity, locationPermissions,
//                            RC_LOCATION_PERM, true, activity.getString(R.string.request_permission_location_message));
//                    break;
//                }
//            }
//
//        }
//
//        activity.autoConnectService.removeSyncedDevice();
    }

    @Override
    public void onServiceDisconnected(ComponentName arg0) {
//        presenter.onServiceUnbind();
//        activity.serviceIsBound = false;
//        activity.autoConnectService = null;
//        Log.i("Disconnecting","onServiceConnection");
    }
}
