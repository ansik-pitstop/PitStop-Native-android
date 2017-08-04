package com.pitstop.ui.add_car.vin_entry;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.dependency.ContextModule;
import com.pitstop.dependency.DaggerUseCaseComponent;
import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.models.Car;
import com.pitstop.ui.FragmentIntentIntegrator;
import com.pitstop.ui.add_car.FragmentSwitcher;
import com.pitstop.ui.add_car.PendingAddCarActivity;
import com.pitstop.utils.AnimatedDialogBuilder;
import com.pitstop.utils.MixpanelHelper;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

/**
 * Created by Karol Zdebel on 8/1/2017.
 */

public class VinEntryFragment extends Fragment implements VinEntryView{

    private final String TAG = getClass().getSimpleName();
    public static final int RC_PENDING_ADD_CAR = 1043;

    @BindView(R.id.add_vehicle)
    Button addVehicleButton;
    @BindView(R.id.VIN)
    EditText vinEditText;
    @BindView(R.id.input_mileage)
    EditText mileageEditText;

    private ViewGroup rootView;
    private VinEntryPresenter presenter;
    private MixpanelHelper mixpanelHelper;
    private FragmentSwitcher fragmentSwitcher;
    private ProgressDialog progressDialog;
    private UseCaseComponent useCaseComponent;

