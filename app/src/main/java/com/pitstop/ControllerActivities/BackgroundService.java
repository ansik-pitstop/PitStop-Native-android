package com.pitstop.ControllerActivities;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.castel.obd.bluetooth.BluetoothChat;
import com.castel.obd.bluetooth.BluetoothManage;
import com.castel.obd.data.OBDInfoSP;
import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.ParameterPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.castel.obd.util.LogUtil;

import java.util.ArrayList;
import java.util.List;


public class BackgroundService extends Service{

    /*BluetoothManage class*/

    private final String BT_NAME = "IDD-212";// �������

    public final static int BLUETOOTH_CONNECT_SUCCESS = 0;
    public final static int BLUETOOTH_CONNECT_FAIL = 1;
    public final static int BLUETOOTH_CONNECT_EXCEPTION = 2;
    public final static int BLUETOOTH_READ_DATA = 4;
    public final static int CANCEL_DISCOVERY = 5;

    public final static int CONNECTED = 0;
    public final static int DISCONNECTED = 1;
    public final static int CONNECTTING = 2;
    private int btState = DISCONNECTED;

    private static Context mContext;
    private static BluetoothManage mInstance;
    private BluetoothChat mBluetoothChat;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothDataListener dataListener;

    public List<DataPackageInfo> dataPackages;

    private boolean isMacAddress = false;

    private boolean isParse = false;
    private List<String> dataLists = new ArrayList<String>();


    public Context context = this;
    public Handler handler = null;
    public static Runnable runnable = null;

    @Override
    public IBinder onBind(Intent intent){
        return null;
    }



    @Override
    public void onCreate() {
        //TODO: What if the bluetooth was connected to start with? Add code to account for that
        Toast.makeText(context, "Service Created!", Toast.LENGTH_LONG).show();

        handler = new Handler(context.getMainLooper());
        runnable = new Runnable() {
            public void run() {
//                android.os.Debug.waitForDebugger();
                Toast.makeText(context, "Service is still running", Toast.LENGTH_LONG).show();

                BluetoothManage mBM = BluetoothManage.getInstance(BackgroundService.this);

//                Intent dialogIntent = new Intent(BackgroundService.this, com.castel.obd.activity.MonitorActivity.class);
//                dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                startActivity(dialogIntent);

                if (mBM.getState() == mBM.CONNECTED) {
                    //Bluetooth is connected
                    //Activity doesn't work -> inflates views
//                    Intent dialogIntent = new Intent(BackgroundService.this, com.castel.obd.activity.MonitorActivity.class);
//                    dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                    startActivity(dialogIntent);

                    BluetoothMangeHelper mBMH = new BluetoothMangeHelper(BackgroundService.this);
                    while (mBM.getState() == mBM.CONNECTED) {
//                        mBMH.startRequest();
                    }

                } else {
                    mBM.connectBluetooth();
                    BluetoothMangeHelper mBMH = new BluetoothMangeHelper(BackgroundService.this);

                    while (mBM.getState() == mBM.CONNECTED) {
//                        mBMH.startRequest();
                    }
                }

                handler.postDelayed(runnable, 15000);
            }
        };

        handler.postDelayed(runnable, 15000);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Service stopped", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStart(Intent intent, int startid){
        Toast.makeText(this, "Service started by user.", Toast.LENGTH_LONG).show();
    }


    public void connectBluetooth() {
        if (btState == CONNECTED) {
            return;
        }

        //ProgressDialogUtil.show(mContext);
        LogUtil.i("Bluetooth state:CONNECTTING");
        btState = CONNECTTING;
        mBluetoothChat.closeConnect();

        //
        if (!mBluetoothAdapter.isEnabled()) {
            LogUtil.i("BluetoothAdapter.enable()");
            mBluetoothAdapter.enable();
        }

        String macAddress = OBDInfoSP.getMacAddress(mContext);
//		 macAddress = "8C:DE:52:71:F7:71";
//		macAddress = "8C:DE:52:71:F8:91";
        if (!"".equals(macAddress)) {
            isMacAddress = true;
            BluetoothDevice device = mBluetoothAdapter
                    .getRemoteDevice(macAddress);
            mBluetoothChat.connectBluetooth(device);
        } else {
            LogUtil.i("startDiscovery()");
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            mBluetoothAdapter.startDiscovery();
        }
    }


    public interface BluetoothDataListener {
        public void getBluetoothState(int state);

        public void setCtrlResponse(ResponsePackageInfo responsePackageInfo);

        public void setParamaterResponse(ResponsePackageInfo responsePackageInfo);

        public void getParamaterData(ParameterPackageInfo parameterPackageInfo);

        public void getIOData(DataPackageInfo dataPackageInfo);
    }
}
