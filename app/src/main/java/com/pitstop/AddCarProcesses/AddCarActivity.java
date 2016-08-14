package com.pitstop.AddCarProcesses;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pitstop.PendingAddCarActivity;
import com.pitstop.model.Car;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.adapters.AddCarViewPagerAdapter;
import com.pitstop.application.GlobalApplication;
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.utils.AddCarViewPager;
import com.pitstop.utils.BSAbstractedFragmentActivity;
import com.pitstop.utils.MixpanelHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by David on 7/20/2016.
 */
public class AddCarActivity extends BSAbstractedFragmentActivity implements AddCarUtils.AddCarUtilsCallback{

    private final String TAG = AddCarActivity.class.getSimpleName();
    public static int ADD_CAR_SUCCESS = 51;

    AddCarViewPager mPager;
    private AddCarViewPagerAdapter mPagerAdapter;
    private ProgressDialog progressDialog;
    private MixpanelHelper mixpanelHelper;
    private AddCarUtils addCarUtils;

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

    public MixpanelHelper getMixpanelHelper() {
        return mixpanelHelper;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_car_fragmented);

        //setup view pager
        mPager = (AddCarViewPager) findViewById(R.id.add_car_view_pager);
        mPagerAdapter = new AddCarViewPagerAdapter(getSupportFragmentManager(),this);
        mPagerAdapter.addFragment(AddCar1Fragment.class, "STEP 1/3",0);
        mPager.setAdapter(mPagerAdapter);
        setupUIReferences();

        mixpanelHelper = new MixpanelHelper((GlobalApplication)getApplicationContext());
        addCarUtils = new AddCarUtils((GlobalApplication)getApplicationContext(),this);

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
            ((TextView)findViewById(R.id.step_text)).setText("STEP "+Integer.toString(mPager.getCurrentItem()) + "/3");
            mPager.setCurrentItem(mPager.getCurrentItem() - 1);
        }
    }

    public void noDongleClicked(View view) {
        mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle",1);
        ((TextView)findViewById(R.id.step_text)).setText("STEP 2/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(1);
    }

    public void selectDealershipClicked(View view) {
        Fragment fragment = mPagerAdapter.getItem(2);
        if(fragment!=null&&fragment instanceof AddCarChooseDealershipFragment){
            AddCarChooseDealershipFragment addCarChooseDealershipFragment = (AddCarChooseDealershipFragment)fragment;
            if(addCarChooseDealershipFragment.getShop() == null) {
                Toast.makeText(this, "No dealership was selected", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                mixpanelHelper.trackButtonTapped("Selected " + ((AddCarChooseDealershipFragment) fragment).getShop().getName(), TAG);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            addCarUtils.setDealership(addCarChooseDealershipFragment.getShop());
            addCarUtils.addCarToServer(null);
        }
    }
    public void yesDongleClicked(View view) {
        mPagerAdapter.addFragment(AddCar2YesDongleFragment.class, "YesDongle",1);
        ((TextView)findViewById(R.id.step_text)).setText("STEP 2/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(1);
    }

    public void searchForCar(View view) {
        if (mPagerAdapter.getItem(1)!=null&&mPagerAdapter.getItem(1) instanceof AddCar2NoDongleFragment) {
            EditText vinEditText = (EditText) findViewById(R.id.VIN);
            addCarUtils.setVin(vinEditText.getText().toString());
            if (addCarUtils.isValidVin()) {
                Log.i(TAG, "Searching for car");

                // Hide keyboard
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
                AddCarMilageDialog dialog = new AddCarMilageDialog();
                dialog.setCallback(addCarUtils).show(getSupportFragmentManager(), "Input Milage");
            } else {
                hideLoading("Invalid VIN");
            }
        }else if(mPagerAdapter.getItem(1)!=null){
            Log.i(TAG, "Searching for car");

            // Hide keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view != null ? view.getWindowToken() : null, 0);
            AddCarMilageDialog dialog = new AddCarMilageDialog();
            dialog.setCallback(addCarUtils).show(getSupportFragmentManager(), "Input Milage");
        }
    }


    private boolean checkBackCamera() {
        final int CAMERA_FACING_BACK = 0;
        int cameraCount = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for(int i = 0; i < cameraCount; i++) {
            Camera.getCameraInfo(i,info);
            if(CAMERA_FACING_BACK == info.facing) {
                return true;
            }
        }
        return false;
    }

    public void startScanner(View view) {
        try {
            mixpanelHelper.trackButtonTapped("Scan VIN Barcode", TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if(!checkBackCamera()) {
            Toast.makeText(this,"This device does not have a back facing camera",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        Log.i(TAG,"Starting barcode scanner");

        IntentIntegrator barcodeScanner = new IntentIntegrator(this);
        barcodeScanner.setBeepEnabled(false);
        barcodeScanner.initiateScan();
    }


    @Override
    protected void onResume() {
        try {
            mixpanelHelper.trackViewAppeared(TAG);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        super.onResume();
    }

    @Override
    protected void onPause() {
        ((GlobalApplication)getApplicationContext()).getMixpanelAPI().flush();

        try {
            unregisterReceiver(bluetoothReceiver);
        } catch (Exception e) {

        }

        //hideLoading();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        hideLoading(null);

        if(serviceIsBound) {
            addCarUtils.unbindService();
        }
        addCarUtils.cancelMashape();
        super.onDestroy();
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.activity_slide_right_in, R.anim.activity_slide_right_out);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            if (result != null) {
                if (result.getContents() != null) {
                    String VIN = result.getContents();
                    if (VIN.length() == 18) {
                        VIN = VIN.substring(1, 18);
                    }
                    try {
                        mixpanelHelper.trackCustom("Scanned VIN",
                                new JSONObject("{'VIN':'" + VIN + "','Device':'Android'}"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    View vinField = findViewById(R.id.VIN);
                    if (vinField != null) {
                        ((EditText) vinField).setText(VIN);
                    }
                    Log.i(TAG, "Barcode read: " + VIN);
                    if (AddCarUtils.isValidVin(VIN)) {
                    } else {
                        Toast.makeText(AddCarActivity.this, "Invalid VIN", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if(requestCode == AddCarUtils.RC_PENDING_ADD_CAR) {
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

    @Override
    public void carSuccessfullyAdded(Car car) {

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
        if(mPager.getCurrentItem()==1){
            if(findViewById(R.id.VIN) != null) {
                ((EditText) findViewById(R.id.VIN)).setText("");
            }
        } else if(mPager.getCurrentItem() == 2) {
            mPagerAdapter.addFragment(AddCar2NoDongleFragment.class, "NoDongle", 1);
            mPager.setCurrentItem(1);
        }
    }

    @Override
    public void openRetryDialog() {
        hideLoading(null);

        if(isFinishing()) { // You don't want to add a dialog to a finished activity
            return;
        }

        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        alertDialog.setTitle("Device not connected");

        // Alert message
        alertDialog.setMessage("Could not connect to device. " +
                "\n\nMake sure your vehicle engine is on and " +
                "OBD device is properly plugged in.\n\nTry again ?");
        alertDialog.setCancelable(false);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                searchForCar(null);
            }
        });

        alertDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alertDialog.show();
    }

    @Override
    public BluetoothAutoConnectService getAutoConnectService() {
        return autoConnectService;
    }

    @Override
    public void postMileageInput() {
        mPagerAdapter.addFragment(AddCarChooseDealershipFragment.class, "SelectDealership",2);
        ((TextView)findViewById(R.id.step_text)).setText("STEP 3/3");
        mPagerAdapter.notifyDataSetChanged();
        mPager.setCurrentItem(2);
    }
}
