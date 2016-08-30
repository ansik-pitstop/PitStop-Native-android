package com.pitstop.ui;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.castel.obd.bluetooth.IBluetoothCommunicator;
import com.castel.obd.bluetooth.ObdManager;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothAutoConnectService;

public class ReceiveDebugActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    TextView BTSTATUS;
    boolean pendingUpload, clicked;
    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private static final String TAG = ReceiveDebugActivity.class.getSimpleName();

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = ((BluetoothAutoConnectService.BluetoothBinder) service1).getService();
            service.setCallbacks(ReceiveDebugActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_debug);
        BTSTATUS  = (TextView) findViewById(R.id.bluetooth_status);
        BTSTATUS.setText("Bluetooth Getting Started");
        setTitle("Connect to Car");
        pendingUpload = false;
        bindService(new Intent(this, BluetoothAutoConnectService.class),
                serviceConnection, BIND_AUTO_CREATE);
        clicked = false;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_receive_debug, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_upload) {
            uploadRecords();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
    }

    public void uploadRecords() {
        //TODO
        //service.uploadRecords();
    }


    @Override
    public void getBluetoothState(int state) {
        if(!pendingUpload) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.loading).setVisibility(View.GONE);
                }
            });
        }
        if (state == IBluetoothCommunicator.CONNECTED) {
            BTSTATUS.setText(R.string.bluetooth_connected);
        } else {
            BTSTATUS.setText(R.string.bluetooth_disconnected);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        Log.i(TAG, "setCtrlResponse: " + responsePackageInfo.toString());
    }

    @Override
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {
        Log.i(TAG, "setParameterResponse: " + responsePackageInfo.toString());
    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
        Log.i(TAG, "getParameterData: " + parameterPackageInfo.toString());
    }

    @Override
    public void getIOData(final DataPackageInfo dataPackageInfo) {
        Log.i(TAG, "getIOData");

        /*if(!pendingUpload) {
            findViewById(R.id.loading).setVisibility(View.GONE);
        }*/

        //display out
        String out = "";
        out += "result : " + dataPackageInfo.result + "\n";
        out += "deviceId : " + dataPackageInfo.deviceId + "\n";
        out += "tripId : " + dataPackageInfo.tripId + "\n";
        out += "dataNumber : " + dataPackageInfo.dataNumber + "\n";
        out += "tripFlag : " + dataPackageInfo.tripFlag + "\n";
        out += "rtcTime : " + dataPackageInfo.rtcTime + "\n";
        out += "protocolType : " + dataPackageInfo.protocolType + "\n";
        out += "tripMileage : " + dataPackageInfo.tripMileage + "\n";
        out += "tripfuel : " + dataPackageInfo.tripfuel + "\n";
        out += "vState : " + dataPackageInfo.vState + "\n";
        out += "OBD Data \n";
        for (PIDInfo i : dataPackageInfo.obdData) {
            out += "     " + i.pidType + " : " + i.value + "\n";
        }
        out += "Freeze Data \n";
        for (PIDInfo i : dataPackageInfo.freezeData) {
            out += "     " + i.pidType + " : " + i.value + "\n";
        }
        out += "surportPid : " + dataPackageInfo.surportPid + "\n";
        out += "dtcData : " + dataPackageInfo.dtcData + "\n";

        final String output = out;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading).setVisibility(View.GONE);
                ((TextView) findViewById(R.id.debug_log)).setText(output);
            }
        });

        //Log.e(TAG, dataPackageInfo.toString());
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    public void getPids(View view) {
        service.getPIDs();
    }

    public void getDTC(View view) {
        if (service.getState() != IBluetoothCommunicator.CONNECTED) {
            service.startBluetoothSearch();
        }else {
            service.getDTCs();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getPIDS(View view) {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        if (service.getState() != IBluetoothCommunicator.CONNECTED) {
            service.startBluetoothSearch();
        }else {
            service.getPIDs();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getParam(View view) {
        String tag = ((EditText) findViewById(R.id.tag)).getText().toString();
        service.getFreeze(tag);
    }

    public void getVin(View view) {
        service.getCarVIN();
    }

    public void getRtc(View view) {
        service.getObdDeviceTime();
    }

    public void setRtc(View view) {
        service.syncObdDevice();
    }

    public void resetRtc(View view) {
        service.resetObdDeviceTime();
    }

    public void setFixedUpload(View view) {
        String tag = ObdManager.FIXED_UPLOAD_TAG;
        String values = ((EditText) findViewById(R.id.values)).getText().toString();
        service.setParam(tag, values);
    }

    public void setParam(View view) {
        service.setFixedUpload();
    }

    public void sendLoginInstruction(View view) {
        service.writeLoginInstruction();
    }

    public void initialize(View view) {
        service.initialize();
    }
}
