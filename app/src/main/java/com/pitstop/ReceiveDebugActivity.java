package com.pitstop;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Responses;
import com.pitstop.database.models.Uploads;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ReceiveDebugActivity extends AppCompatActivity implements BluetoothManage.BluetoothDataListener {

    TextView BTSTATUS;
    boolean pendingUpload, clicked;
    private int count;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_debug);
        BTSTATUS  = (TextView) findViewById(R.id.bluetooth_status);
        BTSTATUS.setText("Bluetooth Getting Started");
        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
        setTitle("Connect to Car");
        count=0;
        pendingUpload = false;
        clicked = false;
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
        if (id == R.id.action_upload) {
            uploadRecords();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void uploadRecords() {
        if (!pendingUpload){
            findViewById(R.id.loading).setVisibility(View.VISIBLE);
            UploadInfoOnline uploadInfoOnline = new UploadInfoOnline();
            uploadInfoOnline.execute();
        }else{
            Toast.makeText(this,"Uploads are already pending!",Toast.LENGTH_SHORT).show();
        }
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
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Responses response = new Responses();
        if(dataPackageInfo.result==5){
            count++;
        }
        if(dataPackageInfo.result==1||dataPackageInfo.result==3||dataPackageInfo.result==4||dataPackageInfo.result==6||count%20==1) {
            count=1;
            response.setValue("result",""+dataPackageInfo.result);
            response.setValue("deviceId",dataPackageInfo.deviceId);
            response.setValue("tripId",dataPackageInfo.tripId);
            response.setValue("dataNumber",dataPackageInfo.dataNumber);
            response.setValue("tripFlag",dataPackageInfo.tripFlag);
            response.setValue("rtcTime",dataPackageInfo.rtcTime);
            response.setValue("protocolType",dataPackageInfo.protocolType);
            response.setValue("tripMileage",dataPackageInfo.tripMileage);
            response.setValue("tripfuel",dataPackageInfo.tripfuel);
            response.setValue("vState",dataPackageInfo.vState);
            String OBD = "[";
            for (PIDInfo i : dataPackageInfo.obdData) {
                OBD+=i.pidType+":"+i.value+";";
            }
            OBD+="]";
            response.setValue("OBD",OBD);
            String Freeze = "[";
            for (PIDInfo i : dataPackageInfo.freezeData) {
                Freeze+=i.pidType+":"+i.value+";";
            }
            Freeze+="]";
            response.setValue("Freeze",Freeze);
            response.setValue("surportPid",dataPackageInfo.surportPid);
            response.setValue("dtcData",dataPackageInfo.dtcData);
            ldr.saveData("Responses",response.getValues());
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
        findViewById(R.id.loading).setVisibility(View.VISIBLE);
        if (BluetoothManage.getInstance(this).getState() != BluetoothManage.CONNECTED) {
            BluetoothManage.getInstance(this).connectBluetooth();
        }else {
            BluetoothManage.getInstance(this).obdSetMonitor(1,"");
            ((TextView) findViewById(R.id.debug_log)).setText("Waiting for response");
        }
    }

    private class UploadInfoOnline extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            pendingUpload = true;
            final LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
            ArrayList<String> devices = ldr.getDistinctDataSet("Responses","deviceId");
            for (final String device : devices){
                ParseObject object = ParseObject.create("Scan");
                String pid = "",freeze = "",dtc = "";
                final ArrayList<DBModel> responses = ldr.getDataSet("Responses",device);
                for (DBModel model:responses){
                    pid +=model.getValue("rtcTime") + ":" + model.getValue("supportPid");
                    freeze +=model.getValue("rtcTime") + ":" + model.getValue("Freeze");
                    dtc +=model.getValue("rtcTime") + ":" + model.getValue("dtcData");
                }
                final int count = responses.size();
                object.add("DTCs",dtc);
                object.add("freezeData",freeze);
                object.add("PIDs",pid);
                object.add("scannerId",device);
                object.saveEventually(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        Toast.makeText(getBaseContext(),"Uploaded data online",Toast.LENGTH_SHORT).show();
                        pendingUpload = false;
                        findViewById(R.id.loading).setVisibility(View.GONE);
                        ldr.deleteData("Responses",device);
                        Uploads upload = new Uploads();
                        String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                        upload.setValue("UploadedAt",timeStamp);
                        upload.setValue("EntriesUploads",""+count);
                        ldr.saveData("Responses",upload.getValues());
                    }
                });
            }
            return null;
        }
    }
}
