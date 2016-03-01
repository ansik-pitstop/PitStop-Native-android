package com.pitstop.background;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.PIDInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;
import com.pitstop.MainActivity;
import com.pitstop.R;
import com.pitstop.database.DBModel;
import com.pitstop.database.LocalDataRetriever;
import com.pitstop.database.models.Cars;
import com.pitstop.database.models.Responses;
import com.pitstop.database.models.Uploads;
import com.pitstop.parse.ParseApplication;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by David Liu on 11/30/2015.
 */
public class BluetoothAutoConnectService extends Service implements BluetoothManage.BluetoothDataListener{

    private final IBinder mBinder = new BluetoothBinder();
    private BluetoothManage.BluetoothDataListener serviceCallbacks;

    private int counter;
    private boolean askforDtcs;
    private int notifID= 1360119;

    String[] pids = new String[0];

    private boolean gettingPIDs = false;
    int checksDone =0;
    int pidI = 0;
    private int status5counter;
    boolean gettingPID =false;

    private boolean deviceConnState = false;
    private String currentDeviceId = null;
    private Cars currentCar = null;

    private static String DTAG = "BLUETOOTH_DEBUG";
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        askforDtcs = false;
        status5counter=0;
        counter = 1;
        Log.i(DTAG,"Creating auto-connect bluetooth service");
        BluetoothManage.getInstance(this).setBluetoothDataListener(this);
    }

    public boolean getDeviceConnState() {
        return deviceConnState;
    }

    public String getCurrentDeviceId() {
        return currentDeviceId;
    }

    public void setCurrentCar(Cars car) {
        currentCar = car;
    }

    public Cars getCurrentCar() {
        return currentCar;
    }

    /**
     * Gets the Car's VIN
     */
    public void getCarVIN(){
        Log.i(DTAG,"Calling getCarVIN from Bluetooth auto-connect");
        BluetoothManage.getInstance(this).obdGetParameter("2201");
    }

    public void setRTCTime(){
        Log.d("SETTINGRTCTIME", "SETTING");
        long currentTime = System.currentTimeMillis();
        BluetoothManage.getInstance(this).obdSetParameter("1A01", String.valueOf(currentTime / 1000));
    }

    public void startBluetoothSearch(boolean isAddCar){
        Log.i(DTAG, "starting bluetooth search - auto-connect service");
        BluetoothManage.getInstance(this).connectBluetooth(isAddCar);
    }

    public int getState(){
        Log.i(DTAG, "getting bluetooth state - auto-connect service");
        return BluetoothManage.getInstance(this).getState();
    }

    public void setIsAddCarState(boolean isAddCarState) {
        Log.i(DTAG,"Setting is add car state");
        BluetoothManage.getInstance(this).setIsAddCarActivityState(isAddCarState);
    }

    public void getPIDs(){
        Log.i(DTAG,"getting PIDs - auto-connect service");
        BluetoothManage.getInstance(this).obdGetParameter("2401");
        gettingPID=true;
    }

    public void getDTCs() {
        Log.i(DTAG, "calling getting DTCs - auto-connect service");
        if (!askforDtcs){
            askforDtcs = true;
            BluetoothManage.getInstance(this).obdSetMonitor(1, "");
        }
    }

    public void getFreeze(){
        Log.i(DTAG, "Getting freeze data - auto-connect service");
        BluetoothManage.getInstance(this).obdSetMonitor(3, "");
    }

    public String parseDTCs(String hex){
        Log.i(DTAG,"Parsing DTCs - auto-connect service");
        int start = 1;
        char head = hex.charAt(0);
        HashMap<Character, String> map = new HashMap<Character, String>();
        map.put('0',"P0");
        map.put('1',"P1");
        map.put('2',"P2");
        map.put('3',"P3");

        map.put('4',"C0");
        map.put('5',"C1");
        map.put('6',"C2");
        map.put('7',"C3");

        map.put('8',"B0");
        map.put('9',"B1");
        map.put('A',"B2");
        map.put('B',"B3");

        map.put('C',"U0");
        map.put('D',"U1");
        map.put('E',"U2");
        map.put('F',"U3");
        return map.get(head)+hex.substring(start);
    }

    private void sendForPIDS(){
        Log.i(DTAG, "Sending for PIDS - auto-connect service");
        gettingPIDs = true;
        String pid="";
        while(pidI!=pids.length){
            pid+=pids[pidI]+",";
            if  ((pidI+1)%9 ==0){
                BluetoothManage.getInstance(this)
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
                pidI++;
                return;
            }else if ((pidI+1)==pids.length){
                BluetoothManage.getInstance(this)
                        .obdSetMonitor(4, pid.substring(0,pid.length()-1));
            }
            pidI++;
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //BluetoothManage.getInstance(this).connectBluetooth();
        Log.i(DTAG, "Running on start command - auto-connect service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(DTAG,"Destroying auto-connect service");
        Toast.makeText(this, "Destroyed",Toast.LENGTH_SHORT).show();
        super.onDestroy();
        BluetoothManage.getInstance(this).close();
    }

    @Override
    public void getBluetoothState(int state) {
        Log.i(DTAG, "Getting bluetooth state - auto-connect service");
        if(state==BluetoothManage.CONNECTED) {
            Log.i(DTAG,"Bluetooth state connected - auto-connect service");
            Log.i(DTAG,"getting bonded devices - auto-connect service");
            BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
            List<BluetoothDevice> devices = new LinkedList<>();
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
                devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
            }
            boolean deviceConnected = false;
            for (BluetoothDevice device : devices) {
                Log.i(DTAG,"Iterating through bonded devices - auto-connect service");
                //if device has name IDD-212
                if (device.getName().contains("IDD-212")) {
                    Log.i(DTAG,"Found connected device - auto-connect service");
                    deviceConnected = true;
                }
            }
            //set RTC time once anything is connected
            //show a custom notification
            //setRTCTime();
            //show a custom notification
            if (deviceConnected) {
                try {// mixpanel stuff
                    if(ParseApplication.mixpanelAPI!=null){
                        ParseApplication.mixpanelAPI.track("Peripheral Connection Status", new JSONObject("{'Status':'Connected'}"));
                        ParseApplication.mixpanelAPI.flush();
                    }else{
                        ParseApplication.setUpMixPanel();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i(DTAG,"Device is connected -  auto-connect service");
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                .setSmallIcon(R.drawable.ic_directions_car_white_24dp)
                                .setColor(getResources().getColor(R.color.highlight))
                                .setContentTitle("Car is Connected")
                                .setContentText("Click here to check out more");
                // Creates an explicit intent for an Activity in your app
                Intent resultIntent = new Intent(this, MainActivity.class);

                // The stack builder object will contain an artificial back stack for the
                // started Activity.
                // This ensures that navigating backward from the Activity leads out of
                // your application to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
                // Adds the back stack for the Intent (but not the Intent itself)
                stackBuilder.addParentStack(MainActivity.class);
                // Adds the Intent that starts the Activity to the top of the stack
                stackBuilder.addNextIntent(resultIntent);
                PendingIntent resultPendingIntent =
                        stackBuilder.getPendingIntent(
                                0,
                                PendingIntent.FLAG_UPDATE_CURRENT
                        );
                mBuilder.setContentIntent(resultPendingIntent);
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Log.i(DTAG, "sending out car is connected notification - auto-connect service");
                mNotificationManager.notify(notifID, mBuilder.build());
            } else {
                NotificationManager mNotificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                Log.i(DTAG, "cancelling car is connected notification");
                mNotificationManager.cancel(notifID);
            }

            if (serviceCallbacks != null) {
                Log.i(DTAG, "Calling service callbacks to getBluetooth State - auto connect service");
                serviceCallbacks.getBluetoothState(state);
            }


        } else {// car not connected
            try {// mixpanel stuff
                if(ParseApplication.mixpanelAPI!=null){
                    ParseApplication.mixpanelAPI.track("Peripheral Connection Status", new JSONObject("{'Status':'Disconnected (Can be any device! May not be our hardware!)'}"));
                    ParseApplication.mixpanelAPI.flush();
                }else{
                    ParseApplication.setUpMixPanel();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
			deviceConnState = false;
            currentCar = null;
        }
    }

    @Override
    public void setCtrlResponse(ResponsePackageInfo responsePackageInfo) {
        if(serviceCallbacks!=null) {
            Log.i(DTAG,"Setting ctrl response on service callbacks - auto-connect service");
            serviceCallbacks.setCtrlResponse(responsePackageInfo);
        }

    }

    @Override
    public void setParamaterResponse(ResponsePackageInfo responsePackageInfo) {
        if(serviceCallbacks!=null) {
            Log.i(DTAG, "Setting parameter response on service callbacks - auto-connect service");
            serviceCallbacks.setParamaterResponse(responsePackageInfo);
        }


    }

    @Override
    public void getParamaterData(ParameterPackageInfo parameterPackageInfo) {
        if(gettingPID){
            Log.i(DTAG,"Getting parameter data- auto-connect service");
            pids  =parameterPackageInfo.value.get(0).value.split(",");
            pidI = 0;
            sendForPIDS();
            gettingPID=false;
        }else if(serviceCallbacks!=null) {
            Log.i(DTAG, "getting parameter data on service Callbacks - auto-connect service");
            serviceCallbacks.getParamaterData(parameterPackageInfo);
        }
    }

    @Override
    public void getIOData(DataPackageInfo dataPackageInfo) {
        deviceConnState = true;
        currentDeviceId  = dataPackageInfo.deviceId;

        Log.i(DTAG, "getting io data - auto-connect service");
        if (dataPackageInfo.result != 5&&dataPackageInfo.result!=4&&askforDtcs) {
            askforDtcs=false;
            String dtcs = "";
            if(dataPackageInfo.dtcData!=null&&dataPackageInfo.dtcData.length()>0){
                String[] DTCs = dataPackageInfo.dtcData.split(",");
                for(String dtc : DTCs) {
                    dtcs+=parseDTCs(dtc)+",";
                }
            }
            //update DTC to online
            ParseObject scansSave = new ParseObject("Scan");
            scansSave.put("DTCs", dtcs);
            scansSave.put("scannerId", dataPackageInfo.deviceId);
            scansSave.put("runAfterSave", true);
            scansSave.saveEventually(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    Log.d("DTC Saving", "DTCs saved");
                }
            });
            if (serviceCallbacks != null)
                serviceCallbacks.getIOData(dataPackageInfo);
            return;
        }
        counter ++;
        //keep looking for pids until all pids are recieved
        if(pidI!=pids.length&&dataPackageInfo.result!=5){
            sendForPIDS();
        }
        //because theres a lot of status 5, keep looking
        if(dataPackageInfo.result==5){
            status5counter++;
        }
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        Responses response = new Responses();
        if(dataPackageInfo.result==1||dataPackageInfo.result==3||dataPackageInfo.result==4||dataPackageInfo.result==6||status5counter%20==1) {
            if (status5counter % 20 == 1)
                status5counter = 1;
            response.setValue("result", "" + dataPackageInfo.result);
            response.setValue("deviceId", dataPackageInfo.deviceId);
            response.setValue("tripId", dataPackageInfo.tripId);
            response.setValue("dataNumber", dataPackageInfo.dataNumber);
            response.setValue("tripFlag", dataPackageInfo.tripFlag);
            response.setValue("rtcTime", dataPackageInfo.rtcTime);
            response.setValue("protocolType", dataPackageInfo.protocolType);
            response.setValue("tripMileage", dataPackageInfo.tripMileage);
            response.setValue("tripfuel", dataPackageInfo.tripfuel);
            response.setValue("vState", dataPackageInfo.vState);
            String OBD = "{";
            boolean recordedOnce = false;
            for (PIDInfo i : dataPackageInfo.obdData) {
                OBD += (recordedOnce ? ";'" : "'") + i.pidType + "':" + i.value;
                recordedOnce = true;
            }
            OBD += "}";
            response.setValue("OBD", OBD);
            if (pidI<=pids.length&&gettingPIDs&&dataPackageInfo.obdData.size()>0) {
                if(pidI==pids.length){
                    gettingPIDs= false;
                }
                JSONObject Freeze = new JSONObject();
                JSONArray arrayOfPids = new JSONArray();
                try {
                    //freeze data will need to be stored ina  different format (PIDS)
                    Freeze.put("time", dataPackageInfo.rtcTime);
                    JSONObject individual = new JSONObject();
                    for (PIDInfo i : dataPackageInfo.obdData) {
                        individual.put("id", i.pidType);
                        individual.put("data", i.value);
                        arrayOfPids.put(individual);
                    }
                    Freeze.put("pids", arrayOfPids);
                    response.setValue("Freeze", Freeze.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            response.setValue("supportPid", dataPackageInfo.surportPid);
            response.setValue("dtcData", dataPackageInfo.dtcData);
            Log.i(DTAG, "IO data saving to local db - auto-connect service");
            ldr.saveData("Responses", response.getValues());

            if (serviceCallbacks != null) {
                Log.i(DTAG, "calling service callbacks for getIOdata - auto-connect service");
                serviceCallbacks.getIOData(dataPackageInfo);
            }

        }
        if(counter%20==0){
            getPIDs();
        }
        if(counter%50==0){
            getDTCs();
        }
        if(counter==100){
            counter = 1;
            uploadRecords();
        }
    }

    public class BluetoothBinder extends Binder {
        public BluetoothAutoConnectService getService() {
            return BluetoothAutoConnectService.this;
        }
    }

    public void setCallbacks(BluetoothManage.BluetoothDataListener callbacks) {
        Log.i(DTAG, "setting call backs - auto-connect service");
        serviceCallbacks = callbacks;
    }


    public void uploadRecords() {
        try {// mixpanel stuff
            if(ParseApplication.mixpanelAPI!=null){
                ParseApplication.mixpanelAPI.track("Peripheral Connection Status", new JSONObject("{'Status':'Uploading Data'}"));
                ParseApplication.mixpanelAPI.flush();
            }else{
                ParseApplication.setUpMixPanel();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.i(DTAG, "Uploading database records");
        LocalDataRetriever ldr = new LocalDataRetriever(this);
        DBModel entry = ldr.getLastRow("Uploads", "UploadID");
        if(entry==null){
            ArrayList<DBModel> array  = ldr.getAllDataSet("Responses");
            UploadInfoOnline uploadInfoOnline = new UploadInfoOnline();
            uploadInfoOnline.execute(array.get(0).getValue("ResponseID"),
                    array.get(array.size()-1).getValue("ResponseID"));
        }else{
            DBModel lastResponse = ldr.getLastRow("Responses", "ResponseID");
            UploadInfoOnline uploadInfoOnline = new UploadInfoOnline();
            uploadInfoOnline.execute(entry.getValue("EntriesEnd"),
                    lastResponse.getValue("ResponseID"));
        }
    }


    private class UploadInfoOnline extends AsyncTask<String,Void,Void> {

        @Override
        protected Void doInBackground(String... params) {
            Log.i(DTAG,"Uploading info online (async task) - auto-connect service");
            final LocalDataRetriever ldr = new LocalDataRetriever(getApplicationContext());
            ArrayList<String> devices = ldr.getDistinctDataSet("Responses","deviceId");
            for (final String device : devices) {
                ParseObject object = ParseObject.create("Scan");
                String pid = "{", freeze = "{", dtc = "{";
                final ArrayList<DBModel> responses = ldr.getResponse(device, params[0],params[1]);
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

                    final Uploads upload = new Uploads();
                    upload.setValue("EntriesStart", responses.get(0).getValue("ResponseID"));
                    upload.setValue("EntriesEnd", responses.get(responses.size()-1).getValue("ResponseID"));
                    final long index = ldr.saveData("Uploads", upload.getValues());
                    try {
                        object.put("DTCArray", new JSONObject(dtc));
                        object.put("runAfterSave",false);
                        object.put("freezeDataArray", new JSONObject(pid));
                        object.put("PIDArray2", new JSONObject(freeze));
                        object.put("scannerId", device);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    object.saveEventually(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Toast.makeText(getBaseContext(), "Uploaded data online", Toast.LENGTH_SHORT).show();
                                ldr.deleteData("Responses", "deviceId", device);
                                final String timeStamp = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());
                                upload.setValue("UploadedAt", timeStamp);
                                ldr.updateData("Uploads", "UploadID",""+index, upload.getValues());
                            } else {
                                Log.d("Cant upload", e.getMessage());
                            }
                        }
                    });
                }else{
                }
            }
            return null;
        }
    }
}
