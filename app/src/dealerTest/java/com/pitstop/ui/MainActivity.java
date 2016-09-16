package com.pitstop.ui;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;
import com.pitstop.adapters.TestActionAdapter;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.models.TestAction;
import com.pitstop.utils.MessageListener;
import com.pitstop.utils.ShadowTransformer;

import java.util.ArrayList;

/**
 * Created by BEN! on 15/9/2016.
 */
public class MainActivity extends AppCompatActivity implements MessageListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String TEST_ACTION_ACTION = "com.ansik.pitstop.TEST_ACTION_ACTION";
    public static final String TEST_ACTION_TYPE = "com.ansik.pitstop.action_type";

    private ViewPager viewPager;
    private TestActionAdapter adapter;
    private ShadowTransformer shadowTransformer;

    private View connectCard;

    private ArrayList<TestAction> testActions = new ArrayList<>();

    private BluetoothAutoConnectService bluetoothService;
    private Intent serviceIntent;

    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION};

    protected ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "connecting: onServiceConnection");

            bluetoothService = ((BluetoothAutoConnectService.BluetoothBinder) service).getService();
            bluetoothService.setCallbacks(MainActivity.this);

            // Send request to user to turn on bluetooth if disabled
            if (BluetoothAdapter.getDefaultAdapter() != null) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[0]) != PackageManager.PERMISSION_GRANTED
                        || ContextCompat.checkSelfPermission(MainActivity.this, LOC_PERMS[1]) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, LOC_PERMS, RC_LOCATION_PERM);
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "Disconnecting: onServiceConnection");
            bluetoothService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeTestActions();

        registerReceiver(testActionReceiver, new IntentFilter(TEST_ACTION_ACTION));

        connectCard = findViewById(R.id.connectCard);
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        adapter = new TestActionAdapter(this, testActions);
        shadowTransformer = new ShadowTransformer(viewPager, adapter);

        shadowTransformer.enableScaling(true);

        viewPager.setPageTransformer(false, shadowTransformer);
        viewPager.setAdapter(adapter);

        serviceIntent = new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(testActionReceiver);
        } catch (Exception e) {
            // not registered
        }
    }

    private void initializeTestActions() {
        testActions.add(new TestAction("Disconnect", "Disconnect from the device.", TestAction.Type.DISCONNECT));
        testActions.add(new TestAction("Device Time", "The device time must be properly set before receiving data. " +
                "This may take up to a minute.", TestAction.Type.CHECK_TIME));
        testActions.add(new TestAction("Sensor Data", "Verify real-time sensor data is working. Received data will be displayed.", TestAction.Type.PID));
        testActions.add(new TestAction("Engine Codes", "Check the vehicle for any engine codes.", TestAction.Type.DTC));
        testActions.add(new TestAction("Get VIN", "Verify the VIN is retrievable.", TestAction.Type.VIN));
        testActions.add(new TestAction("Reset", "Reset the device.", TestAction.Type.RESET));
    }

    @Override
    public void processMessage(String message) {

    }

    public void connectToDevice(View view) {
        connectCard.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                connectCard.setVisibility(View.GONE);
                viewPager.animate().alpha(1f).setDuration(500);
            }
        });
        viewPager.animate().alpha(0f).setDuration(0);
        viewPager.setVisibility(View.VISIBLE);
        bluetoothService.startBluetoothSearch();
    }

    private BroadcastReceiver testActionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TEST_ACTION_ACTION)) {
                TestAction.Type type = (TestAction.Type) intent.getSerializableExtra(TEST_ACTION_TYPE);
                switch (type) {
                    case CONNECT:
                        Toast.makeText(context, "Connect", Toast.LENGTH_SHORT).show();
                        break;
                    case DISCONNECT:
                        Toast.makeText(context, "Disconnect", Toast.LENGTH_SHORT).show();
                        viewPager.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                viewPager.setVisibility(View.GONE);
                                connectCard.setVisibility(View.VISIBLE);
                                connectCard.animate().alpha(1f).setDuration(500);
                            }
                        });
                        break;
                    case CHECK_TIME:
                        Toast.makeText(context, "Check Time", Toast.LENGTH_SHORT).show();
                        break;
                    case PID:
                        Toast.makeText(context, "Sensor Data", Toast.LENGTH_SHORT).show();
                        break;
                    case DTC:
                        Toast.makeText(context, "Engine Codes", Toast.LENGTH_SHORT).show();
                        break;
                    case VIN:
                        Toast.makeText(context, "VIN", Toast.LENGTH_SHORT).show();
                        break;
                    case RESET:
                        Toast.makeText(context, "Reset", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    };
}
