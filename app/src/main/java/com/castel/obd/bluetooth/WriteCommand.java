package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import com.castel.obd215b.util.LogUtil;
import com.castel.obd215b.util.Utils;

import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Created by Paul Soladoye on 19/04/2016.
 */
@TargetApi(Build.VERSION_CODES.M)
public class WriteCommand {

    public byte[] bytes;
    private WRITE_TYPE type;

    public WriteCommand(byte[] bytes, WRITE_TYPE type) {
        this.bytes = bytes;
        this.type = type;
    }

    public void execute(BluetoothGatt gatt) {
        BluetoothGattService mainObdGattService =
                gatt.getService(Bluetooth215BComm.OBD_IDD_212_MAIN_SERVICE);  // TODO: make work with both devices

        if(mainObdGattService == null) {
            return;
        }

        if(type == WRITE_TYPE.DATA) {

            BluetoothGattCharacteristic obdWriteCharacteristic =
                    mainObdGattService.getCharacteristic(Bluetooth215BComm.OBD_WRITE_CHAR);
            //obdWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            obdWriteCharacteristic.setValue(bytes);
            Log.d("Write data", Utils.bytesToHexString(bytes));
            boolean result =  gatt.writeCharacteristic(obdWriteCharacteristic);

            String send = "";
            try {
                send = new String(bytes,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            Log.e("String sent", send);

            Log.i("WriteCommandDebug", "Write result "+result);

        } else if( type == WRITE_TYPE.NOTIFICATION) {

            BluetoothGattCharacteristic obdReadCharacteristic =
                    mainObdGattService.getCharacteristic(Bluetooth215BComm.OBD_READ_CHAR);

            Log.i("WriteCommandDebug", "Setting notification on: " + obdReadCharacteristic.getUuid());

            // Enable local notification
            gatt.setCharacteristicNotification(obdReadCharacteristic, true);

            // Enable remote notification
            List<BluetoothGattDescriptor> descriptors =
                    obdReadCharacteristic.getDescriptors();
            for(BluetoothGattDescriptor descriptor : descriptors) {
                Log.i("WriteCommandDebug", "descriptor: " + descriptor.getUuid());
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                boolean result = gatt.writeDescriptor(descriptor);
                Log.i("WriteCommandDebug", "Writing descriptor... result: " + result);
            }
        }
    }

    public enum WRITE_TYPE {
        NOTIFICATION,
        DATA
    }
}
