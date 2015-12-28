package com.pitstop;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
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
import com.pitstop.background.BluetoothAutoConnectService;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Uploads;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

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

    private class UploadInfoOnline extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... params) {
            pendingUpload = true;
            final LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
            ArrayList<String> devices = ldr.getDistinctDataSet("Responses","deviceId");
            for (final String device : devices) {
                ParseObject object = ParseObject.create("Scan");
                String pid = "{", freeze = "{", dtc = "{";
                final ArrayList<DBModel> responses = ldr.getDataSet("Responses", "deviceId", device);
                boolean firstp = false, firstf = false, firstd = false;
                for (DBModel model : responses) {
                    if ((model.getValue("OBD") != null) && (!model.getValue("OBD").equals("{}"))&&model.getValue("result").equals("6")) {
                        pid += (firstp ? "," : "") + "'" + model.getValue("rtcTime") + "':" + model.getValue("OBD") + "";
                        firstp = true;
                    }
                    if ((model.getValue("Freeze") != null) && (!model.getValue("Freeze").equals("{}"))) {
                        freeze += (firstf ? "," : "") + "'" + model.getValue("rtcTime") + "':" + model.getValue("Freeze");
                        firstf = true;
                    }
                    if ((model.getValue("dtcData") != null) && (!model.getValue("dtcData").equals(""))) {
                        dtc += (firstd ? "," : "") + "'" + model.getValue("rtcTime") + "':'" + model.getValue("dtcData") + "'";
                        firstd = true;
                    }
                }
                pid += "}";
                freeze += "}";
                dtc += "}";
                final int count = responses.size();
                if (count > 0) {
                    try {
                        object.put("DTCArray", new JSONObject(dtc));
                        object.put("freezeDataArray", new JSONObject(freeze));
                        object.put("PIDArray2", new JSONObject(pid));
                        object.put("scannerId", device);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    object.saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            findViewById(R.id.loading).setVisibility(View.GONE);
                            if (e == null) {
                                Toast.makeText(getBaseContext(), "Uploaded data online", Toast.LENGTH_SHORT).show();
                                pendingUpload = false;
                                ldr.deleteData("Responses", "deviceId", device);
                                Uploads upload = new Uploads();
                                String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                                upload.setValue("UploadedAt", timeStamp);
                                upload.setValue("EntriesUploaded", "" + count);
                                ldr.saveData("Uploads", upload.getValues());
                            } else {
                                Log.d("Cant upload", e.getMessage());
                            }
                        }
                    });
                }else{
                    Toast.makeText(getBaseContext(),"Wait to accumulate more information",Toast.LENGTH_SHORT).show();
                }
            }
            return null;
        }
    }
}
