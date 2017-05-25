package com.pitstop.ui;

import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.database.LocalDatabaseHelper;
import com.pitstop.database.LocalDebugMessageAdapter;
import com.pitstop.models.DebugMessage;
import com.pitstop.utils.DateTimeFormatUtil;
import com.pitstop.utils.LogUtils;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.ViewUtils;
import com.pitstop.view.TextFeedView;
import com.squareup.sqlbrite.QueryObservable;

import java.util.Calendar;

import rx.Subscription;
import rx.schedulers.Schedulers;

public abstract class DebugDrawerActivity extends AppCompatActivity {

    private NetworkHelper mNetworkHelper;

    private DrawerLayout mDrawerLayout;

    // for logging
    private LocalDebugMessageAdapter mDebugMessageAdapter;
    private QueryObservable mQueryBluetoothObservable;
    private Subscription mQueryBluetoothSubscription;
    private QueryObservable mQueryNetworkObservable;
    private Subscription mQueryNetworkSubscription;
    private QueryObservable mQueryOtherObservable;
    private Subscription mQueryOtherSubscription;

    @Override
    public void setContentView(@LayoutRes int layoutResID) {
        setContentView(getLayoutInflater().inflate(layoutResID, mDrawerLayout, false));
    }

    @Override
    public void setContentView(View view) {
        if (true) {
            mDrawerLayout.addView(view, 0);
        } else {
            super.setContentView(view);
        }
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (true) {
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

        if (true){
            return;
        }

        mNetworkHelper = new NetworkHelper(getApplicationContext());

        mDrawerLayout = (DrawerLayout) getLayoutInflater().inflate(R.layout.activity_debug_drawer, null);
        super.setContentView(mDrawerLayout);

        View clearPrefsButton = findViewById(R.id.debugClearPrefs); // Only default prefs for now
        clearPrefsButton.setOnClickListener(v -> {
            PreferenceManager.getDefaultSharedPreferences(this).edit().clear().apply();
            Toast.makeText(this, "Preferences Cleared", Toast.LENGTH_SHORT).show();
        });

        View clearDbButton = findViewById(R.id.debugClearDB);
        clearDbButton.setOnClickListener(v -> {
            LocalDatabaseHelper databaseHelper = LocalDatabaseHelper.getInstance(this);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            databaseHelper.onUpgrade(db, 0, 0);
            db.close();
            Toast.makeText(this, "Database Cleared", Toast.LENGTH_SHORT).show();
        });

        EditText vinField = ViewUtils.findView(mDrawerLayout, R.id.debugVinField);
        View vinButton = findViewById(R.id.debugRandomVin);
        vinButton.setOnClickListener(v -> mNetworkHelper.getRandomVin(
                (response, requestError) -> {
                    vinField.setText(requestError == null ? response : "error: " + requestError.getMessage());
                })
        );
        setupLogging();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mQueryBluetoothSubscription != null && !mQueryBluetoothSubscription.isUnsubscribed()) {
            mQueryBluetoothSubscription.unsubscribe();
        }
        if (mQueryBluetoothObservable != null) {
            mQueryBluetoothObservable.unsubscribeOn(Schedulers.io());
        }
        if (mQueryNetworkSubscription != null && !mQueryNetworkSubscription.isUnsubscribed()) {
            mQueryNetworkSubscription.unsubscribe();
        }
        if (mQueryNetworkObservable != null) {
            mQueryNetworkObservable.unsubscribeOn(Schedulers.io());
        }
        if (mQueryOtherSubscription != null && !mQueryOtherSubscription.isUnsubscribed()) {
            mQueryOtherSubscription.unsubscribe();
        }
        if (mQueryOtherObservable != null) {
            mQueryOtherObservable.unsubscribeOn(Schedulers.io());
        }
    }

    public void setupLogging() {

        mDebugMessageAdapter = new LocalDebugMessageAdapter(this);

        // bluetooth
        View testBluetoothLogButton = findViewById(R.id.logBluetooth);
        testBluetoothLogButton.setOnClickListener(v ->
                LogUtils.debugLogV("TEST", "Bluetooth test", false, DebugMessage.TYPE_BLUETOOTH, this));

        TextFeedView bluetoothLogs = ViewUtils.findView(this, R.id.debugLogsBluetooth);

        View toggleBluetoothLogs = findViewById(R.id.debugMessageToggleBluetooth);
        toggleBluetoothLogs.setOnClickListener(v -> ViewUtils.setGone(bluetoothLogs, !ViewUtils.isVisible(bluetoothLogs)));

        mQueryBluetoothObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_BLUETOOTH);
        mQueryBluetoothSubscription = mQueryBluetoothObservable.subscribe(query -> {
            bluetoothLogs.clearText();
            Calendar calendar = Calendar.getInstance();
            query.asRows(DebugMessage::fromCursor).subscribe(debugMessage -> {
                calendar.setTimeInMillis(debugMessage.getTimestamp());
                bluetoothLogs.addLine(DateTimeFormatUtil.format(calendar, DateTimeFormatUtil.TIMESTAMP_FORMAT)
                        + ": " + debugMessage.getMessage());
            });
        });

        // network
        View testNetworkLogButton = findViewById(R.id.logNetwork);
        testNetworkLogButton.setOnClickListener(v ->
                LogUtils.debugLogV("TEST", "Network test", false, DebugMessage.TYPE_NETWORK, this));

        TextFeedView networkLogs = ViewUtils.findView(this, R.id.debugLogsNetwork);

        View toggleNetworkLogs = findViewById(R.id.debugLogToggleNetwork);
        toggleNetworkLogs.setOnClickListener(v -> ViewUtils.setGone(networkLogs, !ViewUtils.isVisible(networkLogs)));

        mQueryNetworkObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_NETWORK);
        mQueryNetworkSubscription = mQueryNetworkObservable.subscribe(query -> {
            networkLogs.clearText();
            Calendar calendar = Calendar.getInstance();
            query.asRows(DebugMessage::fromCursor).subscribe(debugMessage -> {
                calendar.setTimeInMillis(debugMessage.getTimestamp());
                networkLogs.addLine(DateTimeFormatUtil.format(calendar, DateTimeFormatUtil.TIMESTAMP_FORMAT)
                        + ": " + debugMessage.getMessage());
            });
        });

        // other
        View testOtherLogButton = findViewById(R.id.logOther);
        testOtherLogButton.setOnClickListener(v ->
                LogUtils.debugLogV("TEST", "Other test", false, DebugMessage.TYPE_OTHER, this));

        TextFeedView otherLogs = ViewUtils.findView(this, R.id.debugLogsOther);

        View toggleOtherLogs = findViewById(R.id.debugMessageToggleOther);
        toggleOtherLogs.setOnClickListener(v -> ViewUtils.setGone(otherLogs, !ViewUtils.isVisible(otherLogs)));

        mQueryOtherObservable = mDebugMessageAdapter.getQueryObservable(DebugMessage.TYPE_OTHER);
        mQueryOtherSubscription = mQueryOtherObservable.subscribe(query -> {
            otherLogs.clearText();
            Calendar calendar = Calendar.getInstance();
            query.asRows(DebugMessage::fromCursor).subscribe(debugMessage -> {
                calendar.setTimeInMillis(debugMessage.getTimestamp());
                otherLogs.addLine(DateTimeFormatUtil.format(calendar, DateTimeFormatUtil.TIMESTAMP_FORMAT)
                        + ": " + debugMessage.getMessage());
            });
        });
    }

}
