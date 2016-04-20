package com.castel.obd.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.os.Build;
import android.util.Log;

import com.pitstop.MainActivity;

/**
 * Created by Paul Soladoye on 19/04/2016.
 */
@TargetApi(Build.VERSION_CODES.M)
public class WriteCommand extends BluetoothCommand {

    private byte[] bytes;
    private WRITE_TYPE type;

    public WriteCommand(byte[] bytes, WRITE_TYPE type) {
        this.bytes = bytes;
        this.type = type;
    }

    @Override
    public void execute(BluetoothGatt gatt) {
        BluetoothGattService mainObdGattService =
                gatt.getService(BluetoothLeComm.OBD_IDD_212_MAIN_SERVICE);

        if(type == WRITE_TYPE.DATA) {

            BluetoothGattCharacteristic obdWriteCharacteristic =
                    mainObdGattService.getCharacteristic(BluetoothLeComm.OBD_WRITE_CHAR);
            obdWriteCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
            obdWriteCharacteristic.setValue(bytes);
            Log.i(MainActivity.TAG, "Writing characteristic...");
            boolean result =  gatt.writeCharacteristic(obdWriteCharacteristic);

            Log.i(MainActivity.TAG, "Write result "+result);

        } else if( type == WRITE_TYPE.NOTIFICATION) {

            BluetoothGattCharacteristic obdReadCharacteristic =
                    mainObdGattService.getCharacteristic(BluetoothLeComm.OBD_READ_CHAR);

            Log.i(MainActivity.TAG, "Setting notification on: " + obdReadCharacteristic.getUuid());

            // Enable local notification
            gatt.setCharacteristicNotification(obdReadCharacteristic, true);

            // Enable remote notification
            BluetoothGattDescriptor descriptor =
                    obdReadCharacteristic.getDescriptor(BluetoothLeComm.CONFIG_DESCRIPTOR);
            Log.i(MainActivity.TAG, "descriptor: " + descriptor.getUuid());
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean result = gatt.writeDescriptor(descriptor);
            Log.i(MainActivity.TAG, "Writing descriptor... result: "+result);

        }


    }

    public enum WRITE_TYPE {
        NOTIFICATION,
        DATA
    }
}
