package com.pitstop.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;

import com.pitstop.R;
import com.pitstop.adapters.TestActionAdapter;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.BluetoothAutoConnectService.State;
import com.pitstop.models.TestAction;
import com.pitstop.utils.MessageListener;
import com.pitstop.utils.NetworkHelper;
import com.pitstop.utils.ShadowTransformer;
import com.pitstop.utils.TestTimer;

import java.util.ArrayList;
import java.util.List;

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

    // Before connect
    @BindView(R.id.cardTitle)
    TextView cardTitle;
    @BindView(R.id.cardDescription)
    TextView cardDescription;
    @BindView(R.id.connectButton)
    Button connectButton;
    @BindView(R.id.setupButton)
    Button setupButton;

    private boolean connected = false;

    private ArrayList<TestAction> testActions = new ArrayList<>();

    private BluetoothAutoConnectService bluetoothService;
    private Intent serviceIntent;

    private StringBuilder logText = new StringBuilder();

    public static final int RC_LOCATION_PERM = 101;
    public static final String[] LOC_PERMS = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    private ProgressDialog progressDialog;
    private boolean isLoading = false;


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
        viewPager.setOffscreenPageLimit(6);

        progressDialog = new ProgressDialog(this);
        progressDialog.setCancelable(true);
        progressDialog.setCanceledOnTouchOutside(false);

        serviceIntent = new Intent(MainActivity.this, BluetoothAutoConnectService.class);
        startService(serviceIntent);

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 1);
        }
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
        if (connectTimer != null) {
            connectTimer.cancel();
        }
        unbindService(serviceConnection);
        try {
            unregisterReceiver(testActionReceiver);
        } catch (Exception e) {
            // not registered
        }
    }

    @Override
    public void onBackPressed() {
        if (connectButton.getVisibility() == View.VISIBLE) {
            connectButton.setVisibility(View.GONE);
            setupButton.setVisibility(View.VISIBLE);
            cardTitle.setText(getString(R.string.start_dialog_setup_title));
            cardDescription.setText(getString(R.string.start_dialog_setup_description));
        } else {
            super.onBackPressed();
        }
    }

    private void initializeTestActions() {
        testActions.add(new TestAction("Device Time", getString(R.string.test_card_device_time), TestAction.Type.CHECK_TIME));
        testActions.add(new TestAction("Get VIN", getString(R.string.test_card_get_vin), TestAction.Type.VIN));
        testActions.add(new TestAction("Sensor Data", getString(R.string.test_card_sensor_data), TestAction.Type.PID));
        testActions.add(new TestAction("Engine Codes", getString(R.string.test_card_engine_codes), TestAction.Type.DTC));
        testActions.add(new TestAction("Collect Data", getString(R.string.test_card_collect_data), TestAction.Type.COLLECT_DATA));
        testActions.add(new TestAction("End Test", getString(R.string.test_card_end_test), TestAction.Type.RESET));
    }

    // callback for OBD function result
    @Override
    public void processMessage(int status, State state, String message) {
        Log.i(TAG, "Received message: " + message + ", status: " + status);
        logText.append(message);
        logText.append("\n");
        logTextView.setText(logText);
        if (status == MessageListener.STATUS_SUCCESS) {
            if (state == State.VERIFY_RTC || state == State.GET_RTC) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 0);
                viewPager.setCurrentItem(1);
            } else if (state == State.GET_VIN) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 1);
                viewPager.setCurrentItem(2);
            } else if (state == State.READ_PIDS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 2);
                viewPager.setCurrentItem(3);
            } else if (state == State.READ_DTCS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 3);
                viewPager.setCurrentItem(4);
                showLoading("Hold on, we are collecting data...");
            } else if (state == State.COLLECT_DATA) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(true, 4);
                viewPager.setCurrentItem(5);
                viewPager.setOnTouchListener(null);
                hideLoading();
            }

        } else if (status == STATUS_FAILED) {
            if (state == State.VERIFY_RTC || state == State.GET_RTC) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 0);
                viewPager.setCurrentItem(1);
            } else if (state == State.GET_VIN) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 1);
                viewPager.setCurrentItem(2);
            } else if (state == State.READ_PIDS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 2);
                viewPager.setCurrentItem(3);
            } else if (state == State.READ_DTCS) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 3);
                viewPager.setCurrentItem(4);
                showLoading("Hold on, we are collecting data...");
            } else if (state == State.COLLECT_DATA) {
                ((TestActionAdapter) viewPager.getAdapter()).updateItem(false, 4);
                viewPager.setCurrentItem(5);
                viewPager.setOnTouchListener(null);
                hideLoading();
            }
        } else if (status == STATUS_UPDATE) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void connectSuccess() {
        if (connected) {
            return;
        }
        connectTimer.cancel();
        hideLoading();
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

    private CountDownTimer connectTimer;
    private int connectAttempts = 0;


    /**
     * Onclick method for setup button
     *
     * @param view
     */
    public void startSetup(View view) {

        final View dialogTitle = getLayoutInflater().inflate(R.layout.dialog_setup_title, null);
        final View dialogBody = getLayoutInflater().inflate(R.layout.dialog_setup_layout, null);
        final TextInputEditText user = (TextInputEditText) dialogBody.findViewById(R.id.input_user);
        final RecyclerView recyclerView = (RecyclerView) dialogBody.findViewById(R.id.list_failures);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setHasFixedSize(true);

        final FailureListAdapter adapter = new FailureListAdapter();
        recyclerView.setAdapter(adapter);

        final AlertDialog setupDialog = new AlertDialog.Builder(this)
                .setCustomTitle(dialogTitle)
                .setView(dialogBody)
                .setPositiveButton("Confirm", null)
                .setNegativeButton("Cancel", null)
                .create();

        if (NetworkHelper.getUser() != null
                && !NetworkHelper.getUser().isEmpty()
                && !NetworkHelper.getUser().equals(getString(R.string.user_unknown))) {
            user.setText(NetworkHelper.getUser());
        }

        setupDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                setupDialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (user.getText().toString().isEmpty()) {
                            NetworkHelper.setUser(getString(R.string.user_unknown));
                        } else {
                            NetworkHelper.setUser(user.getText().toString());
                        }
                        NetworkHelper.setFailures(adapter.getSelectedFailures());
                        dialog.dismiss();

                        cardTitle.setText(getString(R.string.start_dialog_connect_title));
                        cardDescription.setText(R.string.start_dialog_connect_description);
                        setupButton.setVisibility(View.GONE);
                        connectButton.setVisibility(View.VISIBLE);
                    }
                });
            }
        });

        setupDialog.show();
    }

    /**
     * Onclick method for connect button
     *
     * @param view
     */
    public void connectToDevice(View view) {
        connectAttempts = 0;
        connectTimer = new CountDownTimer(14000, 14000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                if (++connectAttempts == 3 && !connected) {
                    hideLoading();
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                    alertDialog.setTitle("Could not connect to device");
                    alertDialog.setMessage("Could not connect to device. " +
                            "\n\nMake sure your vehicle engine is on and " +
                            "OBD device is properly plugged in.\n\nYou may also try turning off the Bluetooth on your phone and then turning it back on.\n\nTry again ?");
                    alertDialog.setCancelable(false);

                    alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connectToDevice(null);
                            showLoading("Connecting to device...");
                        }
                    });

                    alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });
                    alertDialog.show();
                } else {
                    searchForCar();
                }
            }
        };
        searchForCar();
        connectTimer.start();
        showLoading("Connecting to device...");
    }

    private void searchForCar() {
        bluetoothService.startBluetoothSearch();
        bluetoothService.updateState(State.CONNECTING);
        if (connectTimer != null) {
            connectTimer.start();
        }
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
                    case CHECK_TIME:
                        viewPager.setOnTouchListener(new View.OnTouchListener() {
                            @Override
                            public boolean onTouch(View v, MotionEvent event) {
                                return true;
                            }
                        });
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
                    case VIN:
                        Toast.makeText(context, "VIN", Toast.LENGTH_SHORT).show();
                        break;
                    case PID:
                        Toast.makeText(context, "Sensor Data", Toast.LENGTH_SHORT).show();
                        break;
                    case DTC:
                        Toast.makeText(context, "Engine Codes", Toast.LENGTH_SHORT).show();
                        break;
                    case COLLECT_DATA:
                        Toast.makeText(context, "Collect Data", Toast.LENGTH_SHORT).show();
                        break;
                    case RESET:
                        Toast.makeText(context, "End Test, reset device and disconnect", Toast.LENGTH_SHORT).show();
                        bluetoothService.clearObdDataPackage();
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
                                                connectButton.setVisibility(View.GONE);
                                                setupButton.setVisibility(View.VISIBLE);
                                                cardTitle.setText(getString(R.string.start_dialog_setup_title));
                                                cardDescription.setText(getString(R.string.start_dialog_setup_description));
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
                                        viewPager.setCurrentItem(0);
                                    }
                                });
                            }
                        }).start();
                        bluetoothService.disconnectFromDevice();
                        bluetoothService.reset();
                        connected = false;
                        NetworkHelper.reset();
                        adapter.resetItems();
                        logText = new StringBuilder();
                        break;
                }
            }
        }
    };

    public void hideLoading() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        } else {
            progressDialog = new ProgressDialog(this);
            progressDialog.setCanceledOnTouchOutside(false);
        }
        isLoading = false;
    }

    public void showLoading(String text) {
        isLoading = true;
        if (progressDialog == null) {
            Log.d(TAG, "Progress dialog is null");
            return;
        }
        progressDialog.setMessage(text);
        if (!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    public class FailureListAdapter extends RecyclerView.Adapter<FailureListAdapter.FailureViewHolder> {

        List<String> mFailures;
        List<String> mSelectedFailures;

        public FailureListAdapter() {
            mFailures = new ArrayList<>();
            mSelectedFailures = new ArrayList<>();
            mFailures.add("Dirty Air Filter");
            mFailures.add("Bad Battery");
            mFailures.add("Faulty Spark Plug");
            mFailures.add("Engine Light On");
        }

        @Override
        public FailureViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.dialog_setup_failure_item, parent, false);
            return new FailureViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final FailureViewHolder holder, int position) {
            final String failure = mFailures.get(position);
            holder.failureTitle.setText(failure);
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.failureCheckBox.setChecked(!holder.failureCheckBox.isChecked());
                }
            });
            holder.failureCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        mSelectedFailures.add(failure);
                    } else if (mSelectedFailures.contains(failure)) {
                        mSelectedFailures.remove(failure);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            if (mFailures == null) return 0;
            return mFailures.size();
        }

        public List<String> getSelectedFailures() {
            return mSelectedFailures;
        }

        public class FailureViewHolder extends RecyclerView.ViewHolder {

            View rootView;
            CheckBox failureCheckBox;
            TextView failureTitle;

            public FailureViewHolder(View itemView) {
                super(itemView);
                rootView = itemView;
                failureTitle = (TextView) itemView.findViewById(R.id.failure_title);
                failureCheckBox = (CheckBox) itemView.findViewById(R.id.failure_checkbox);
            }
        }
    }

}
