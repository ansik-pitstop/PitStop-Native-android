package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;

public class ReceiveDebugActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {

    TextView BTSTATUS;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_debug);
        BTSTATUS  = (TextView) findViewById(R.id.bluetooth_status);
        BTSTATUS.setText("Bluetooth Getting Started");
        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
        //BluetoothManage.getInstance(this).obdSetMonitor();

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
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void getBluetoothState(int state) {
        if (state == BluetoothManage.CONNECTED) {
            BTSTATUS.setText(R.string.bluetooth_connected);
        } else {
            BTSTATUS.setText(R.string.bluetooth_disconnected);
        }
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
        if(dataPackageInfo.result==1||dataPackageInfo.result==3||dataPackageInfo.result==4||dataPackageInfo.result==6) {
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
    }


    public void getDTC(View view) {
        if (BluetoothManage.getInstance(this).getState() != BluetoothManage.CONNECTED) {
            BluetoothManage.getInstance(this).connectBluetooth();
        }else {
            BluetoothManage.getInstance(this).obdSetMonitor(4, "2105,2142");
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    public void getFreeze(View view) {
        if (BluetoothManage.getInstance(this).getState() != BluetoothManage.CONNECTED) {
            BluetoothManage.getInstance(this).connectBluetooth();
        }else {
            BluetoothManage.getInstance(this).obdSetMonitor(1,"");
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }
}
