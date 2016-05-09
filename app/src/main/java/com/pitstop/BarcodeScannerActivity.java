package com.pitstop;

import android.app.Dialog;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.MultiProcessor;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;
import com.pitstop.BarcodeScanner.BarcodeGraphic;
import com.pitstop.BarcodeScanner.BarcodeGraphicTracker;
import com.pitstop.BarcodeScanner.BarcodeScanner;
import com.pitstop.BarcodeScanner.BarcodeScannerBuilder;
import com.pitstop.BarcodeScanner.BarcodeTrackerFactory;
import com.pitstop.BarcodeScanner.CameraSource;
import com.pitstop.BarcodeScanner.CameraSourcePreview;
import com.pitstop.BarcodeScanner.GraphicOverlay;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;

import static junit.framework.Assert.assertNotNull;

public class BarcodeScannerActivity extends AppCompatActivity {

    private static final int RC_HANDLE_GMS = 9001;

    private static final String TAG = "BarcodeScanner";

    private BarcodeScanner mBarcodeScanner;
    private BarcodeScannerBuilder mBarcodeScannerBuilder;

    private BarcodeDetector barcodeDetector;

    private CameraSourcePreview mCameraSourcePreview;

    private GraphicOverlay<BarcodeGraphic> mGraphicOverlay;

    /**
     * true if no further barcode should be detected or given as a result
     */
    private boolean mDetectionConsumed = false;

    private boolean mFlashOn = false;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        if(getWindow() != null){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }else{
            Log.e(TAG, "Barcode scanner could not go into fullscreen mode!");
        }
        setContentView(R.layout.activity_barcode_scanner);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onMaterialBarcodeScanner(BarcodeScanner barcodeScanner){
        this.mBarcodeScanner = barcodeScanner;
        mBarcodeScannerBuilder = mBarcodeScanner.getMaterialBarcodeScannerBuilder();
        barcodeDetector = mBarcodeScanner.getMaterialBarcodeScannerBuilder().getBarcodeDetector();
        startCameraSource();
        setupLayout();
    }

    private void setupLayout() {
        final TextView topTextView = (TextView) findViewById(R.id.topText);
        assertNotNull(topTextView);
        String topText = mBarcodeScannerBuilder.getText();
        if(!mBarcodeScannerBuilder.getText().equals("")){
            topTextView.setText(topText);
        }
        setupButtons();
        setupCenterTracker();
    }

    private void setupCenterTracker() {
        if(mBarcodeScannerBuilder.getScannerMode() == BarcodeScanner.SCANNER_MODE_CENTER){
            final ImageView centerTracker  = (ImageView) findViewById(R.id.barcode_square);
            centerTracker.setImageResource(mBarcodeScannerBuilder.getTrackerResourceID());
            mGraphicOverlay.setVisibility(View.INVISIBLE);
        }
    }

    private void updateCenterTrackerForDetectedState() {
        if(mBarcodeScannerBuilder.getScannerMode() == BarcodeScanner.SCANNER_MODE_CENTER){
            final ImageView centerTracker  = (ImageView) findViewById(R.id.barcode_square);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    centerTracker.setImageResource(mBarcodeScannerBuilder.getTrackerDetectedResourceID());
                }
            });
        }
    }

    private void setupButtons() {
        final LinearLayout flashOnButton = (LinearLayout)findViewById(R.id.flashIconButton);
        final ImageView flashToggleIcon = (ImageView)findViewById(R.id.flashIcon);
        assertNotNull(flashOnButton);
        flashOnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mFlashOn) {
                    flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_on_white_24dp);
                    disableTorch();
                } else {
                    flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_off_white_24dp);
                    enableTorch();
                }
                mFlashOn ^= true;
            }
        });
        if(mBarcodeScannerBuilder.isFlashEnabledByDefault()){
            flashToggleIcon.setBackgroundResource(R.drawable.ic_flash_off_white_24dp);
        }
    }

    /**
     * Starts or restarts the camera source, if it exists.  If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() throws SecurityException {
        // check that the device has play services available.
        int code = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(
                getApplicationContext());
        if (code != ConnectionResult.SUCCESS) {
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(this, code, RC_HANDLE_GMS);
            dialog.show();
        }
        mGraphicOverlay = (GraphicOverlay<BarcodeGraphic>)findViewById(R.id.graphicOverlay);
        BarcodeGraphicTracker.NewDetectionListener listener =  new BarcodeGraphicTracker.NewDetectionListener() {
            @Override
            public void onNewDetection(Barcode barcode) {
                if(!mDetectionConsumed){
                    mDetectionConsumed = true;
                    Log.d(TAG, "Barcode detected! - " + barcode.displayValue);
                    EventBus.getDefault().postSticky(barcode);
                    updateCenterTrackerForDetectedState();
                    mGraphicOverlay.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            finish();
                        }
                    },50);
                }
            }
        };
        BarcodeTrackerFactory barcodeFactory = new BarcodeTrackerFactory(mGraphicOverlay, listener, mBarcodeScannerBuilder.getTrackerColor());
        barcodeDetector.setProcessor(new MultiProcessor.Builder<>(barcodeFactory).build());
        CameraSource mCameraSource = mBarcodeScannerBuilder.getCameraSource();
        if (mCameraSource != null) {
            try {
                mCameraSourcePreview = (CameraSourcePreview) findViewById(R.id.preview);
                mCameraSourcePreview.start(mCameraSource, mGraphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                mCameraSource.release();
                mCameraSource = null;
            }
        }
    }

    private void enableTorch() throws SecurityException{
        mBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        try {
            mBarcodeScannerBuilder.getCameraSource().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void disableTorch() throws SecurityException{
        mBarcodeScannerBuilder.getCameraSource().setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        try {
            mBarcodeScannerBuilder.getCameraSource().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mCameraSourcePreview != null) {
            mCameraSourcePreview.stop();
        }
    }

    /**
     * Releases the resources associated with the camera source, the associated detectors, and the
     * rest of the processing pipeline.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isFinishing()){
            clean();
        }
    }

    private void clean() {
        EventBus.getDefault().removeStickyEvent(BarcodeScanner.class);
        if (mCameraSourcePreview != null) {
            mCameraSourcePreview.release();
            mCameraSourcePreview = null;
        }
    }
}
