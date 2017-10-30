package com.pitstop.ui;

import android.content.ComponentName;
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
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;
import com.pitstop.bluetooth.BluetoothAutoConnectService;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.observer.BluetoothConnectionObservable;

import java.util.Map;

public class ReceiveDebugActivity extends AppCompatActivity implements ObdManager.IBluetoothDataListener {

    private static final String TAG = ReceiveDebugActivity.class.getSimpleName();

    TextView BTSTATUS;
    boolean pendingUpload, clicked;
    /** Callbacks for service binding, passed to bindService() */
    private BluetoothAutoConnectService service;

    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = ((BluetoothAutoConnectService.BluetoothBinder) service1).getService();
            //service.addCallback(ReceiveDebugActivity.this); // register
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
//        bindService(new Intent(this, BluetoothAutoConnectService.class),
//                serviceConnection, BIND_AUTO_CREATE);
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

//    @Override
//    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {
//        Log.i(TAG, "getParameterData: " + parameterPackageInfo.toString());
//    }

    @Override
    public void tripData(TripInfoPackage tripInfoPackage) {

    }

    @Override
    public void parameterData(final ParameterPackage parameterPackage) {
        Log.i(TAG, "parameterData: " + parameterPackage.toString());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading).setVisibility(View.GONE);
                ((TextView) findViewById(R.id.debug_log)).setText("Parameter data: " + parameterPackage.toString());
            }
        });
    }

    @Override
    public void idrPidData(PidPackage pidPackage) {
        if(pidPackage.pids == null) {
            return;
        }

        final StringBuilder pidList = new StringBuilder();

        pidList.append("PIDS:\n");

        for(Map.Entry<String, String> pid : pidPackage.pids.entrySet()) {
            pidList.append(pid.getKey());
            pidList.append(": ");
            pidList.append(pid.getValue());
            pidList.append("\n");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading).setVisibility(View.GONE);
                ((TextView) findViewById(R.id.debug_log)).setText(pidList.toString());
            }
        });
    }

    @Override
    public void pidData(PidPackage pidPackage) {

    }

    @Override
    public void dtcData(final DtcPackage dtcPackage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading).setVisibility(View.GONE);
                ((TextView)findViewById(R.id.debug_log)).setText(dtcPackage.toString());
            }
        });
    }

    @Override
    public void ffData(final FreezeFramePackage ffPackage) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.loading).setVisibility(View.GONE);
                ((TextView)findViewById(R.id.debug_log)).setText(ffPackage.toString());
            }
        });
    }

    @Override
    public void scanFinished() {

    }

//    @Override
//    public void getIOData(final DataPackageInfo dataPackageInfo) {
//        Log.i(TAG, "getIOData");
//
//        /*if(!pendingUpload) {
//            findViewById(R.id.loading).setVisibility(View.GONE);
//        }*/
//
//        //display out
//        String out = "";
//        out += "result : " + dataPackageInfo.result + "\n";
//        out += "deviceId : " + dataPackageInfo.deviceId + "\n";
//        out += "tripId : " + dataPackageInfo.tripId + "\n";
//        out += "dataNumber : " + dataPackageInfo.dataNumber + "\n";
//        out += "tripFlag : " + dataPackageInfo.tripFlag + "\n";
//        out += "rtcTime : " + dataPackageInfo.rtcTime + "\n";
//        out += "protocolType : " + dataPackageInfo.protocolType + "\n";
//        out += "tripMileage : " + dataPackageInfo.tripMileage + "\n";
//        out += "tripfuel : " + dataPackageInfo.tripfuel + "\n";
//        out += "vState : " + dataPackageInfo.vState + "\n";
//        out += "OBD Data \n";
//        for (PIDInfo i : dataPackageInfo.obdData) {
//            out += "     " + i.pidType + " : " + i.value + "\n";
//        }
//        out += "Freeze Data \n";
//        for (PIDInfo i : dataPackageInfo.freezeData) {
//            out += "     " + i.pidType + " : " + i.value + "\n";
//        }
//        out += "surportPid : " + dataPackageInfo.surportPid + "\n";
//        out += "dtcData : " + dataPackageInfo.dtcData + "\n";
//
//        final String output = out;
//
//        runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                findViewById(R.id.loading).setVisibility(View.GONE);
//                ((TextView) findViewById(R.id.debug_log)).setText(output);
//            }
//        });
//
//        //Log.e(TAG, dataPackageInfo.toString());
//    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    public void getSupportedPids(View view) {
    }

    public void getPids(View view) {
    }

    public void getDTC(View view) {
        if (service.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)) {
            service.requestDeviceSearch(false,false);
        }else {
            service.requestDtcData();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getPIDS(View view) {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        if (service.getDeviceState().equals(BluetoothConnectionObservable.State.CONNECTED_VERIFIED)) {
            service.requestDeviceSearch(false,false);
        }else {
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getParam(View view) {
        String tag = ((EditText) findViewById(R.id.tag)).getText().toString();
        //service.getFreeze(tag);
    }

    public void getVin(View view) {
        service.requestVin();
    }

    public void getRtc(View view) {
    }

    public void setRtc(View view) {
        service.requestDeviceSync();
    }

    public void resetRtc(View view) {
    }

    public void setFixedUpload(View view) {
        String tag = ObdManager.FIXED_UPLOAD_TAG;
        String values = ((EditText) findViewById(R.id.values)).getText().toString();
    }

    public void setParam(View view) {
       // service.setFixedUpload();
    }

    public void initialize(View view) {
        //service.initialize();
    }

    public void writeToObd(View view) {
        String tag = ((EditText) findViewById(R.id.tag)).getText().toString();
        String value = ((EditText) findViewById(R.id.values)).getText().toString();
    }

    @Override
    public void alarmEvent(String alarmEvents, String alarmValues, String rtcTime) {
        // do nothing
    }
}
