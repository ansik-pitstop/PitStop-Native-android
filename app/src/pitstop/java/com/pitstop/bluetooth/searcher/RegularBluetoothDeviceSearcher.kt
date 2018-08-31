package com.pitstop.bluetooth.searcher

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.util.Log
import com.pitstop.bluetooth.bleDevice.AbstractDevice
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger
import java.util.*

/**
 * Created by Karol Zdebel on 8/31/2018.
 */
class RegularBluetoothDeviceSearcher(private val useCaseComponent: UseCaseComponent) {

    private val TAG = RegularBluetoothDeviceSearcher::class.java.simpleName

    private val bluetoothAdapter: BluetoothAdapter? = null
    private var discoveryNum = 0
    private var nonUrgentScanInProgress = false
    private var discoveryWasStarted = false
    private var ignoreVerification = false
    private var btConnectionState = BluetoothCommunicator.DISCONNECTED
    private var deviceInterface: AbstractDevice? = null
    private var rssiScan = false
    private val foundDevices = HashMap<BluetoothDevice, Short>() //Devices found by receiver

    @Synchronized
    fun startScan(urgent: Boolean, ignoreVerification: Boolean): Boolean {
        Log.d(TAG, "startScan() urgent: " + java.lang.Boolean.toString(urgent)
                + " ignoreVerification: " + java.lang.Boolean.toString(ignoreVerification))
        this.ignoreVerification = ignoreVerification
        if (!bluetoothAdapter!!.isEnabled && !urgent) {
            Log.i(TAG, "Scan unable to start, bluetooth is disabled and non urgent scan")
            return false
        } else if (!bluetoothAdapter?.isEnabled && urgent) {
            Log.d(TAG, "urgent scan, bluetooth is enabled()")
            bluetoothAdapter.enable() //Enable bluetooth, scan will be triggered someplace else
            return false
        }

        if (bluetoothAdapter?.isDiscovering == true) {
            Log.i(TAG, "Already scanning")
            return false
        }

        return connectBluetooth(urgent)
    }

    @Synchronized
    private fun connectBluetooth(urgent: Boolean): Boolean {
        nonUrgentScanInProgress = !urgent //Set the flag regardless of whether a scan is in progress
        btConnectionState = if (deviceInterface == null) BluetoothCommunicator.DISCONNECTED else deviceInterface!!.communicatorState

        if (btConnectionState == BluetoothCommunicator.CONNECTED) {
            Log.i(TAG, "Bluetooth connected")
            return false
        }

        if (btConnectionState == BluetoothCommunicator.CONNECTING) {
            Log.i(TAG, "Bluetooth already connecting")
            return false
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
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

}