package com.pitstop.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.continental.rvd.mobile_sdk.SDKIntentService
import com.pitstop.application.GlobalApplication
import com.pitstop.bluetooth.bleDevice.AbstractDevice
import com.pitstop.bluetooth.bleDevice.Device212B
import com.pitstop.bluetooth.bleDevice.Device215B
import com.pitstop.bluetooth.bleDevice.ELM327Device
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator
import com.pitstop.bluetooth.communicator.ObdManager
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.bluetooth.elm.enums.ObdProtocols
import com.pitstop.bluetooth.searcher.RVDBLuetoothDeviceSearcherStatusListener
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcher
import com.pitstop.bluetooth.searcher.RegularBluetoothDeviceSearcher
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger

/**
 * Created by Ben!
 */
class BluetoothDeviceManager(private val mContext: Context
                             , sdkIntentService: SDKIntentService)
    : RVDBLuetoothDeviceSearcherStatusListener {

    private val TAG = BluetoothDeviceManager::class.java.simpleName

    private val application: GlobalApplication = mContext.applicationContext as GlobalApplication
    private var dataListener: ObdManager.IBluetoothDataListener? = null
    private val regularBluetoothDeviceSearcher: RegularBluetoothDeviceSearcher
    private val rvdBluetoothDeviceSearcher: RVDBluetoothDeviceSearcher

    private val deviceInterface: AbstractDevice? = null
    private val useCaseComponent: UseCaseComponent

    private var btConnectionState = BluetoothCommunicator.DISCONNECTED

    enum class CommType {
        CLASSIC, LE
    }

    enum class DeviceType {
        ELM327, OBD215, OBD212, RVD
    }

    init {
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(mContext))
                .build()

        regularBluetoothDeviceSearcher = RegularBluetoothDeviceSearcher(useCaseComponent, dataListener!!, mContext, this)
        rvdBluetoothDeviceSearcher = RVDBluetoothDeviceSearcher(sdkIntentService, this)
    }

    /*
     * Callback methods for RVD SDK device searcher interface
     */
    override fun onBindingRequired() {
        Log.d(TAG, "onBindingRequired()")
    }

    override fun onBindingQuestionPrompted(question: String) {
        Log.d(TAG, "onBindingQuestionPrompted() question: $question")

    }

    override fun onFirmwareUpdateRequired() {
        Log.d(TAG, "onFirmwareUpdateRequired()")
    }

    override fun onFirmwareUpdateStatus(status: Int) {
        Log.d(TAG, "onFirmwareUpdateStatus() status:$status")
    }

    override fun onError(err: String) {
        Log.d(TAG, "onError() err: $err")
    }

    override fun onConnectionCompleted(device: AbstractDevice) {

    }
    /*
     * End of callback methods for RVD SDK
     */

    fun getConnectionState(): Int{
        Log.d(TAG, "getConnectionState()")
        return btConnectionState
    }

    fun setState(state: Int) {
        Log.d(TAG, "setState() state: $state")
        this.btConnectionState = state
        dataListener!!.getBluetoothState(state)

    }

    fun onGotVin(VIN: String, deviceID: String) {
        Log.d(TAG, "onGotVin: $VIN")
        dataListener!!.handleVinData(VIN, deviceID)
    }

    fun gotDtcData(dtcPackage: DtcPackage) {
        Log.d(TAG, "gotDtcData")
        dataListener!!.dtcData(dtcPackage)
    }

    fun gotPidPackage(pidPackage: PidPackage) {
        Log.d(TAG, "pidData: " + pidPackage.toString())
        dataListener!!.idrPidData(pidPackage)
        dataListener!!.pidData(pidPackage)
    }

    fun onGotRtc(l: Long) {
        dataListener!!.onGotRtc(l)
    }

    fun setBluetoothDataListener(dataListener: ObdManager.IBluetoothDataListener) {
        this.dataListener = dataListener
    }

    @Synchronized
    fun startScan(urgent: Boolean, ignoreVerification: Boolean, deviceType: DeviceType): Boolean {
        Log.d(TAG, "startScan() urgent: " + java.lang.Boolean.toString(urgent) + " ignoreVerification: " + java.lang.Boolean.toString(ignoreVerification))
        return if (deviceType == DeviceType.ELM327 || deviceType == DeviceType.OBD212
                || deviceType == DeviceType.OBD215)
            regularBluetoothDeviceSearcher.startScan(urgent, ignoreVerification)
        else
            rvdBluetoothDeviceSearcher.start()
    }

    @Synchronized
    fun onConnectDeviceValid() {
        regularBluetoothDeviceSearcher.onConnectDeviceValid()
    }

    fun close() {
        Log.d(TAG, "close()")
        regularBluetoothDeviceSearcher.close()
    }


    fun closeDeviceConnection() {
        Log.d(TAG, "closeDeviceConnection()")
        deviceInterface?.closeConnection()
        btConnectionState = IBluetoothCommunicator.DISCONNECTED
    }

    fun bluetoothStateChanged(state: Int) {
        if (state == BluetoothAdapter.STATE_OFF) {
            btConnectionState = BluetoothCommunicator.DISCONNECTED
            if (deviceInterface != null) {
                deviceInterface.communicatorState = state
            }
        }
    }

    fun changeScanUrgency(urgent: Boolean) {
        regularBluetoothDeviceSearcher.changeScanUrgency(urgent)
    }

    fun getVin() {
        Log.d(TAG, "getVin() btConnectionState: $btConnectionState")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        Log.d(TAG, "deviceInterface.getVin()")
        val ret = deviceInterface!!.vin
        Log.d(TAG, "get vin returned $ret")
    }

    fun getRtc() {
        Log.d(TAG, "getRtc()")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        deviceInterface!!.rtc
        /*writeToObd(deviceInterface.getRtc());*/
    }

    fun setDeviceNameAndId(id: String) {
        Log.d(TAG, "setDeviceNameAndId() id: $id")
        //Device name should never be set for 212
        if (deviceInterface is Device215B) {
            val device215B = deviceInterface as Device215B?
            device215B!!.setDeviceNameAndId(ObdManager.BT_DEVICE_NAME_215 + " " + id, id)
        }
    }

    fun setDeviceId(id: String) {
        Log.d(TAG, "setDeviceId() id: $id")
        if (deviceInterface is Device215B) {
            val device215B = deviceInterface as Device215B?
            device215B!!.setDeviceId(id)
        }
    }

    fun clearDeviceMemory() {
        Log.d(TAG, "clearDeviceMemory() ")
        if (deviceInterface is Device215B) {
            deviceInterface.clearDeviceMemory()

        }
    }

    fun resetDeviceToDefaults() {

        Log.d(TAG, "resetToDefualts() ")
        if (deviceInterface is Device215B) {
            deviceInterface.resetDeviceToDefaults()
        }

    }

    fun resetDevice() {
        Log.d(TAG, "resetDevice() ")
        if (deviceInterface is Device215B) {
            deviceInterface.resetDevice()
        }

    }

    fun setRtc(rtcTime: Long) {
        Log.d(TAG, "setRtc() rtc: $rtcTime")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        deviceInterface!!.setRtc(rtcTime)
    }

    fun getPids(pids: String) {
        Log.d(TAG, "getPids() pids: $pids")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        deviceInterface!!.getPids(pids)
    }

    fun clearDtcs() {
        Log.d(TAG, "clearDTCs")
        if (deviceInterface is Device215B || deviceInterface is ELM327Device) {
            deviceInterface.clearDtcs()
        }
    }

    fun getSupportedPids() {
        Logger.getInstance()!!.logI(TAG, "Requested supported pid", DebugMessage.TYPE_BLUETOOTH)
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        deviceInterface!!.supportedPids
    }

    // sets pids to check and sets data interval
    fun setPidsToSend(pids: String, timeInterval: Int) {
        Logger.getInstance()!!.logI(TAG, "Set pids to be sent: $pids, interval: $timeInterval", DebugMessage.TYPE_BLUETOOTH)
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }
        deviceInterface!!.setPidsToSend(pids, timeInterval)
    }

    fun getDtcs() {
        Log.d(TAG, "getDtcs()")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            Log.d(TAG, " can't get Dtcs because my sate is not connected")
            return
        }
        deviceInterface!!.dtcs
        deviceInterface.pendingDtcs
    }

    fun getFreezeFrame() {
        Log.d(TAG, "getFreezeFrame()")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }

        deviceInterface!!.freezeFrame
    }

    fun requestData() {
        Log.d(TAG, "requestData()")
        if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            return
        }

        deviceInterface!!.requestData()
    }

    fun requestSnapshot() {
        Log.d(TAG, "requestSnapshot()")
        if (deviceInterface != null) {
            if ((deviceInterface is Device215B || deviceInterface is ELM327Device) && btConnectionState == BluetoothCommunicator.CONNECTED) {
                Log.d(TAG, "executing writeToOBD requestSnapshot()")
                deviceInterface.requestSnapshot()
            }
        }
    }

    fun requestDescribeProtocol(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestDescribeProtocol()
            return true
        } else {
            return false
        }
    }

    fun request2141PID(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.request2141PID()
            return true
        } else {
            return false
        }
    }

    fun requestStoredDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestStoredTroubleCodes()
            return true
        } else {
            return false
        }
    }

    fun requestPendingDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestPendingTroubleCodes()
            return true
        } else {
            return false
        }
    }

    fun requestSelectProtocol(p: ObdProtocols): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestSelectProtocol(p)
            return true
        } else {
            return false
        }
    }

    fun getDeviceType(): DeviceType?{
        Log.d(TAG, "isConnectedTo215()")
        return if (deviceInterface != null)
            when (deviceInterface) {
                is Device215B -> DeviceType.OBD215
                is Device212B -> DeviceType.OBD212
                is ELM327Device -> DeviceType.ELM327
                else -> null
            }
        else
            null
    }
}
