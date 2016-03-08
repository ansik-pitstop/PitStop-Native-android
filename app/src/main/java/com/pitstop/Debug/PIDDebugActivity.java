package com.pitstop.Debug;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.R;

public class PIDDebugActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_piddebug);
        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_piddebug, menu);
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
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }
}
