package com.pitstop.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.BuildConfig;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothWriter;
import com.pitstop.database.LocalAlarmStorage;
import com.pitstop.database.LocalDebugMessageStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.ViewUtils;
import com.squareup.sqlbrite.QueryObservable;

import java.util.Calendar;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

public abstract class DebugDrawerActivity extends AppCompatActivity implements BluetoothConnectionObserver{
    public static final String TAG = DebugDrawerActivity.class.getSimpleName();

    private NetworkHelper mNetworkHelper;

    protected DrawerLayout mDrawerLayout;

    // for logging
    private LocalDebugMessageStorage mDebugMessageAdapter;
    private LocalAlarmStorage localAlarmStorage;
    private QueryObservable mQueryBluetoothObservable;
    private Subscription mQueryBluetoothSubscription;
    private QueryObservable mQueryNetworkObservable;
    private Subscription mQueryNetworkSubscription;
    private QueryObservable mQueryOtherObservable;
    private Subscription mQueryOtherSubscription;
    private AlertDialog confirmRTCAlertDialog;
    private BluetoothWriter bluetoothWriter;

    private Intent serviceIntent;
    private boolean mLogsEnabled;
    BluetoothConnectionObservable bluetoothConnectionObservable;
    EditText editText;


    ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "connecting: onServiceConnected");
            // cast the IBinder and get MyService instance
            bluetoothConnectionObservable = ((BluetoothConnectionObservable)((BluetoothAutoConnectService.BluetoothBinder) service).getService());
            bluetoothConnectionObservable.subscribe(DebugDrawerActivity.this);
            bluetoothWriter = ((BluetoothWriter)((BluetoothAutoConnectService.BluetoothBinder) service).getService());

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Disconnecting: onServiceConnection");
            bluetoothConnectionObservable = null;
        }
    };

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        setContentView(getLayoutInflater().inflate(layoutResID, mDrawerLayout, false));
    }

    @Override
    public void setContentView(View view) {
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)
                ) {
            mDrawerLayout.addView(view, 0);
        } else {
            super.setContentView(view);
        }
    }



    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (!BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)) {
            DrawerLayout drawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
            drawerLayout.addView(view, 0, params);
            super.setContentView(drawerLayout);
        } else {
            super.setContentView(view, params);
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)){
            return;
        }


        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        mNetworkHelper = tempNetworkComponent.networkHelper();

        localAlarmStorage = new LocalAlarmStorage(this);
        mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
        super.setContentView(mDrawerLayout);


        serviceIntent = new Intent(DebugDrawerActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);


       editText = ViewUtils.findView(mDrawerLayout, R.id.debug_edit_text);
        Button getSupportedPids = ViewUtils.findView(mDrawerLayout, R.id.debugGetSupportedPids);
        getSupportedPids.setOnClickListener(v -> {
            if (bluetoothConnectionObservable!=null)
                bluetoothConnectionObservable.getSupportedPids();
        });

        Button clearAlarms = ViewUtils.findView(mDrawerLayout, R.id.clear_alarms);
        clearAlarms.setOnClickListener(v->{
            localAlarmStorage.deleteAllRows();
        });

        Button setInterval = ViewUtils.findView(mDrawerLayout, R.id.debugSetInterval);
        setInterval.setOnClickListener(v->{
            showRTCOverWriteCOnfirmDialog();

        });

        Button resetDTC = ViewUtils.findView(mDrawerLayout, R.id.debugClearDTC);
        resetDTC.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmResetDTCDialog();

            }
        });
        Button resetMemory = ViewUtils.findView(mDrawerLayout, R.id.debugClearDeviceMemory);
        resetMemory.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showConfirmResetMemoryDialog();
            }
        });
        View vinButton = findViewById(R.id.debugRandomVin);
        vinButton.setOnClickListener(v -> mNetworkHelper.getRandomVin(
                (response, requestError) -> {
                    editText.setText(requestError == null ? response : "error: " + requestError.getMessage());
                })
        );

        Button setChunkSizeButton = ViewUtils.findView(mDrawerLayout, R.id.debugSetNetworkChunkSize);
        setChunkSizeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    int k = Integer.valueOf(editText.getText().toString());
                    setChunkSize(k);
                }
                catch(NumberFormatException e){
                    // if input isnt an integer
                    editText.setText("Make sure you input an integer.");
                }
            }
        });
        setupLogging();
    }

    private void setChunkSize(int chunkSize) {
        if(bluetoothWriter!= null){
            bluetoothWriter.setChunkSize(chunkSize);
        }
    }

    private void showConfirmResetDTCDialog() {
        if (bluetoothWriter==null)
            return;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Confirm Clear DTC");
        alertDialogBuilder
                .setMessage("Are you sure you want to clear DTCs")
                .setCancelable(true)
                .setNegativeButton("NO", (dialog, id) -> {
                    dialog.dismiss();})
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bluetoothWriter.clearDTCs();
                    }
                });
        confirmRTCAlertDialog = alertDialogBuilder.create();
        confirmRTCAlertDialog.show();

    }

    private void showConfirmResetMemoryDialog() {
        if (bluetoothWriter==null)
            return;
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
        alertDialogBuilder.setTitle("Confirm Clear Memory");
        alertDialogBuilder
                .setMessage("Are you sure you want to reset device memory and historical Data")
                .setCancelable(true)
                .setNegativeButton("NO", (dialog, id) -> {
                    dialog.dismiss();})
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        bluetoothWriter.resetMemory();
                    }
                });
        confirmRTCAlertDialog = alertDialogBuilder.create();
        confirmRTCAlertDialog.show();
    }

    private void showRTCOverWriteCOnfirmDialog() {
        if (bluetoothWriter==null)
            return;
        int Interval;
        try {
            Interval =  Integer.parseInt(editText.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Make sure there is a number in the box", Toast.LENGTH_SHORT).show();
            return;
        }
        if (Interval%2==1){
            editText.setText("Please input an even number");
            return;
        }
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setTitle("Confirm RTC Overwrite");
            alertDialogBuilder
                    .setMessage("Are you sure you want to set the RTC Time to " + Interval)
                    .setCancelable(true)
                    .setNegativeButton("NO", (dialog, id) -> {
                        dialog.dismiss();})
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.d(TAG, "yes OverwriteRTC");
                            bluetoothWriter.writeRTCInterval(Integer.parseInt(editText.getText().toString()));
                        }
                    });
        confirmRTCAlertDialog = alertDialogBuilder.create();
        confirmRTCAlertDialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mQueryBluetoothSubscription != null && !mQueryBluetoothSubscription.isUnsubscribed()) {
            mQueryBluetoothSubscription.unsubscribe();
        }
        if (mQueryBluetoothObservable != null) {
            mQueryBluetoothObservable.unsubscribeOn(AndroidSchedulers.mainThread());
        }
        if (mQueryNetworkSubscription != null && !mQueryNetworkSubscription.isUnsubscribed()) {
            mQueryNetworkSubscription.unsubscribe();
        }
        if (mQueryNetworkObservable != null) {
            mQueryNetworkObservable.unsubscribeOn(AndroidSchedulers.mainThread());
        }
        if (mQueryOtherSubscription != null && !mQueryOtherSubscription.isUnsubscribed()) {
            mQueryOtherSubscription.unsubscribe();
        }
        if (mQueryOtherObservable != null) {
            mQueryOtherObservable.unsubscribeOn(AndroidSchedulers.mainThread());
        }
    }

    public void setupLogging() {

        mDebugMessageAdapter = new LocalDebugMessageStorage(this);

        // bluetooth
        View testBluetoothLogButton = findViewById(R.id.logBluetooth);
        testBluetoothLogButton.setOnClickListener(v ->
                Logger.getInstance().logV("TEST", "Bluetooth test", DebugMessage.TYPE_BLUETOOTH));

        TextView bluetoothLogs = ViewUtils.findView(this, R.id.debugLogsBluetooth);

        View toggleBluetoothLogs = findViewById(R.id.debugMessageToggleBluetooth);
        toggleBluetoothLogs.setOnClickListener(v -> ViewUtils.setGone(bluetoothLogs, !ViewUtils.isVisible(bluetoothLogs)));

        mQueryBluetoothObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_BLUETOOTH);

        // network
        View testNetworkLogButton = findViewById(R.id.logNetwork);
        testNetworkLogButton.setOnClickListener(v ->
                Logger.getInstance().logV("TEST", "Network test", DebugMessage.TYPE_NETWORK));

        TextView networkLogs = ViewUtils.findView(this, R.id.debugLogsNetwork);

        View toggleNetworkLogs = findViewById(R.id.debugLogToggleNetwork);
        toggleNetworkLogs.setOnClickListener(v -> ViewUtils.setGone(networkLogs, !ViewUtils.isVisible(networkLogs)));

        mQueryNetworkObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_NETWORK);

        // other
        View testOtherLogButton = findViewById(R.id.logOther);
        testOtherLogButton.setOnClickListener(v ->
                Logger.getInstance().logV("TEST", "Other test", DebugMessage.TYPE_OTHER));

        TextView otherLogs = ViewUtils.findView(this, R.id.debugLogsOther);

        View toggleOtherLogs = findViewById(R.id.debugMessageToggleOther);
        toggleOtherLogs.setOnClickListener(v -> ViewUtils.setGone(otherLogs, !ViewUtils.isVisible(otherLogs)));

        mQueryOtherObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_OTHER);

        Button enableButton = ViewUtils.findView(this, R.id.debugEnableLogs);
        enableButton.setText("LOGS: " + mLogsEnabled);
        enableButton.setOnClickListener(v -> {
            if (mLogsEnabled) {
                mQueryBluetoothSubscription.unsubscribe();
                mQueryNetworkSubscription.unsubscribe();
                mQueryOtherSubscription.unsubscribe();
            } else {
                mQueryBluetoothSubscription = mQueryBluetoothObservable.subscribe(query -> {
                    Cursor cursor = query.run();
                    writeLogs(cursor, bluetoothLogs);
                });
                mQueryNetworkSubscription = mQueryNetworkObservable.subscribe(query -> {
                    Cursor cursor = query.run();
                    writeLogs(cursor, networkLogs);
                });
                mQueryOtherSubscription = mQueryOtherObservable.subscribe(query -> {
                    Cursor cursor = query.run();
                    writeLogs(cursor, otherLogs);
                });
            }
            mLogsEnabled = !mLogsEnabled;
            enableButton.setText("LOGS: " + mLogsEnabled);
        });
    }

    private void writeLogs(Cursor cursor, TextView logView) {
        if (cursor != null && cursor.moveToFirst()) {
            StringBuilder stringBuilder = new StringBuilder();
            Calendar calendar = Calendar.getInstance();

            do {
                DebugMessage debugMessage = DebugMessage.fromCursor(cursor);

                calendar.setTimeInMillis((long)debugMessage.getTimestamp()*1000);
                stringBuilder.append("\n");
                stringBuilder.append(DateTimeFormatUtil.format(calendar, DateTimeFormatUtil.TIMESTAMP_FORMAT));
                stringBuilder.append(": ");
                stringBuilder.append(debugMessage.getMessage());
                stringBuilder.append("\n");
            } while (cursor.moveToNext());

            logView.setText(stringBuilder.toString());

            cursor.close();
        }
    }

    @Override
    public void onSearchingForDevice() {

    }

    @Override
    public void onDeviceReady(ReadyDevice readyDevice) {

    }

    @Override
    public void onDeviceDisconnected() {

    }

    @Override
    public void onDeviceVerifying() {

    }

    @Override
    public void onDeviceSyncing() {

    }

    @Override
    public void onGotSuportedPIDs(String value) {
        Log.d(TAG, "onGotSupportedPID");
        editText.setText(value);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bluetoothConnectionObservable != null){
            bluetoothConnectionObservable.unsubscribe(this);
        }
    }
}