    public static VinEntryFragment getInstance(){
        return new VinEntryFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.d(TAG,"onCreateView()");

        if (progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setCancelable(false);

            /*Has to be handled because when the ProgressDialog
            * is open onBackPressed() is not invoked */
            progressDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    if (presenter != null){
                        presenter.onProgressDialogKeyPressed(keyCode);
                    }
                    return false;
                }
            });
        }

        if (useCaseComponent == null){
            useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(new ContextModule(getContext()))
                    .build();
        }

        if (mixpanelHelper == null){
            mixpanelHelper = new MixpanelHelper(
                    (GlobalApplication)getActivity().getApplicationContext());
        }

        if (fragmentSwitcher == null){
            fragmentSwitcher = (FragmentSwitcher)getActivity();
        }

        rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_vin_entry, container, false);
        ButterKnife.bind(this, rootView);
        if (presenter == null){
            presenter = new VinEntryPresenter(useCaseComponent,mixpanelHelper);
        }
        presenter.subscribe(this);
        return rootView;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG,"onDestroyView()");

        presenter.unsubscribe();
        super.onDestroyView();
    }

    private boolean checkBackCamera() {
        Log.d(TAG,"checkBackCamera()");

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

    @OnClick(R.id.scan_vin)
    protected void scanVinClicked(){
        Log.d(TAG,"scanVinClicked()");

        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_SCAN_VIN_BARCODE, MixpanelHelper.ADD_CAR_VIEW);

        if (!checkBackCamera()) {
            Toast.makeText(getContext(), "This device does not have a back facing camera",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        IntentIntegrator barcodeScanner = new FragmentIntentIntegrator(this);
        barcodeScanner.setBeepEnabled(false);
        barcodeScanner.initiateScan();

        //When the Barcode Scanner view appears
        mixpanelHelper.trackViewAppeared(MixpanelHelper.ADD_CAR_BARCODE_SCANNER_VIEW_APPEARED);
    }

    @OnClick(R.id.add_vehicle)
    protected void addVehicleClicked(){
        Log.d(TAG,"addVehicleClicked()");

        mixpanelHelper.trackButtonTapped(MixpanelHelper.ADD_CAR_METHOD_MANUAL
                ,MixpanelHelper.ADD_CAR_VIEW);
        presenter.addVehicle();
    }

    @Override
    public String getVin(){
        if (vinEditText == null || vinEditText.getText() == null) return "";
        else return vinEditText.getText().toString();
    }

    @OnTextChanged(R.id.VIN)
    protected void onVinTextChanged(Editable editable){
        Log.d(TAG,"onVinTextChanged() vin:"+editable.toString());
        presenter.vinChanged(vinEditText.getText().toString());
    }

    @Override
    public void onValidVinInput() {
        Log.d(TAG,"onValidVininput()");

        if (addVehicleButton.isEnabled()) return;

        addVehicleButton.setEnabled(true);
        addVehicleButton.setBackground(getResources()
                .getDrawable(R.drawable.color_button_rectangle_highlight));
    }

    @Override
    public void onInvalidVinInput() {
        Log.d(TAG,"onInvalidVinInput()");

        if (!addVehicleButton.isEnabled()) return;

        addVehicleButton.setBackground(getResources().getDrawable(R.drawable.color_button_rectangle_grey));
        addVehicleButton.setEnabled(false);
    }

    @Override
    public int getMileage() {
        Log.d(TAG,"getMileage(), returning: "
                +Integer.valueOf(mileageEditText.getText().toString()));

        return Integer.valueOf(mileageEditText.getText().toString());
    }

    @Override
    public void onInvalidMileage() {
        Log.d(TAG,"onInvalidMileage()");

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Invalid Mileage")
                .setMessage("Please input a mileage between 0 and 3,000,000")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("", null).create();
        invalidMileageDialog.show();
    }

    @Override
    public void onGotDeviceInfo(String scannerId, String scannerName) {
        Log.d(TAG,"onGotDeviceInfo() scannerId: "+scannerId+", scannerName: "+scannerName);

        presenter.gotDeviceInfo(scannerId,scannerName);
    }

    @Override
    public void onCarAddedWithShop(Car car) {
        Log.d(TAG,"onCarAddedWithShop() car:"+car);

        if (fragmentSwitcher == null) return;
        fragmentSwitcher.endAddCarSuccess(car,true);
    }

    @Override
    public void onCarAddedWithoutShop(Car car) {
        Log.d(TAG,"onCarAddedWithoutShop() car:"+car);

        if (fragmentSwitcher == null) return;
        fragmentSwitcher.endAddCarSuccess(car,false);
    }

    @Override
    public void onErrorAddingCar(String message) {
        Log.d(TAG,"onErrorAddingCar()");

        AlertDialog invalidMileageDialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Add Car Error")
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setNegativeButton("", null).create();
        invalidMileageDialog.show();
    }

    @Override
    public void onCarAlreadyAdded(Car car) {
        Log.d(TAG, "onCarAlreadyAdded() car: "+car);

        AlertDialog dialog= new AnimatedDialogBuilder(getActivity())
                .setTitle("Car Already Added")
                .setMessage("This car has already been added and is in use." +
                        " If this is your vehicle please remove it and try again.")
                .setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setNegativeButton("",null)
                .create();

        dialog.show();
    }

    @Override
    public void displayVin(String vin) {
        if (vin == null) vin = "";
        vinEditText.setText(vin);
    }

    @Override
    public void displayScannedVinValid() {
        Log.d(TAG,"displayScannedVinValid()");
        Toast.makeText(getContext(),"Scanned VIN Successully",Toast.LENGTH_LONG).show();
    }

    @Override
    public void displayScannedVinInvalid() {
        Log.d(TAG,"displayScannedVinInvalid()");
        Toast.makeText(getContext(),"Scanned VIN is invalid",Toast.LENGTH_LONG).show();
    }

    @Override
    public void showAskHasDeviceView() {
        Log.d(TAG,"showAskHasDeviceView()");
        if (fragmentSwitcher == null) return;

        fragmentSwitcher.setViewAskHasDevice();
    }

    @Override
    public void beginPendingAddCarActivity(String vin, double mileage, String scannerId) {
        if (getActivity() == null) return;

        Intent intent = new Intent(getActivity(), PendingAddCarActivity.class);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_MILEAGE, mileage);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_SCANNER, scannerId);
        intent.putExtra(PendingAddCarActivity.ADD_CAR_VIN, vin);
        startActivityForResult(intent, RC_PENDING_ADD_CAR);
    }

    @Override
    public void showLoading(@NonNull String message) {
        Log.d(TAG,"showLoading() message: "+message);
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.setMessage(message);
        progressDialog.show();
    }

    @Override
    public void hideLoading(@Nullable String message) {
        Log.d(TAG,"hideLoading() message: "+message);
        if (progressDialog == null || getActivity() == null) return;

        progressDialog.dismiss();

        if (message != null && !message.isEmpty()){
            Toast.makeText(getActivity(),message,Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void setLoadingCancelable(boolean cancelable) {
        if (progressDialog == null) return;

        progressDialog.setCancelable(cancelable);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG,"onActivityResult()");
        if (presenter == null) return;

        if (requestCode == IntentIntegrator.REQUEST_CODE) {
            IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
            Log.d(TAG,"onActivityResult() requestCode == ScanRequestCode" +
                    ", result: "+(result == null ? "" : result.getContents()));

            if (result == null || result.getContents() == null) presenter.onGotVinScanResult("");
            else{
                presenter.onGotVinScanResult(result.getContents());
            }
        }
        else if (requestCode == RC_PENDING_ADD_CAR){
            String vin = data.getStringExtra(PendingAddCarActivity.ADD_CAR_VIN);
            int mileage = Integer.valueOf(data.getStringExtra(PendingAddCarActivity
                    .ADD_CAR_MILEAGE));
            String scannerId = data.getStringExtra(PendingAddCarActivity.ADD_CAR_SCANNER);

            presenter.onGotPendingActivityResults(vin,mileage,scannerId,scannerId);
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onBackPressed(){
        if (presenter == null) return;

        presenter.onBackPressed();
    }
}
