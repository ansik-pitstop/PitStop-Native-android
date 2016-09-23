package com.pitstop.ui;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.adapters.TestActionAdapter;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothAutoConnectService.State;
import com.pitstop.models.TestAction;
import com.pitstop.utils.MessageListener;
import com.pitstop.utils.ShadowTransformer;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by BEN! on 15/9/2016.
 */
public class MainActivity extends AppCompatActivity implements MessageListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    public static final String TEST_ACTION_ACTION = "com.ansik.pitstop.TEST_ACTION_ACTION";
    public static final String TEST_ACTION_TYPE = "com.ansik.pitstop.action_type";

    @BindView(R.id.viewPager)
    ViewPager viewPager;
    private TestActionAdapter adapter;
    private ShadowTransformer shadowTransformer;

    @BindView(R.id.disconnectedBackground)
    View disconnectBackground;
    @BindView(R.id.connectCard)
    View connectCard;
    @BindView(R.id.logoLayout)
    View logoLayout;
    @BindView(R.id.logText)
    TextView logTextView;
    @BindView(R.id.logView)
    View logView;

    private boolean connected = false;

    private ArrayList<TestAction> testActions = new ArrayList<>();

    private BluetoothAutoConnectService bluetoothService;
    private Intent serviceIntent;

    private StringBuilder logText = new StringBuilder();

    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private ProgressDialog progressDialog;

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

        ButterKnife.bind(this);

        logTextView.setMovementMethod(new ScrollingMovementMethod());

        adapter = new TestActionAdapter(this, testActions);
        shadowTransformer = new ShadowTransformer(viewPager, adapter);

        shadowTransformer.enableScaling(true);

        viewPager.setPageTransformer(false, shadowTransformer);
        viewPager.setAdapter(adapter);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Connecting to device...");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);

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
        bluetoothService.disconnectFromDevice();
        unbindService(serviceConnection);
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

    // callback for OBD function result
    @Override
    public void processMessage(int status, State state, String message) {
        Log.i(TAG, "Received message: " + message + ", status: " + status);
        logText.append(message);
        logText.append("\n");
        logTextView.setText(logText);
        if(status == MessageListener.STATUS_SUCCESS) {
            if(state == State.VERIFY_RTC || state == State.GET_RTC) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 1);
                viewPager.setCurrentItem(2);
            } else if(state == State.READ_PIDS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 2);
                viewPager.setCurrentItem(3);
            } else if(state == State.READ_DTCS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 3);
                viewPager.setCurrentItem(4);
            } else if(state == State.GET_VIN) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 4);
                viewPager.setCurrentItem(5);
            }
        } else if(status == STATUS_FAILED) {
            if(state == State.VERIFY_RTC || state == State.GET_RTC) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 1);
                viewPager.setCurrentItem(2);
            } else if(state == State.READ_PIDS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 2);
                viewPager.setCurrentItem(3);
            } else if(state == State.READ_DTCS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 3);
                viewPager.setCurrentItem(4);
            } else if(state == State.GET_VIN) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 4);
                viewPager.setCurrentItem(5);
            }
        }
    }

    @Override
    public void connectSuccess() {
        if(connected) {
            return;
        }
        progressDialog.hide();
        connected = true;
        connectCard.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
            @Override
            public void run() {
                disconnectBackground.animate().alpha(0f).setDuration(500);
                connectCard.setVisibility(View.GONE);
                viewPager.animate().alpha(1f).setDuration(500);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = this.getWindow();
            window.setStatusBarColor(getResources().getColor(R.color.highlight));
        }
        viewPager.animate().alpha(0f).setDuration(0);
        viewPager.setVisibility(View.VISIBLE);
    }

    public void connectToDevice(View view) {
        bluetoothService.startBluetoothSearch();
        progressDialog.show();
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
                        logView.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                viewPager.animate().alpha(0f).setDuration(500).translationY(0).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        logoLayout.animate().alpha(1f).setDuration(500).withEndAction(new Runnable() {
                                            @Override
                                            public void run() {
                                                connectCard.setVisibility(View.VISIBLE);
                                                connectCard.animate().alpha(1f).setDuration(500);
                                                disconnectBackground.animate().alpha(1f).setDuration(500);
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                                    Window window = MainActivity.this.getWindow();
                                                    window.setStatusBarColor(getResources().getColor(R.color.primary_dark_dark));
                                                }
                                            }
                                        }).start();
                                        logView.setVisibility(View.GONE);
                                        viewPager.setVisibility(View.GONE);
                                    }
                                });
                            }
                        }).start();
                        bluetoothService.disconnectFromDevice();
                        connected = false;
                        break;
                    case CHECK_TIME:
                        Toast.makeText(context, "Check Time", Toast.LENGTH_SHORT).show();
                        bluetoothService.getObdDeviceTime();
                        logoLayout.animate().alpha(0f).setDuration(500).withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                viewPager.animate().y(30).setDuration(500).withEndAction(new Runnable() {
                                    @Override
                                    public void run() {
                                        logView.setVisibility(View.VISIBLE);
                                        logView.animate().alpha(1f).setDuration(500).start();
                                    }
                                }).start();
                            }
                        }).start();
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
