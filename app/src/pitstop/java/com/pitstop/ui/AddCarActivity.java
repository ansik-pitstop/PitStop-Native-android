package com.pitstop.ui;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pitstop.database.LocalScannerAdapter;
import com.pitstop.models.Car;
import com.pitstop.models.ObdScanner;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.ui.addCarFragments.AddCar1Fragment;
import com.pitstop.ui.addCarFragments.AddCar2NoDongleFragment;
import com.pitstop.ui.addCarFragments.AddCar2YesDongleFragment;
import com.pitstop.ui.addCarFragments.AddCarChooseDealershipFragment;
import com.pitstop.ui.addCarFragments.AddCarMileageDialog;
import com.pitstop.ui.addCarFragments.AddCarUtils;
import com.pitstop.R;
import com.pitstop.adapters.AddCarViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.addCarFragments.AddCarViewPager;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.BSAbstractedFragmentActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David on 7/20/2016.
 */
public class AddCarActivity extends BSAbstractedFragmentActivity
        implements AddCarUtils.AddCarUtilsCallback {

    private final String TAG = AddCarActivity.class.getSimpleName();

    // extras
    public static final String EXTRA_PAIR_PENDING = "com.pitstop.ui.AddCarActivity.extra_pair_pending";

    // activity result
    public static int ADD_CAR_SUCCESS = 51;
    public static int PAIR_CAR_SUCCESS = 52;

    public static boolean addingCar = false;

    AddCarViewPager mPager;
    private AddCarViewPagerAdapter mPagerAdapter;
    private ProgressDialog progressDialog;
    private NetworkHelper networkHelper;
    private MixpanelHelper mixpanelHelper;
    private AddCarUtils addCarUtils;

    /**
     * <p>This variable is used to keep track of if user is adding car with device.</p>
     * <p>This variable's value is <b>written</b> when:<br>
     * 1.When the user tap NO when being asked if he has device, the value will be false.<br>
     * 2.When the user tap YES when being asked if he has device, the value will be true.</p>
     * <p>
     * This variable's value is <b>read</b> in:<br>
     * 1.AddCarActivity and AddCarUtils, to write logs in Mixpanel<br>
     * 2.BluetoothAutoConnectService, BluetoothClassicComm and BluetoothLeComm, to determine whether if we should connect to
     * the detected IDD prefix device<br>
     */
    public static boolean addingCarWithDevice = false;

    private boolean carSuccessfullyAdded = false;

    public static boolean isPairingUnrecognizedDevice = false;

    private BroadcastReceiver bluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                Log.i(TAG, "Bond state changed: " + intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0));
                if (intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, 0) == BluetoothDevice.BOND_BONDED) {
                    searchForCar(null);
                }
            }
        }
    };

    private LocalScannerAdapter localScannerAdapter;

    private class CarListAdapter extends BaseAdapter {
        private List<Car> ownedCars;

        public CarListAdapter(List<Car> cars) {
            ownedCars = cars;
        }

        @Override
        public int getCount() {
            return ownedCars.size();
        }

        @Override
        public Object getItem(int position) {
            return ownedCars.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = getLayoutInflater();
            View rowView = convertView != null ? convertView :
                    inflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);
            Car ownedCar = (Car) getItem(position);

            TextView carName = (TextView) rowView.findViewById(android.R.id.text1);
            carName.setText(String.format("%s %s", ownedCar.getMake(), ownedCar.getModel()));
            return rowView;
        }
    }

    private boolean selectCarDialogShowing = false;

    public MixpanelHelper getMixpanelHelper() {
        return mixpanelHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car_fragmented);

        isPairingUnrecognizedDevice = getIntent().getBooleanExtra(EXTRA_PAIR_PENDING, false);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        networkHelper = new NetworkHelper(getApplicationContext());
        addCarUtils = new AddCarUtils((GlobalApplication) getApplicationContext(), this);

        localScannerAdapter = new LocalScannerAdapter(this);

        //setup view pager
        mPager = (AddCarViewPager) findViewById(R.id.add_car_view_pager);
        mPagerAdapter = new AddCarViewPagerAdapter(getSupportFragmentManager(), this);

        if (isPairingUnrecognizedDevice) {
            addingCarWithDevice = true;
            mPagerAdapter.addFragment(AddCar2YesDongleFragment.class, "Pair Scanner", 0);
            ((TextView) findViewById(R.id.step_text)).setText("Pair Scanner");
        } else {
            mPagerAdapter.addFragment(AddCar1Fragment.class, "STEP 1/3", 0);
        }
        mPager.setAdapter(mPagerAdapter);
        setupUIReferences();

        // Mixpanel time event
        mixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_ADD_CAR);
    }

    private void setupUIReferences() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setCanceledOnTouchOutside(false);
        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                addCarUtils.cancelMashape();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == 0) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();

        } else {
            // Otherwise, select the previous step.
            ((TextView) findViewById(R.id.step_text)).setText("STEP " + Integer.toString(mPager.getCurrentItem()) + "/3");
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }

        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_BACK, MixpanelHelper.ADD_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Invoked when the user tapped the "SELECT DEALERSHIP" in step 3
     *
     * @param view Select dealership button in Step 3
     */
    public void selectDealershipClicked(View view) {
        Fragment fragment = mPagerAdapter.getItem(2);
        if (fragment != null && fragment instanceof AddCarChooseDealershipFragment) {
            AddCarChooseDealershipFragment addCarChooseDealershipFragment = (AddCarChooseDealershipFragment) fragment;
            if (addCarChooseDealershipFragment.getShop() == null) {
                Toast.makeText(this, "No dealership was selected", Toast.LENGTH_SHORT).show();
                return;
            }

            addCarUtils.setDealership(addCarChooseDealershipFragment.getShop());
            addCarUtils.saveCarToServer();

            try { // Log in Mixpanel
                JSONObject properties = new JSONObject();
                properties.put("Button", "Selected " + ((AddCarChooseDealershipFragment) fragment).getShop().getName())
                        .put("View", MixpanelHelper.ADD_CAR_SELECT_DEALERSHIP_VIEW)
                        .put("Car", addCarUtils.getPendingCar().getMake() + " " + addCarUtils.getPendingCar().getModel());
                mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Invoked when the user tapped the "Yes" in step 1
     *
     * @param view The "Yes" button
     */
    public void yesDongleClicked(View view) {

        addingCarWithDevice = true;

        try { // Log in Mixpanel
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE, MixpanelHelper.ADD_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mPagerAdapter.addFragment(AddCar2YesDongleFragment.class, "YesDongle", 1);
        ((TextView) findViewById(R.id.step_text)).setText("STEP 2/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(1);
    }

    /**
     * Invoked when the user tapped the "No" in step 1
     *
     * @param view The "No" button
     */
    public void noDongleClicked(View view) {

        addingCarWithDevice = false;

        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_NO_HARDWARE, MixpanelHelper.ADD_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle", 1);
        ((TextView) findViewById(R.id.step_text)).setText("STEP 2/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(1);
    }

    /**
     * Invoked when the "SEARCH FOR VEHICLE" or "Add Vehicle" button is tapped in the second step of the add car process
     *
     * @param view the "Search for vehicle"/"Add vehicle" button
     */
    public void searchForCar(View view) {

        if (isPairingUnrecognizedDevice) {
            addCarUtils.searchForUnrecognizedDevice();
            return;
        }

        try { // Log in Mixpanel
            JSONObject properties = new JSONObject();
            properties.put("Button", addingCarWithDevice ? MixpanelHelper.ADD_CAR_YES_HARDWARE_ADD_VEHICLE : MixpanelHelper.ADD_CAR_NO_HARDWARE_ADD_VEHICLE);
            properties.put("View", MixpanelHelper.ADD_CAR_VIEW);
            properties.put("Method of Adding Car", addingCarWithDevice ? MixpanelHelper.ADD_CAR_METHOD_DEVICE : MixpanelHelper.ADD_CAR_METHOD_MANUAL);
            mixpanelHelper.trackCustom(MixpanelHelper.EVENT_BUTTON_TAPPED, properties);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (mPagerAdapter.getItem(1) == null) return;

        if (mPagerAdapter.getItem(1) instanceof AddCar2NoDongleFragment) { // If in the AddCar2NoDongleFragment
            EditText vinEditText = (EditText) findViewById(R.id.VIN);
            addCarUtils.setVin(vinEditText.getText().toString());
            if (addCarUtils.isValidVin()) {
                Log.i(TAG, "Searching for car");

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);

                if (!AddCarUtils.gotMileage) {
                    AddCarMileageDialog dialog = new AddCarMileageDialog();
                    dialog.setCallback(addCarUtils).show(getSupportFragmentManager(), "Input Milage");
                } else {
                    postMileageInput();
                }
            } else {
                hideLoading("Invalid VIN");
            }
        } else if (mPagerAdapter.getItem(1) instanceof AddCar2YesDongleFragment) { // If in the AddCar2YesDongleFragment
            Log.i(TAG, "Searching for car");

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
            if (!AddCarUtils.gotMileage) {
                AddCarMileageDialog dialog = new AddCarMileageDialog();
                dialog.setCallback(addCarUtils).show(getSupportFragmentManager(), "Input Milage");
            } else {
                postMileageInput();
            }
        }
    }

    private boolean checkBackCamera() {
        final int CAMERA_FACING_BACK = 0;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i, info);
            if (CAMERA_FACING_BACK == info.facing) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invoked when the "Scan VIN Barcode" button is tapped
     *
     * @param view the "Scan VIN Barcode" button
     */
    public void startScanner(View view) {
        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCAN_VIN_BARCODE, MixpanelHelper.ADD_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (!checkBackCamera()) {
            Toast.makeText(this, "This device does not have a back facing camera",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG, "Starting barcode scanner");

        IntentIntegrator barcodeScanner = new IntentIntegrator(this);
        barcodeScanner.setBeepEnabled(false);
        barcodeScanner.initiateScan();

        //When the Barcode Scanner view appears
        try {
            mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_BARCODE_SCANNER_VIEW_APPEARED);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        try {
            if (isPairingUnrecognizedDevice){
                mixpanelHelper.trackViewAppeared(MixpanelHelper.UNRECOGNIZED_MODULE_VIEW);
            } else {
                mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIEW);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        registerBluetoothReceiver();

        addingCar = true;

        // The result is by default cancelled
        setResult(RESULT_CANCELED);

        super.onResume();
    }

    @Override
    protected void onPause() {
        ((GlobalApplication) getApplicationContext()).getMixpanelAPI().flush();

        unregisterBluetoothReceiver();

        addingCar = false;

        //hideLoading();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        hideLoading(null);

        addingCarWithDevice = false;
        isPairingUnrecognizedDevice = false;

        if (serviceIsBound) {
            addCarUtils.unbindService();
        }

        addCarUtils.cancelMashape();

        if (carSuccessfullyAdded) {
            mixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_ADD_CAR);
        }

        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() != null) {
                    String VIN = result.getContents();
                    if (VIN.length() == 18) {
                        VIN = VIN.substring(1, 18);
                    }
                    try {
                        mixpanelHelper.trackCustom("Scanned VIN",
                                new JSONObject("{'VIN':'" + VIN + "','View':'Add Car'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    View vinField = findViewById(R.id.VIN);
                    if (vinField != null) {
                        ((EditText) vinField).setText(VIN);
                    }
                    Log.i(TAG, "Barcode read: " + VIN);
                    if (!AddCarUtils.isValidVin(VIN)) {
                        Toast.makeText(AddCarActivity.this, "Invalid VIN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (requestCode == AddCarUtils.RC_PENDING_ADD_CAR) {
            Log.i(TAG, "Adding car from pending");
            showLoading("Adding car");
            addCarUtils.runVinTask();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void getBluetoothState(int state) {

    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    @Override
    public void hideLoading(String string) {
        if(progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if(string!=null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(String string) {
        progressDialog.setMessage(string);
        if(!progressDialog.isShowing()) {
            progressDialog.show();
        }
    }

    private void registerBluetoothReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothReceiver, filter);
    }

    private void unregisterBluetoothReceiver() {
        unregisterReceiver(bluetoothReceiver);
    }

    /**
     * From AddCarUtils.AddCarUtilsCallback
     */

    @Override
    public void carSuccessfullyAdded(Car car) {
        if (!addingCar) return;

        carSuccessfullyAdded = true; // At this point car is successfully added

        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, car);
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
        setResult(ADD_CAR_SUCCESS, data);

        new CountDownTimer(2000, 2000) { // to let issues populate in server
            @Override
            public void onTick(long millisUntilFinished) {}

            @Override
            public void onFinish() {
                hideLoading(null);
                finish();
            }
        }.start();
    }

    @Override
    public void resetScreen() {
        if (!addingCar) return;

        if (mPager.getCurrentItem() == 1) {
            if (findViewById(R.id.VIN) != null) {
                ((EditText) findViewById(R.id.VIN)).setText("");
            }
        } else if (mPager.getCurrentItem() == 2) {
            mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle", 1);
            mPager.setCurrentItem(1);
        }
    }

    @Override
    public void openRetryDialog() {
        if (!addingCar) return;

        hideLoading(null);

        if(isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Device not connected");

        // Alert message
        alertDialog.setMessage("Could not connect to device. " +
                "\n\nMake sure your vehicle engine is on and " +
                "OBD device is properly plugged in.\n\nYou may also try turning off the Bluetooth on your phone and then turning it back on.\n\nTry again ?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!isPairingUnrecognizedDevice) {
                    searchForCar(null);
                } else {
                    addCarUtils.searchForUnrecognizedDevice();
                }
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();

        // Mixpanel - Try to connect bluetooth again
        try {
            mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_BLUETOOTH_RETRY, MixpanelHelper.ADD_CAR_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public BluetoothAutoConnectService getAutoConnectService() {
        return autoConnectService;
    }

    @Override
    public void postMileageInput() {
        // After the user entered the mileage, search for car and get Vin
        addCarUtils.searchAndGetVin();
    }

    @Override
    public void askForDealership() {
        if (!addingCar) return;

        mPagerAdapter.addFragment(AddCarChooseDealershipFragment.class, "SelectDealership", 2);
        ((TextView) findViewById(R.id.step_text)).setText("STEP 3/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(2);

        // Go to the selectDealership fragment
        try {
            mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_SELECT_DEALERSHIP_VIEW);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * During pair car with device process, when the scanner does not return the VIN
     *
     * @param scannerName
     * @param scannerId
     */
    @Override
    public void showSelectCarDialog(final String scannerName, final String scannerId) {
        if (autoConnectService != null && !selectCarDialogShowing) {
            final CarListAdapter carListAdapter = new CarListAdapter(MainActivity.carList);
            final ArrayList<Car> selectedCar = new ArrayList<>(1);

            AnimatedDialogBuilder dialog = new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW);
            dialog.setCancelable(false)
                    .setTitle("Unrecognized module detected, please select the car this device is connected to:")
                    .setSingleChoiceItems(carListAdapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            selectedCar.clear();
                            selectedCar.add((Car) carListAdapter.getItem(which));
                        }
                    })
                    .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(final DialogInterface dialog, int which) {
                            if (selectedCar.isEmpty()) {
                                Toast.makeText(AddCarActivity.this, "Please pick a car!", Toast.LENGTH_SHORT).show();
                            } else if (localScannerAdapter.carHasDevice(selectedCar.get(0).getId())) {
                                Toast.makeText(AddCarActivity.this, "This car has scanner!", Toast.LENGTH_SHORT).show();
                            } else {
                                validateAndPostScanner(selectedCar.get(0), scannerId, scannerName);
                            }
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            selectCarDialogShowing = false;
                        }
                    });
            dialog.show();
            selectCarDialogShowing = true;
        }
    }

    /**
     * Show a dialog prompting user to pair the corresponding car
     *
     * @param existedCar The car retrieved from the backend whose user_id matches current user id
     * @param scannerName Current scanner name
     * @param scannerId Current scanner id
     */
    @Override
    public void pairCarWithDevice(final Car existedCar, final String scannerName, final String scannerId) {
        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Unrecognized device found")
                .setMessage("We have found a OBD bluetooth device " + scannerId + ", and it is connected to " +
                        "your " + existedCar.getYear() + " " + existedCar.getMake() + " " + existedCar.getModel() +
                        ". Do your want us to link the device with your car?")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        validateAndPostScanner(existedCar, scannerId, scannerName);
                    }
                }).show();
    }

    /**
     * 1. Check with the backend and see if scanner is valid <br>
     * 2. If so, create an association with the car selected and the scanner <br>
     * 3. On success, store scanner information locally, and finish.
     * @param car selected car or the car retrieved from the backend by its VIN
     * @param scannerId The current scannerId
     * @param scannerName The current scannerName
     */
    private void validateAndPostScanner(final Car car, final String scannerId, final String scannerName) {
        showLoading("Checking device ID..");
        networkHelper.validateScannerId(scannerId, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (requestError != null) {
                    pairCarError("Network error, please try again later");
                    mixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_NETWORK_ERROR);
                } else {
                    try {
                        JSONObject result = new JSONObject(response);
                        if (result.has("id")) { //invalid
                            Log.d(TAG, "DeviceID is not valid");
                            pairCarError("This device has been paired with another car.");
                            mixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_INVALID_ID);
                        } else {
                            showLoading("Valid scanner, saving...");
                            networkHelper.createNewScanner(car.getId(), scannerId, new RequestCallback() {
                                @Override
                                public void done(String response, RequestError requestError) {
                                    if (requestError != null) {
                                        // Error occurred during creating new scanner
                                        Log.d(TAG, "Create new scanner failed!");
                                        pairCarError("Network errors, please try again later");
                                        mixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_NETWORK_ERROR);
                                    } else {
                                        // Save locally
                                        ObdScanner scanner = new ObdScanner(
                                                car.getId(), scannerName, scannerId);
                                        localScannerAdapter.updateScannerByCarId(scanner);
                                        mixpanelHelper.trackDetectUnrecognizedModule(MixpanelHelper.UNRECOGNIZED_MODULE_PAIRING_SUCCESS);
                                        Intent data = new Intent();
                                        data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
                                        setResult(PAIR_CAR_SUCCESS, data);
                                        hideLoading("Finish");
                                        finish();
                                    }
                                }
                            });
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        pairCarError("Unknown error, please try again later");
                    }
                }
            }
        });
    }

    @Override
    public void pairCarError(String errorMessage) {
        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Error in pairing device")
                .setMessage(errorMessage)
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setPositiveButton("RETRY", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addCarUtils.searchForUnrecognizedDevice();
                    }
                }).show();
    }

}
