package com.pitstop.bluetooth.searcher

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import com.pitstop.bluetooth.BluetoothDeviceManager
import com.pitstop.bluetooth.bleDevice.AbstractDevice
import com.pitstop.bluetooth.bleDevice.Device212B
import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator
import com.pitstop.bluetooth.communicator.ObdManager
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger
import java.util.*

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RegularBluetoothDeviceSearcher(private val useCaseComponent: UseCaseComponent
                                     , private val dataListener: ObdManager.IBluetoothDataListener
                                     , private val context: Context
                                     , private val manager: BluetoothDeviceManager) {

    private val TAG = RegularBluetoothDeviceSearcher::class.java.simpleName

    private var bluetoothAdapter: BluetoothAdapter
    private var discoveryNum = 0
    private var nonUrgentScanInProgress = false
    private var discoveryWasStarted = false
    private var ignoreVerification = false
    private var btConnectionState = BluetoothCommunicator.DISCONNECTED
    private var deviceInterface: AbstractDevice? = null
    private var rssiScan = false
    private val foundDevices = HashMap<BluetoothDevice, Short>() //Devices found by receiver

    // for classic discovery
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action

            //Found device
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, java.lang.Short.MIN_VALUE)
                Log.d(TAG, "name: " + device.name + ", address: " + device.address + " RSSI: " + rssi)
                //Store all devices in a map
                if (device.name != null && (device.name.contains(ObdManager.BT_DEVICE_NAME) ||
                                device.name.equals(ObdManager.CARISTA_DEVICE, ignoreCase = true)
                                || device.name.equals(ObdManager.VIECAR_DEVICE, ignoreCase = true)
                                || device.name.equals(ObdManager.OBDII_DEVICE_NAME, ignoreCase = true)
                                || device.name.equals(ObdManager.OBD_LINK_MX, ignoreCase = true))
                        && !foundDevices.containsKey(device)) {
                    foundDevices[device] = rssi
                    Log.d(TAG, "foundDevices.put() device name: " + device.name)
                } else {
                    Log.d(TAG, "Device did not meet criteria for foundDevice list name=" + device.name)
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                discoveryWasStarted = false
                discoveryNum++
                Logger.getInstance()!!.logI(TAG, "Discovery finished", DebugMessage.TYPE_BLUETOOTH)

                //Connect to device with strongest signal if scan has been requested
                if (rssiScan) {
                    rssiScan = false
                    dataListener.onDevicesFound()
                    var foundDevicesString = "{"
                    for ((key, value) in foundDevices) {
                        foundDevicesString += key.name + "=" + value + ","
                    }
                    foundDevicesString += "}"

                    Logger.getInstance()!!.logI(TAG, "Found devices: $foundDevicesString", DebugMessage.TYPE_BLUETOOTH)
                    if (foundDevices.size > 0) {
                        //Try to connect to available device, if none qualify then finish scan
                        if (!connectToNextDevice()) dataListener.scanFinished()
                    } else {
                        //Notify scan finished after 0.5 seconds due to delay
                        // in receiving CONNECTING notification
                        dataListener.scanFinished()
                    }
                }

            }//Finished scanning
        }
    }

    init{
        // for classic discovery
        val intentFilter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        context.registerReceiver(receiver, intentFilter)
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    @Synchronized
    fun startScan(urgent: Boolean, ignoreVerification: Boolean): Boolean {
        Log.d(TAG, "startScan() urgent: " + java.lang.Boolean.toString(urgent)
                + " ignoreVerification: " + java.lang.Boolean.toString(ignoreVerification))
        this.ignoreVerification = ignoreVerification
        if (!bluetoothAdapter.isEnabled && !urgent) {
            Log.i(TAG, "Scan unable to start, bluetooth is disabled and non urgent scan")
            return false
        } else if (!bluetoothAdapter.isEnabled && urgent) {
            Log.d(TAG, "urgent scan, bluetooth is enabled()")
            bluetoothAdapter.enable() //Enable bluetooth, scan will be triggered someplace else
            return false
        }

        if (bluetoothAdapter.isDiscovering) {
            Log.i(TAG, "Already scanning")
            return false
        }

        return connectBluetooth(urgent)
    }

    fun changeScanUrgency(urgent: Boolean) {
        this.nonUrgentScanInProgress = !urgent
    }

    @Synchronized
    private fun connectBluetooth(urgent: Boolean): Boolean {
        nonUrgentScanInProgress = !urgent //Set the flag regardless of whether a scan is in progress
        btConnectionState = if (deviceInterface == null) BluetoothCommunicator.DISCONNECTED else deviceInterface!!.getCommunicatorState()

        if (btConnectionState == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Bluetooth connected")
            return false
        }

        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting")
            return false
        }

        if (bluetoothAdapter.isEnabled) {
            Log.i(TAG, "Bluetooth not enabled or BluetoothAdapt is null")
            return false
        }

        //Order matters in the IF condition below, if rssiScan=true then discovery will not be started
        if (!rssiScan && bluetoothAdapter.startDiscovery()) {
            Logger.getInstance()!!.logI(TAG, "Discovery started", DebugMessage.TYPE_BLUETOOTH)
            //If discovery takes longer than 20 seconds, timeout and cancel it
            discoveryWasStarted = true
            discoveryNum++
            useCaseComponent.discoveryTimeoutUseCase().execute(discoveryNum, { timerDiscoveryNum ->
                if (discoveryNum == timerDiscoveryNum && discoveryWasStarted) {
                    Logger.getInstance()!!.logE(TAG, "Discovery timeout", DebugMessage.TYPE_BLUETOOTH)
                    bluetoothAdapter.cancelDiscovery()
                }
            })

            rssiScan = true
            foundDevices.clear() //Reset found devices map from previous scan
            return true
        } else {
            return false
        }
    }

    //Returns whether a device qualified for connection
    @Synchronized
    fun connectToNextDevice(): Boolean {
        Log.d(TAG, "connectToNextDevice(), foundDevices count: " + foundDevices.keys.size)

        val minRssiThreshold: Short
        var strongestRssi = java.lang.Short.MIN_VALUE
        var strongestRssiDevice: BluetoothDevice? = null

        if (nonUrgentScanInProgress) {
            minRssiThreshold = -70

        } else {
            minRssiThreshold = java.lang.Short.MIN_VALUE
        }//Deliberate scan, connect regardless

        Log.d(TAG, "minRssiThreshold set to: $minRssiThreshold")

        for ((key, value) in foundDevices) {
            if (value != null && value > strongestRssi) {
                strongestRssiDevice = key
                strongestRssi = value
            }
        }
        Log.d(TAG, "strongest rssi: $strongestRssi")
        if (strongestRssiDevice == null || strongestRssi < minRssiThreshold) {
            Log.d(TAG, "No device was found as candidate for a potential connection.")
            foundDevices.clear()
            return false

        }

        //Close previous connection
        if (deviceInterface != null) {
            deviceInterface?.closeConnection()
        }

        if (strongestRssiDevice.name.contains(ObdManager.BT_DEVICE_NAME_212)) {
            Log.d(TAG, "device212 > RSSI_Threshold, device: $strongestRssiDevice")

            foundDevices.remove(strongestRssiDevice)
            connectTo212Device(strongestRssiDevice)
        } else if (strongestRssiDevice.name.contains(ObdManager.BT_DEVICE_NAME_215)) {
            Log.d(TAG, "device215 > RSSI_Threshold, device: $strongestRssiDevice")
            foundDevices.remove(strongestRssiDevice)
            connectTo215Device(strongestRssiDevice)
        } else if (strongestRssiDevice.name.contains(ObdManager.CARISTA_DEVICE) ||
                strongestRssiDevice.name.contains(ObdManager.VIECAR_DEVICE) ||
                strongestRssiDevice.name.contains(ObdManager.OBDII_DEVICE_NAME) ||
                strongestRssiDevice.name.contains(ObdManager.OBD_LINK_MX)) {
            Log.d(TAG, "CaristaDevice> RSSI_Threshold, device: $strongestRssiDevice")
            foundDevices.remove(strongestRssiDevice)
            connectToELMDevice(strongestRssiDevice)
        }

        return true
    }

    private fun connectTo212Device(device: BluetoothDevice) {
        Log.d(TAG, "connectTo212Device() device: " + device.name)
        deviceInterface = Device212B(context, dataListener, device.name, manager)

        (deviceInterface as Device212B).connectToDevice(device)
    }

    private fun connectTo215Device(device: BluetoothDevice) {
        Log.d(TAG, "connectTo215Device() device: " + device.name)
        deviceInterface = Device215B(context, dataListener, device.name, manager)

        (deviceInterface as Device215B).connectToDevice(device)

    }

    private fun connectToELMDevice(device: BluetoothDevice) {
        Log.d(TAG, "connectToELM327Device() device: " + device.name)
        deviceInterface = ELM327Device(context, manager)
        dataListener.setDeviceName(device.address)
        (deviceInterface as ELM327Device).connectToDevice(device)
    }

    fun moreDevicesLeft(): Boolean {
        return foundDevices.size > 0
    }

    @Synchronized
    fun onConnectDeviceValid() {
        if (bluetoothAdapter.isEnabled && bluetoothAdapter.isDiscovering) {
            Log.i(TAG, "Stopping scan")
            bluetoothAdapter.cancelDiscovery()
            dataListener.scanFinished()
        }
    }

    fun close() {
        Log.d(TAG, "close()")
        btConnectionState = IBluetoothCommunicator.DISCONNECTED
        if (bluetoothAdapter != null && bluetoothAdapter.isDiscovering) {
            bluetoothAdapter.cancelDiscovery()
            dataListener.scanFinished()
        }

        if (deviceInterface != null) {
            deviceInterface?.closeConnection()
        }
        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
            Log.d(TAG, "Receiver not registered")
        }

    }


}