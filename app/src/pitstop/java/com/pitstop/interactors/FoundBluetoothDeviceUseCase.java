package com.pitstop.interactors;

/**
 * Created by Karol Zdebel on 7/10/2017.
 */

public interface FoundBluetoothDeviceUseCase extends Interactor {

    public final static String BT_DEVICE_NAME_212 = "IDD-212";
    public final static String BT_DEVICE_NAME_215 = "IDD-215";
    public final static String BT_DEVICE_NAME = "IDD";
    public final static String BT_DEVICE_NAME_BROKEN = "XXXXXX";

    interface Callback{
        void onDeviceValid();
        void onDevice215Broken();
        void onDeviceNotIDD();
        void onError();
    }

    void execute(String scannerId, String scannerName);
}
