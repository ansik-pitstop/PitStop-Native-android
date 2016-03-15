package com.pitstop;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.background.BluetoothAutoConnectService;

/**
 * TODO move to DEBUG folder
 */
public class ReceiveDebugActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {

    TextView BTSTATUS;
    boolean pendingUpload, clicked;
    private BluetoothAutoConnectService service;
    /** Callbacks for service binding, passed to bindService() */
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service1) {
            // cast the IBinder and get MyService instance
            BluetoothAutoConnectService.BluetoothBinder binder = (BluetoothAutoConnectService.BluetoothBinder) service1;
            service = binder.getService();
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
        bindService(MainActivity.serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
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
        service.uploadRecords();
    }


    @Override
    public void getBluetoothState(int state) {
        if(!pendingUpload) {
            findViewById(R.id.loading).setVisibility(View.GONE);
        }
        if (state == BluetoothManage.CONNECTED) {
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

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        if(!pendingUpload) {
            findViewById(R.id.loading).setVisibility(View.GONE);
        }

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

        ((TextView) findViewById(R.id.debug_log)).setText(out);
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }


    public void getDTC(View view) {
        if (service.getState() != BluetoothManage.CONNECTED) {
            service.startBluetoothSearch();
        }else {
            service.getDTCs();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getPIDS(View view) {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        if (service.getState() != BluetoothManage.CONNECTED) {
            service.startBluetoothSearch();
        }else {
            service.getPIDs();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getFreeze(View view) {
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        if (service.getState() != BluetoothManage.CONNECTED) {
            service.startBluetoothSearch();
        }else {
            service.getFreeze();
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }
}
