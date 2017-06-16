package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import com.castel.obd215b.util.Utils;

import java.util.List;
import java.util.UUID;

/**
 * Created by Paul Soladoye on 19/04/2016.
 */
@TargetApi(Build.VERSION_CODES.M)
public class WriteCommand {

    public byte[] bytes;
    private WRITE_TYPE type;

    private UUID serviceUuid;
    private UUID writeChar;
    private UUID readChar;

    public WriteCommand(byte[] bytes, WRITE_TYPE type, UUID serviceUuid, UUID writeChar, UUID readChar) {
        this.bytes = bytes;
        this.type = type;
        this.serviceUuid = serviceUuid;
        this.writeChar = writeChar;
        this.readChar = readChar;
    }

    public void execute(BluetoothGatt gatt) {
        BluetoothGattService mainObdGattService =
                gatt.getService(serviceUuid);

        if(mainObdGattService == null) {
            return;
        }

        if(type == WRITE_TYPE.DATA) {

            BluetoothGattCharacteristic obdWriteCharacteristic =
                    mainObdGattService.getCharacteristic(writeChar);
            obdWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            obdWriteCharacteristic.setValue(bytes);
            Log.d("Write data", Utils.bytesToHexString(bytes));
            boolean result =  gatt.writeCharacteristic(obdWriteCharacteristic);

            Log.i("WriteCommandDebug", "Write result "+result);

        } else if( type == WRITE_TYPE.NOTIFICATION) {

            BluetoothGattCharacteristic obdReadCharacteristic =
                    mainObdGattService.getCharacteristic(readChar);

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
