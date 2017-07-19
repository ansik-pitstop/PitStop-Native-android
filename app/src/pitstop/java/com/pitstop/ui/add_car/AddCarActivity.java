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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pitstop.EventBus.EventSource;
import com.pitstop.R;
import com.pitstop.adapters.AddCarViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.SetUserCarUseCase;
import com.pitstop.models.Car;
import com.pitstop.ui.IBluetoothServiceActivity;
import com.pitstop.ui.add_car.view_fragment.AddCar1Fragment;
import com.pitstop.ui.add_car.view_fragment.AddCar2NoDongleFragment;
import com.pitstop.ui.add_car.view_fragment.AddCar2YesDongleFragment;
import com.pitstop.ui.add_car.view_fragment.AddCarChooseDealershipFragment;
import com.pitstop.ui.add_car.view_fragment.AddCarMileageDialog;
import com.pitstop.ui.add_car.view_fragment.AddCarViewPager;
import com.pitstop.ui.custom_shops.CustomShopActivity;
import com.pitstop.ui.main_activity.MainActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import butterknife.internal.DebouncingOnClickListener;

import static com.pitstop.ui.main_activity.MainActivity.CAR_EXTRA;

/**
 * Created by David on 7/20/2016.
 */
public class AddCarActivity extends IBluetoothServiceActivity implements AddCarContract.View {

    private final String TAG = AddCarActivity.class.getSimpleName();

    // extras
    public static final String EXTRA_PAIR_PENDING = "com.pitstop.ui.add_car.AddCarActivity.extra_pair_pending";

    // activity result
    public static final int ADD_CAR_SUCCESS = 51;
    public static final int PAIR_CAR_SUCCESS = 52;
    public static final int ADD_CAR_NO_DEALER_SUCCESS = 53;
    private static final int RC_PENDING_ADD_CAR = 1043;
    private static final int DEALER_CHOSEN = 4785;


    // views
    private AddCarViewPager mPager;
    private AddCarViewPagerAdapter mPagerAdapter;
    private ProgressDialog progressDialog;

    private UseCaseComponent component;

    private MixpanelHelper mixpanelHelper;
    private AddCarContract.Presenter presenter;

    public static boolean addingCar = false;
    public static boolean addingCarWithDevice = false;
    private boolean carSuccessfullyAdded;

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
    public void setPresenter(AddCarContract.Presenter presenter) {
        this.presenter = presenter;
        presenter.bind(this);
        presenter.bindBluetoothService();
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
        setPresenter(new AddCarPresenter(this, (GlobalApplication) getApplicationContext()));

        mixpanelHelper = new MixpanelHelper((GlobalApplication) getApplicationContext());
        component = DaggerUseCaseComponent.builder()
                .contextModule(new ContextModule((GlobalApplication)getApplicationContext()))
                .build();
        //setup view pager
        mPager = (AddCarViewPager) findViewById(R.id.add_car_view_pager);
        mPagerAdapter = new AddCarViewPagerAdapter(getSupportFragmentManager(), this);


        mPagerAdapter.addFragment(AddCar1Fragment.class, "STEP 1/3", 0);
        carSuccessfullyAdded = false;

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
                presenter.cancelAllTimeouts();
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
                            data.putExtra(MainActivity.CAR_EXTRA, presenter.getCreatedCar());
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
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_BACK, MixpanelHelper.ADD_CAR_VIEW);
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

            presenter.updateCreatedCarDealership(addCarChooseDealershipFragment.getShop());

            try { // Log in Mixpanel
                JSONObject properties = new JSONObject();
                properties.put("Button", "Selected " + ((AddCarChooseDealershipFragment) fragment).getShop().getName())
                        .put("View", MixpanelHelper.ADD_CAR_SELECT_DEALERSHIP_VIEW)
                        .put("Car", presenter.getPendingCar().getMake() + " " + presenter.getPendingCar().getModel());
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

        // Log in Mixpanel
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_YES_HARDWARE, MixpanelHelper.ADD_CAR_VIEW);
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
    Button addCarButton;
    public void noDongleClicked(View view) {

        addingCarWithDevice = false;

        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_NO_HARDWARE, MixpanelHelper.ADD_CAR_VIEW);
        mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle", 1);
        ((TextView) findViewById(R.id.step_text)).setText("STEP 2/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(1);
        addCarButton = (Button)mPagerAdapter.getItem(1).getView().findViewById(R.id.add_vehicle);
        addCarButton.setOnClickListener(new DebouncingOnClickListener() {
            @Override
            public void doClick(View v) {
                if (addCarButton != null){
                    addCarButton.setEnabled(false);
                }
                searchForCar(v);
            }
        });
    }

