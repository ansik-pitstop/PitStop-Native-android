package com.pitstop.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothService;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.utils.AnimatedDialogBuilder;

import io.reactivex.Observable;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by david on 7/21/2016.
 */
public abstract class IBluetoothServiceActivity extends DebugDrawerActivity{
    private final String TAG = getClass().getSimpleName();

    public static final int RC_LOCATION_PERM = 101;
    protected BluetoothService bluetoothService;

    public void requestPermission(final Activity activity, final String[] permissions, final int requestCode,
                                  @Nullable final String message) {
        if (isFinishing()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 23 && shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
            new AnimatedDialogBuilder(activity)
                    .setCancelable(false)
                    .setTitle(getString(R.string.request_permission_alert_title))
                    .setMessage(message != null ? message : getString(R.string.request_permission_message_default))
                    .setNegativeButton("", null)
                    .setPositiveButton(getString(R.string.ok_button), (dialog, which) -> ActivityCompat.requestPermissions(activity, permissions, requestCode)).show();
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    public boolean checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"Permission not granted! requesting permission!");
            requestPermission(this, new String[]{ACCESS_FINE_LOCATION}, RC_LOCATION_PERM
                    , getString(R.string.request_permission_location_message));
            return false;
        }
        else{
            Log.d(TAG,"Permission granted!");
            return true;
        }
    }

    @Override
    protected void onStart() {
        checkPermissions();
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions
                                            ,int[] grantResults) {
        if (requestCode == RC_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getBluetoothService()
                        .take(1)
                        .filter((it)-> it.getDeviceState() != BluetoothConnectionObservable.State.DISCONNECTED)
                        .subscribe((it)-> it.requestDeviceSearch(false,false));
            }
        }
    }

    public Observable<BluetoothService> getBluetoothService(){
        return ((GlobalApplication)getApplicationContext())
                .getServices()
                .filter ((it)-> it instanceof BluetoothService)
                .map((it)->{
                    bluetoothService = (BluetoothService)it;
                    return bluetoothService;
                });
    }

    public BluetoothConnectionObservable getBluetoothConnectionObservable(){
        return bluetoothService;
    }
}
