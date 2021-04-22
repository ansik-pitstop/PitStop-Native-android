package com.pitstop.ui;

import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import com.pitstop.BuildConfig;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventSourceImpl;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothWriter;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;
import com.pitstop.database.LocalAlarmStorage;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalDebugMessageStorage;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerTempNetworkComponent;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.TempNetworkComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.get.GetUserCarUseCase;
import com.pitstop.interactors.update.UpdateCarMileageUseCase;
import com.pitstop.models.Car;
import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.models.ReadyDevice;
import com.pitstop.network.RequestError;
import com.pitstop.observer.BluetoothConnectionObservable;
import com.pitstop.observer.BluetoothConnectionObserver;
import com.pitstop.repositories.Repository;
import com.pitstop.ui.main_activity.MainActivity;
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

    private boolean mLogsEnabled;
    EditText editText;

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

//        if (BuildConfig.BUILD_TYPE.equals(BuildConfig.BUILD_TYPE_RELEASE)){
//            return;
//        }

        TempNetworkComponent tempNetworkComponent = DaggerTempNetworkComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        UseCaseComponent useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule(this))
                .build();

        mNetworkHelper = tempNetworkComponent.networkHelper();

        localAlarmStorage = new LocalAlarmStorage(LocalDatabaseHelper.getInstance(getApplicationContext()));
        mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
        super.setContentView(mDrawerLayout);

        View mainActivityLayout = (View) findViewById(R.id.main_activity_layout);
        if (!(this instanceof MainActivity)){
            mainActivityLayout.setVisibility(View.GONE);
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, findViewById(R.id.drawer_layout_garage));

        }

       editText = ViewUtils.findView(mDrawerLayout, R.id.debug_edit_text);
        Button getSupportedPids = ViewUtils.findView(mDrawerLayout, R.id.debugGetSupportedPids);
        getSupportedPids.setOnClickListener(v -> {
            if (getBluetoothConnectionObservable()!=null)
                getBluetoothConnectionObservable().getSupportedPids();
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
        resetDTC.setOnClickListener(view -> showConfirmResetDTCDialog());
        Button resetMemory = ViewUtils.findView(mDrawerLayout, R.id.debugClearDeviceMemory);

        resetMemory.setOnClickListener(view -> showConfirmResetMemoryDialog());
        View vinButton = findViewById(R.id.debugRandomVin);
        vinButton.setOnClickListener(v -> mNetworkHelper.getRandomVin(
                (response, requestError) -> editText.setText(requestError == null ? response : "error: " + requestError.getMessage()))
        );

        Button setChunkSizeButton = ViewUtils.findView(mDrawerLayout, R.id.debugSetNetworkChunkSize);
        setChunkSizeButton.setOnClickListener(view -> {
            try {
                int k = Integer.valueOf(editText.getText().toString());
                setChunkSize(k);
            }
            catch(NumberFormatException e){
                editText.setText("Make sure you input an integer.");
            }
        });

        Button getMileage = findViewById(R.id.getMileage);
        getMileage.setOnClickListener(v -> {
            Log.d(TAG,"getMileage()");
            useCaseComponent.getUserCarUseCase().execute(Repository.DATABASE_TYPE.REMOTE, new GetUserCarUseCase.Callback() {
                @Override
                public void onCarRetrieved(Car car, Dealership dealership, boolean isLocal) {
                    editText.setText("mileage: "+car.getTotalMileage());
                }

                @Override
                public void onNoCarSet(boolean isLocal) {
                    Toast.makeText(DebugDrawerActivity.this
                            ,"No car added",Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onError(RequestError error) {
                    Toast.makeText(DebugDrawerActivity.this
                            ,"Error: "+error.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        });

        Button updateMileage = findViewById(R.id.updateMileage);
        updateMileage.setOnClickListener(v -> {
            try{
                int num = Integer.valueOf(editText.getText().toString());
                useCaseComponent.updateCarMileageUseCase().execute(num
                        ,new EventSourceImpl(EventSource.SOURCE_DRAWER), new UpdateCarMileageUseCase.Callback() {
                    @Override
                    public void onMileageUpdated() {
                        Toast.makeText(DebugDrawerActivity.this
                                ,"Mileage updated",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onNoCarAdded() {
                        Toast.makeText(DebugDrawerActivity.this
                                ,"Error: no car added",Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(RequestError error) {
                        Toast.makeText(DebugDrawerActivity.this
                                ,"Error: "+error.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }catch(NumberFormatException e){
                editText.setText("Invalid mileage, try again");
            }
            Log.d(TAG,"updateMileage()");
        });

        ViewUtils.findView(mDrawerLayout, R.id.describeProtocol).setOnClickListener(view -> {
            if (getBluetoothConnectionObservable() != null)
                getBluetoothConnectionObservable().requestDescribeProtocol();
        });

        ViewUtils.findView(mDrawerLayout, R.id.request2141PID).setOnClickListener(view -> {
            if (getBluetoothConnectionObservable() != null)
                getBluetoothConnectionObservable().request2141PID();
        });

        ViewUtils.findView(mDrawerLayout, R.id.requestPendingDTC).setOnClickListener(view -> {
            if (getBluetoothConnectionObservable() != null)
                getBluetoothConnectionObservable().requestPendingDTC();
        });

        ViewUtils.findView(mDrawerLayout, R.id.requestStoredDTC).setOnClickListener(view -> {
            if (getBluetoothConnectionObservable() != null)
                getBluetoothConnectionObservable().requestStoredDTC();
        });

        ViewUtils.findView(mDrawerLayout, R.id.selectELMProtocol).setOnClickListener(view -> {
            if (getBluetoothWriter() != null){
                try{
                    int editTextValue = Integer.valueOf(editText.getText().toString());
                    if (editTextValue < 0 || editTextValue > 13){
                        editText.setText("Invalid input, only integers between 0-13 accepted");
                    }else{
                        boolean succeeded = getBluetoothWriter().requestSelectProtocol(ObdProtocols.values()[editTextValue]);
                        if (!succeeded)
                            editText.setText("Failed to write");
                        else editText.setText("Wrote successfully");
                    }
                }catch(NumberFormatException e){
                    editText.setText("Invalid input, only integers between 0-13 accepted");
                    e.printStackTrace();
                }

            }
        });

        setupLogging();
    }

    private void setChunkSize(int chunkSize) {
        if(getBluetoothWriter()!= null){
            getBluetoothWriter().setChunkSize(chunkSize);
        }
    }

    private void showConfirmResetDTCDialog() {
        if (getBluetoothWriter()==null)
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
                        getBluetoothWriter().clearDTCs();
                    }
                });
        confirmRTCAlertDialog = alertDialogBuilder.create();
        confirmRTCAlertDialog.show();

    }

    private void showConfirmResetMemoryDialog() {
        if (getBluetoothWriter()==null)
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
                        getBluetoothWriter().resetMemory();
                    }
                });
        confirmRTCAlertDialog = alertDialogBuilder.create();
        confirmRTCAlertDialog.show();
    }

    private void showRTCOverWriteCOnfirmDialog() {
        if (getBluetoothWriter()==null)
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
                            getBluetoothWriter().writeRTCInterval(Integer.parseInt(editText.getText().toString()));
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

        mDebugMessageAdapter = new LocalDebugMessageStorage(LocalDatabaseHelper.getInstance(getApplicationContext()));

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
        if (editText!=null)
            editText.setText(value);
    }

    @Override
    public void onConnectingToDevice() {

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (getBluetoothConnectionObservable() != null){
            getBluetoothConnectionObservable().unsubscribe(this);
        }
    }

    @Override
    public void onGotPid(PidPackage pidPackage){
        Log.d(TAG,"onGotPid");
    }

    public abstract BluetoothConnectionObservable getBluetoothConnectionObservable();
    public abstract BluetoothWriter getBluetoothWriter();
}