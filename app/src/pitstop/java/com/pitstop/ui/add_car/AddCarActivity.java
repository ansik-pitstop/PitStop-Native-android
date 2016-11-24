package com.pitstop.ui.add_car;

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
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
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
import com.pitstop.models.Car;
import com.pitstop.ui.BasePresenter;
import com.pitstop.ui.MainActivity;
import com.pitstop.R;
import com.pitstop.adapters.AddCarViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.ui.add_car.view_fragment.AddCar1Fragment;
import com.pitstop.ui.add_car.view_fragment.AddCar2NoDongleFragment;
import com.pitstop.ui.add_car.view_fragment.AddCar2YesDongleFragment;
import com.pitstop.ui.add_car.view_fragment.AddCarChooseDealershipFragment;
import com.pitstop.ui.add_car.view_fragment.AddCarMileageDialog;
import com.pitstop.ui.add_car.view_fragment.AddCarViewPager;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.BSAbstractedFragmentActivity;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

/**
 * Created by David on 7/20/2016.
 */
public class AddCarActivity extends BSAbstractedFragmentActivity implements AddCarContract.View {

    private final String TAG = AddCarActivity.class.getSimpleName();

    // extras
    public static final String EXTRA_PAIR_PENDING = "com.pitstop.ui.add_car.AddCarActivity.extra_pair_pending";
    public static boolean isPairingUnrecognizedDevice = false;

    // activity result
    public static int ADD_CAR_SUCCESS = 51;
    public static int PAIR_CAR_SUCCESS = 52;
    public static int ADD_CAR_NO_DEALER_SUCCESS = 53;
    private static final int RC_PENDING_ADD_CAR = 1043;

    // views
    private AddCarViewPager mPager;
    private AddCarViewPagerAdapter mPagerAdapter;
    private ProgressDialog progressDialog;

    private MixpanelHelper mixpanelHelper;
    private AddCarPresenter mAddCarPresenter;

    public static boolean addingCar = false;
    public static boolean addingCarWithDevice = false;
    private boolean carSuccessfullyAdded = false;

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

