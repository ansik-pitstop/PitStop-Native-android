package com.pitstop.bluetooth.communicator;

import android.content.Context;

import com.castel.obd.info.DataPackageInfo;
import com.castel.obd.info.LoginPackageInfo;
import com.castel.obd.info.ResponsePackageInfo;
import com.pitstop.bluetooth.dataPackages.DtcPackage;
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage;
import com.pitstop.bluetooth.dataPackages.ParameterPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;
import com.pitstop.models.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public class ObdManager {
    private static final String TAG = ObdManager.class.getSimpleName();

    public final static String BT_DEVICE_NAME_212 = "IDD-212";
    public final static String BT_DEVICE_NAME_215 = "IDD-215";
    public final static String BT_DEVICE_NAME = "IDD";
    public final static String CARISTA_DEVICE = "Carista";
    public static final String VIECAR_DEVICE = "Viecar";
    public static final String OBDII_DEVICE_NAME = "OBDII";
    public static final String OBD_LINK_MX = "OBDLink MX";
    public final static String FIXED_UPLOAD_TAG = "1202,1201,1203,1204,1205,1206";
    public final static String RTC_TAG = "1A01";
    public final static String VIN_TAG = "2201";
    public final static String PID_TAG = "2401";

    // For result 4
    public final static String TRIP_START_FLAG = "0";
    public final static String FREEZE_FRAME_FLAG = "3";
    public final static String STORE_DTC_FLAG = "5";
    public final static String PENDING_DTC_FLAG = "6";
    public final static String TRIP_END_FLAG = "9";

    public final static int DEVICE_LOGIN_FLAG = 1;
    public final static int DEVICE_LOGOUT_FLAG = 0;
    public final static int TYPE_MONITOR_PID_DATA = 0;
    public final static int TYPE_DTC = 1;
    public final static int TYPE_PENDING_DTC = 2;
    public final static int TYPE_FREEZE_DATA = 3;

    private Context mContext;
    private IBluetoothDataListener dataListener;
    private boolean isParse = false;
    public List<DataPackageInfo> dataPackages;

    public ObdManager(Context context) {
        mContext = context;
        dataPackages = new ArrayList<>();
    }

    public boolean isParse() {
        return isParse;
    }

    /**
     *  Callbacks for obd functions
     */
    public interface IBluetoothDataListener {  // TODO: Remove unnecessary functions
        void getBluetoothState(int state);

        void setCtrlResponse(ResponsePackageInfo responsePackageInfo);

        void setParameterResponse(ResponsePackageInfo responsePackageInfo);

        void deviceLogin(LoginPackageInfo loginPackageInfo);

        void tripData(TripInfoPackage tripInfoPackage);

        void parameterData(ParameterPackage parameterPackage);

        void idrPidData(PidPackage pidPackage);

        void pidData(PidPackage pidPackage);

        void dtcData(DtcPackage dtcPackage);

        void ffData(FreezeFramePackage ffPackage);

        void scanFinished();

        void alarmEvent(Alarm alarm);

        void idrFuelEvent(String scannerID, double fuelConsumed);

        void onDevicesFound();

        void handleVinData(String vin, String deviceId);

        void onGotRtc(long l);

        void setDeviceName(String address);
    }

}