    @Override
    public void onMileageInputCancelled(){
        if (addCarButton != null){
            addCarButton.setEnabled(true);
        }
    }

    /**
     * Invoked when the "SEARCH FOR VEHICLE" or "Add Vehicle" button is tapped in the step 2 of the add car process
     *
     * @param view the "Search for vehicle"/"Add vehicle" button
     */
    public void searchForCar(View view) {

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
            presenter.setPendingCarVin(enteredVin);
            if (AddCarPresenter.isValidVin(enteredVin)) {
                Log.i(TAG, "Searching for car");

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);

                if (!presenter.hasGotMileage()) {
                    AddCarMileageDialog dialog = new AddCarMileageDialog();
                    dialog.setCallback(presenter).show(getSupportFragmentManager(), "Input Mileage");
                } else {
                    presenter.searchAndGetVin();
                }

            } else {
                hideLoading("Invalid VIN");
                if (addCarButton != null){
                    addCarButton.setEnabled(true);
                }
            }
        } else if (mPagerAdapter.getItem(1) instanceof AddCar2YesDongleFragment) { // If in the AddCar2YesDongleFragment
            Log.i(TAG, "Searching for car");

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
            if (!presenter.hasGotMileage()) {
                AddCarMileageDialog dialog = new AddCarMileageDialog();
                dialog.setCallback(presenter).show(getSupportFragmentManager(), "Input Mileage");
            } else {
                presenter.searchAndGetVin();
            }
        }
    }

    @Override
    public void onPostCarSucceeded(Car createdCar) {
        askForDealership(createdCar);
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
        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCAN_VIN_BARCODE, MixpanelHelper.ADD_CAR_VIEW);

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
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_BARCODE_SCANNER_VIEW_APPEARED);
    }

    @Override
    protected void onResume() {
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_VIEW);

        registerBluetoothReceiver();

        addingCar = true;

        // The result is by default cancelled
        //setResult(RESULT_CANCELED);

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

        if (serviceIsBound) {
            presenter.unbindBluetoothService();
        }

        presenter.finish();
        presenter.unbind();

        if (carSuccessfullyAdded) {
            mixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_ADD_CAR);
        }

        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
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
            presenter.startAddingNewCar();
        }else if(requestCode == DEALER_CHOSEN){

            finishActivity(presenter.getCreatedCar());
        }else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void hideLoading(String string) {
        if(progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        if(string != null) {
            Toast.makeText(this, string, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showLoading(final String string) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.setMessage(string);
                if(!progressDialog.isShowing()) {
                    progressDialog.show();
                }
            }
        });
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
    public IBluetoothServiceActivity getActivity() {
        return this;
    }

    @Override
    public void onMileageEntered() {
        if (addCarButton != null){
            addCarButton.setEnabled(true);
        }
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
                        mixpanelHelper.trackButtonTapped(mixpanelEvent, MixpanelHelper.ADD_CAR_VIEW);
                        searchForCar(null);
                    }
                })
                .setNegativeButton("NO", null)
                .show();
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


    private void finishActivity(Car createdCar) {
        carSuccessfullyAdded = true; // At this point car is successfully added

        Intent data = new Intent();
        data.putExtra(MainActivity.CAR_EXTRA, createdCar);
        data.putExtra(MainActivity.REFRESH_FROM_SERVER, true);
        setResult(ADD_CAR_SUCCESS, data);

        showLoading("Updating car");

        new CountDownTimer(2000, 2000) { // to let issues populate in server
            @Override
            public void onTick(long millisUntilFinished) {
                showLoading("Updating car");
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
    public void askForDealership(Car createdCar) {
        if (!addingCar) return;

        AddCarActivity thisActivity = this;

       component.setUseCarUseCase().execute(createdCar.getId(), EventSource.SOURCE_ADD_CAR, new SetUserCarUseCase.Callback() {
            @Override
            public void onUserCarSet() {

                Intent intent = new Intent(thisActivity, CustomShopActivity.class);
                intent.putExtra(CAR_EXTRA,createdCar);
                startActivityForResult(intent,DEALER_CHOSEN);

            }

            @Override
            public void onError() {// really need this to work
               askForDealership(createdCar);
            }
        });
        /*mPagerAdapter.addFragment(AddCarChooseDealershipFragment.class, "SelectDealership", 2);
        ((TextView) findViewById(R.id.step_text)).setText("STEP 3/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(2);

        // Go to the selectDealership fragment
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_SELECT_DEALERSHIP_VIEW);*/
    }

    @Override
    public void onPairingDeviceWithCar() {
        showLoading("Linking device with the vehicle...");
    }

    @Override
    public void onDeviceSuccessfullyPaired(Car car) {
        askForDealership(car);
        hideLoading("Finish");
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