    @Override
    public void setPresenter(BasePresenter presenter) {
        mAddCarPresenter = (AddCarPresenter) presenter;
    }

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car_fragmented);
        isPairingUnrecognizedDevice = getIntent().getBooleanExtra(EXTRA_PAIR_PENDING, false);

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        mAddCarPresenter = new AddCarPresenter(this, (GlobalApplication) getApplicationContext(), isPairingUnrecognizedDevice);

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
                mAddCarPresenter.cancelAllTimeouts(); // sounds like a bad idea
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (mPager.getCurrentItem() == AddCarViewPager.PAGE_FIRST) {
            // If the user is currently looking at the first step, allow the system to handle the
            // Back button. This calls finish() on this activity and pops the back stack.
            super.onBackPressed();

        } else if (mPager.getCurrentItem() == AddCarViewPager.PAGE_DEALERSHIP) {
            new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                    .setMessage("Are you sure you don't want to select a dealership? Selecting a dealership allows you book service appointment and " +
                            "message for support.")
                    .setNegativeButton("SELECT A DEALERSHIP", null)
                    .setPositiveButton("CONTINUE WITHOUT DEALERSHIP", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent data = new Intent();
                            data.putExtra(MainActivity.CAR_EXTRA, mAddCarPresenter.getCreatedCar());
                            data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
                            setResult(ADD_CAR_NO_DEALER_SUCCESS, data);
                            finish();
                        }
                    })
                    .show();
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

            mAddCarPresenter.updateCreatedCarDealership(addCarChooseDealershipFragment.getShop());

            try { // Log in Mixpanel
                JSONObject properties = new JSONObject();
                properties.put("Button", "Selected " + ((AddCarChooseDealershipFragment) fragment).getShop().getName())
                        .put("View", MixpanelHelper.ADD_CAR_SELECT_DEALERSHIP_VIEW)
                        .put("Car", mAddCarPresenter.getPendingCar().getMake() + " " + mAddCarPresenter.getPendingCar().getModel());
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
     * Invoked when the "SEARCH FOR VEHICLE" or "Add Vehicle" button is tapped in the step 2 of the add car process
     *
     * @param view the "Search for vehicle"/"Add vehicle" button
     */
    public void searchForCar(View view) {

        if (isPairingUnrecognizedDevice) { // if is searching for unrecognized device
            mAddCarPresenter.searchForUnrecognizedDevice();
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
            String enteredVin = ((EditText) findViewById(R.id.VIN)).getText().toString();
            mAddCarPresenter.setPendingCarVin(enteredVin);
            if (AddCarPresenter.isValidVin(enteredVin)) {
                Log.i(TAG, "Searching for car");

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);

                if (!AddCarPresenter.hasGotMileage) {
                    AddCarMileageDialog dialog = new AddCarMileageDialog();
                    dialog.setCallback(mAddCarPresenter).show(getSupportFragmentManager(), "Input Mileage");
                } else {
                    mAddCarPresenter.searchAndGetVin();
                }

            } else {
                hideLoading("Invalid VIN");
            }
        } else if (mPagerAdapter.getItem(1) instanceof AddCar2YesDongleFragment) { // If in the AddCar2YesDongleFragment
            Log.i(TAG, "Searching for car");

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
            if (!AddCarPresenter.hasGotMileage) {
                AddCarMileageDialog dialog = new AddCarMileageDialog();
                dialog.setCallback(mAddCarPresenter).show(getSupportFragmentManager(), "Input Mileage");
            } else {
                mAddCarPresenter.searchAndGetVin();
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
            if (isPairingUnrecognizedDevice) {
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
            mAddCarPresenter.unbindBluetoothService();
        }

        mAddCarPresenter.finish();

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
                    if (!AddCarPresenter.isValidVin(VIN)) {
                        Toast.makeText(AddCarActivity.this, "Invalid VIN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (requestCode == RC_PENDING_ADD_CAR) {
            Log.i(TAG, "Adding car from pending");
            showLoading("Adding car");
            mAddCarPresenter.startAddingNewCar();
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
        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if (string != null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(String string) {
        progressDialog.setMessage(string);
        if (!progressDialog.isShowing()) {
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

    @Override
    public BluetoothAutoConnectService getAutoConnectService() {
        return autoConnectService;
    }

    @Override
    public BSAbstractedFragmentActivity getActivity() {
        return this;
    }

    @Override
    public void onMileageEntered() {
        showLoading("Mileage entered, searching for car...");
    }

    @Override
    public void onDeviceConnected() {
        showLoading("Connected with device, getting VIN...");
    }

    @Override
    public void onRTCRetrieved(boolean needToBeSet) {
        if (needToBeSet) {
            showLoading("Syncing with device...");
        } else {
            showLoading("Reading VIN from device...");
        }
    }

    @Override
    public void onRTCReset() {
        showLoading("Device successfully synced...");
    }

    @Override
    public void onVINRetrieved(@Nullable String VIN, boolean isValid) {
        if (isValid) {
            showLoading("VIN retrieved, verifying...");
        } else {
            askForManualVinInput();
            hideLoading("Not supported VIN, please use manual input!");
        }
    }

    @Override
    public void onTimeoutRetry(final String timeoutEvent, final String mixpanelEvent) {
        hideLoading(null);

        if (isFinishing() || !addingCar) { // You don't want to add a dialog to a finished activity
            return;
        }

        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle(timeoutEvent)
                .setMessage(timeoutEvent + " failed. " +
                        "\n\nMake sure your vehicle engine is on and " +
                        "OBD device is properly plugged in.\n\nYou may also try turning off the Bluetooth on your phone and then turning it back on.\n\nTry again ?")
                .setCancelable(false)
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if (!isPairingUnrecognizedDevice) {
                            try {
                                mixpanelHelper.trackButtonTapped(mixpanelEvent, MixpanelHelper.ADD_CAR_VIEW);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            searchForCar(null);
                        } else {
                            mAddCarPresenter.searchForUnrecognizedDevice();
                        }
                    }
                })
                .setNegativeButton("NO", null)
                .show();
    }

    @Override
    public void onConfirmPostCar(String deviceId, String VIN) {

    }

    @Override
    public void onConfirmAddingDeletedCar(Car deletedCar, DialogInterface.OnClickListener positiveButton) {
        if (isFinishing()) return;

        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle("Adding deleted car")
                .setMessage("This " + deletedCar.getYear() + " " + deletedCar.getMake() + " " + deletedCar.getModel()
                        + " has been deleted by a previous user, do you wish to add it?")
                .setPositiveButton("YES", positiveButton)
                .setNegativeButton("CANCEL", null)
                .show();
    }

    @Override
    public void onPostCarStarted() {
        showLoading("VIN is valid, creating car profile...");
    }

    @Override
    public void onPostCarFailed(String errorMessage) {
        hideLoading(errorMessage);
        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setMessage(errorMessage)
                .setPositiveButton("OK", null)
                .show();
    }

    @Override
    public void onPostCarSucceeded(Car createdCar) {
        if (!addingCar) return;

        carSuccessfullyAdded = true; // At this point car is successfully added

        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, createdCar);
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
        setResult(ADD_CAR_SUCCESS, data);

        new CountDownTimer(2000, 2000) { // to let issues populate in server
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                hideLoading(null);
                finish();
            }
        }.start();
    }

    @Override
    public void askForManualVinInput() {
        if (!addingCar) return;

        hideLoading("This Car has been added previously!");

        if (mPager.getCurrentItem() == 1) {
            if (mPagerAdapter.getItem(1) instanceof AddCar2NoDongleFragment) {
                if (findViewById(R.id.VIN) != null) {
                    ((EditText) findViewById(R.id.VIN)).setText("");
                }
            } else if (mPagerAdapter.getItem(1) instanceof AddCar2YesDongleFragment) {
                mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle", 1);
                mPager.setCurrentItem(1);
            }
        }
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
        if (isFinishing()) return;

        if (autoConnectService != null && !selectCarDialogShowing) {
            final CarListAdapter carListAdapter = new CarListAdapter(MainActivity.carList);
            final Car[] pickedCar = new Car[1];

            final AnimatedDialogBuilder dialogBuilder = new AnimatedDialogBuilder(this)
                    .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW);
            final AlertDialog d = dialogBuilder.setCancelable(false)
                    .setTitle("Unrecognized module detected, please select the car this device is connected to:")
                    .setSingleChoiceItems(carListAdapter, -1, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            pickedCar[0] = (Car) carListAdapter.getItem(which);
                        }
                    })
                    .setPositiveButton("Confirm", null)
                    .setNegativeButton("Cancel", null)
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            selectCarDialogShowing = false;
                        }
                    }).create();

            d.setOnShowListener(new DialogInterface.OnShowListener() {
                @Override
                public void onShow(DialogInterface dialog) {
                    d.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (pickedCar[0] == null) {
                                Toast.makeText(AddCarActivity.this, "Please pick a car!", Toast.LENGTH_SHORT).show();
                            } else if (mAddCarPresenter.selectedValidCar(pickedCar[0])) {
                                mAddCarPresenter.validateAndPostScanner(pickedCar[0], scannerId, scannerName);
                                d.dismiss();
                            } else {
                                Toast.makeText(AddCarActivity.this, "This car has scanner!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            });

            d.show();
            selectCarDialogShowing = true;
        }
    }

    /**
     * Show a dialog prompting user to pair the corresponding car
     *
     * @param existedCar  The car retrieved from the backend whose user_id matches current user id
     * @param scannerName Current scanner name
     * @param scannerId   Current scanner id
     */
    @Override
    public void confirmPairCarWithDevice(final Car existedCar, final String scannerName, final String scannerId) {
        if (isFinishing()) {
            return;
        }

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
                        mAddCarPresenter.validateAndPostScanner(existedCar, scannerId, scannerName);
                    }
                }).show();
    }

    @Override
    public void onPairingDeviceWithCar() {
        showLoading("Linking device with the vehicle...");
    }

    @Override
    public void pairCarError(String errorMessage) {
        if (isFinishing()) {
            return;
        }

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
                        mAddCarPresenter.searchForUnrecognizedDevice();
                    }
                }).show();
    }

    @Override
    public void onDeviceSuccessfullyPaired() {
        Intent data = new Intent();
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
        setResult(PAIR_CAR_SUCCESS, data);
        hideLoading("Finish");
        finish();
    }

    @Override
    public boolean checkNetworkConnection(String error) {
        if (NetworkHelper.isConnected(this)) {
            return true;
        } else {
            if (error != null) {
                hideLoading(error);
            } else {
                hideLoading("No network connection! Please check your network connection and try again.");
            }
            return false;
        }
    }

    @Override
    public void startPendingAddCarActivity(Car pendingCar) {
        Intent intent = new Intent(this, PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, pendingCar.getBaseMileage());
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, pendingCar.getScannerId());
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, pendingCar.getVin());
        startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    @Override
    public void showRetryDialog(String title, String message, DialogInterface.OnClickListener retryButton, DialogInterface.OnClickListener cancelButton) {
        if (isFinishing()) return;

        new AnimatedDialogBuilder(this)
                .setAnimation(AnimatedDialogBuilder.ANIMATION_GROW)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Retry", retryButton)
                .setNegativeButton("Cancel", cancelButton)
                .show();
    }

}
