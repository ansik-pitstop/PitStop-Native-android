package com.pitstop;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.parse.PIDParse;

import java.util.List;

/**
 * Proof of concept only! NOT IN USE!
 */
public class EnviornmentalCheckActivity extends AppCompatActivity  implements BluetoothManage.BluetoothDataListener{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enviornmental_check);
        setTitle("Environmental Check");

        BluetoothManage.getInstance(this).setBluetoothDataListener(this);

        BluetoothManage.getInstance(EnviornmentalCheckActivity.this)
                .obdSetMonitor(4, "2141");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_enviornmental_check, menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        }else if (id== android.R.id.home){
            finish();
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
    public void setParameterResponse(ResponsePackageInfo responsePackageInfo) {

    }

    @Override
    public void getParameterData(ParameterPackageInfo parameterPackageInfo) {

    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {

        if (6 != dataPackageInfo.result) {
            return;
        }
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer = getPIDData(stringBuffer, dataPackageInfo.freezeData);
        ((TextView) findViewById(R.id.textView9)).setText(stringBuffer.toString());
        Toast.makeText(this,"Updated",Toast.LENGTH_SHORT).show();
    }

    @Override
    public void deviceLogin(LoginPackageInfo loginPackageInfo) {

    }

    private StringBuffer getPIDData(StringBuffer stringBuffer,
                                    List<PIDInfo> lists) {
        stringBuffer.append(getString(R.string.freeze_data) + "\n");
        for (int i = 0; i < lists.size(); i++) {
            PIDInfo pidValue = PIDParse.parse(this, lists.get(i));

            if (null == pidValue.meaning || null == pidValue.unit) {
                stringBuffer.append(getString(R.string.type) + ": 0x"
                        + pidValue.pidType + "\n");
                stringBuffer.append(getString(R.string.value) + ": "
                        + pidValue.value + "\n");
            } else {
                stringBuffer.append(getString(R.string.type) + ": 0x"
                        + pidValue.pidType + "(" + pidValue.meaning + ")\n");
                stringBuffer.append(getString(R.string.value) + ": "
                        + pidValue.value + "(" + pidValue.intValues[0]
                        + pidValue.unit + ")\n");
            }
        }
        return stringBuffer;
    }
}
