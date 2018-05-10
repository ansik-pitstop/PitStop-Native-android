package com.pitstop.ui;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;

import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothServiceConnection;
import com.pitstop.ui.trip.TripActivityObservable;
import com.pitstop.utils.AnimatedDialogBuilder;

import static android.Manifest.permission.ACCESS_FINE_LOCATION;

/**
 * Created by david on 7/21/2016.
 */
public abstract class IBluetoothServiceActivity extends DebugDrawerActivity{
    private final String TAG = getClass().getSimpleName();
    public TripActivityObservable tripActivityObservable;

    public static final int RC_LOCATION_PERM = 101;

    public static final String[] LOC_PERMS = {ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    public void requestPermission(final Activity activity, final String[] permissions, final int requestCode,
                                   final boolean needDescription, @Nullable final String message) {
        if (isFinishing()) {
            return;
        }

        if (needDescription) {
            new AnimatedDialogBuilder(activity)
                    .setCancelable(false)
                    .setTitle(getString(R.string.request_permission_alert_title))
                    .setMessage(message != null ? message : getString(R.string.request_permission_message_default))
                    .setNegativeButton("", null)
                    .setPositiveButton(getString(R.string.ok_button), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(activity, permissions, requestCode);
                        }
                    }).show();
        } else {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    public void checkPermissions(){
        if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG,"Permission not granted! requesting permission!");
            requestPermission(this, new String[]{ACCESS_FINE_LOCATION}, RC_LOCATION_PERM,
                    true, getString(R.string.request_permission_location_message));
        }
        else if (ContextCompat.checkSelfPermission(this, ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            Log.d(TAG,"Permission granted!");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == BluetoothServiceConnection.RC_LOCATION_PERM) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Snackbar.make(findViewById(android.R.id.content), R.string.location_request_rationale, Snackbar.LENGTH_INDEFINITE)
                        .setAction(getString(R.string.retry_button), new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ActivityCompat.requestPermissions(IBluetoothServiceActivity.this,
                                        getResources().getStringArray(R.array.permissions_location),
                                        BluetoothServiceConnection.RC_LOCATION_PERM);
                            }
                        })
                        .show();
            }
        }
    }
}
