package com.pitstop;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import me.dm7.barcodescanner.zbar.Result;
import me.dm7.barcodescanner.zbar.ZBarScannerView;

public class BarcodeScannerActivity extends AppCompatActivity implements ZBarScannerView.ResultHandler{

    private ZBarScannerView mScannerView;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);    // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view

        ActionBar actionBar = getActionBar();       // hiding menu
        if (actionBar != null) {                    // just to avoid null pointer exception
            actionBar.hide();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(Result rawResult) {
        // Do something with the result here
        Log.d("barcode result", rawResult.getContents()); // Prints scan results
        Log.d("barcode format", rawResult.getBarcodeFormat().getName()); // Prints the scan format (qrcode, pdf417 etc.)

//        Intent intent = new Intent(this, AddCarActivity.class);
        Intent intent = new Intent();
        intent.putExtra("scannerVIN", rawResult.getContents());
        setResult(Activity.RESULT_OK, intent);
        finish();

    }
}