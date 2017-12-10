package com.castel.obd.bleDevice;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

import com.castel.obd.bluetooth.BluetoothCommunicator;
import com.pitstop.bluetooth.BluetoothDeviceManager;
import com.pitstop.interactors.get.GetPrevIgnitionTimeUseCase;

import java.util.UUID;

/**
 * Created by Ben Wu on 2016-08-29.
 */
public interface AbstractDevice {

    UUID getServiceUuid();

    UUID getReadChar();

    UUID getWriteChar();
    


    BluetoothDeviceManager.CommType commType();

    byte[] getBytes(String payload);

    void parseData(byte[] data);
    void setManagerState(int state);

    void requestData(); // for 215 to ask for IDR

    String getDeviceName();

    // parameters
    void getVin();
    void getRtc();
    void setRtc(long rtcTime);
    void getPids(String pids);
    void getSupportedPids();
    void setPidsToSend(String pids, int timeInterval);
    void requestSnapshot();

    // monitor
    void clearDtcs();
    void getDtcs(); // stored
    void getPendingDtcs(); // pending
    void getFreezeFrame(); // FF

    void clearDeviceMemory();
    void resetDeviceToDefaults();
    void resetDevice();


    void createCommunicator(Context mContext);
    void connectToDevice(BluetoothDevice device);
    void sendPassiveCommand(String payload);
    void closeConnection();
    void setCommunicatorState(int state);
    int getCommunicatorState();
}
