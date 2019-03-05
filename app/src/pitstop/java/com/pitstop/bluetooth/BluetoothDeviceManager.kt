package com.pitstop.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.continental.rvd.mobile_sdk.AvailableSubscriptions
import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.BindingQuestionType
import com.continental.rvd.mobile_sdk.RvdIntentService
import com.pitstop.application.GlobalApplication
import com.pitstop.bluetooth.bleDevice.*
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator
import com.pitstop.bluetooth.communicator.ObdManager
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.bluetooth.dataPackages.FreezeFramePackage
import com.pitstop.bluetooth.dataPackages.ParameterPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.bluetooth.elm.enums.ObdProtocols
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcher
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcherStatusListener
import com.pitstop.bluetooth.searcher.RegularBluetoothDeviceSearcher
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.Alarm
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger



/**
 *
 * Created by Ben!
 */
class BluetoothDeviceManager(private val mContext: Context
                             , rvdIntentService: RvdIntentService
                             , private val dataListener: ObdManager.IBluetoothDataListener)
    : RVDBluetoothDeviceSearcherStatusListener {

    private val TAG = BluetoothDeviceManager::class.java.simpleName

    private val application: GlobalApplication = mContext.applicationContext as GlobalApplication
    private val regularBluetoothDeviceSearcher: RegularBluetoothDeviceSearcher
    private val rvdBluetoothDeviceSearcher: RVDBluetoothDeviceSearcher

    private var deviceInterface: AbstractDevice? = null
    private var useCaseComponent: UseCaseComponent

    private var btConnectionState = BluetoothCommunicator.DISCONNECTED

    enum class CommType {
        CLASSIC, LE
    }

    enum class DeviceType(val type: String) {
        ELM327("ELM327"),
        OBD215("215B"),
        OBD212("212B"),
        RVD("RVD Continental");

        companion object {
            @JvmStatic
            fun fromString(text: String): DeviceType? {
                for (d in DeviceType.values()) {
                    if (d.type == text) {
                        return d
                    }
                }
                return null
            }
        }
    }

    init {
        useCaseComponent = DaggerUseCaseComponent.builder()
                .contextModule(ContextModule(mContext))
                .build()

        regularBluetoothDeviceSearcher = RegularBluetoothDeviceSearcher(useCaseComponent
                , mContext, dataListener, this)
        rvdBluetoothDeviceSearcher = RVDBluetoothDeviceSearcher(rvdIntentService, this,this)
    }

    fun startBinding(): Boolean{
        Log.d(TAG,"startBinding()")
        return rvdBluetoothDeviceSearcher.respondBindingRequest(true)
    }

    fun startFirmwareInstallation(): Boolean{
        Log.d(TAG,"startFirmwareInstallation()")
        return rvdBluetoothDeviceSearcher.respondFirmwareInstallationRequest(true)
    }

    fun cancelBinding(): Boolean{
        Log.d(TAG,"cancelBinding()")
        return rvdBluetoothDeviceSearcher.respondBindingRequest(false)
    }

    /*
     * Callback methods for RVD SDK device searcher interface
     */
    override fun onBindingRequired() {
        Log.d(TAG, "onBindingRequired()")
        dataListener.onBindingRequired()
    }

    override fun onBindingQuestionPrompted(question: BindingQuestion) {
        Log.d(TAG, "onBindingQuestionPrompted() question: $question")
        dataListener.onBindingQuestionPrompted(question)
    }

    override fun onFirmwareInstallationRequired() {
        Log.d(TAG, "onFirmwareInstallationRequired()")
        dataListener.onFirmwareInstallationRequired()
    }

    override fun onFirmwareInstallationProgress(progress: Float) {
        Log.d(TAG, "onFirmwareInstallationProgress() progress:$progress")
        dataListener.onFirmwareInstallationProgress(progress)
    }

    override fun onConnectionFailure(err: Error) {
        Log.d(TAG,"onConnectionFailure() err: $err")
    }

    override fun onConnectionCompleted() {
        Log.d(TAG,"onConnectionCompleted()")
    }

    override fun onBindingProgress(progress: Float) {
        Log.d(TAG,"onBindingProgress() progress: $progress")
        dataListener.onBindingProgress(progress)
    }

    override fun onBindingFinished() {
        Log.d(TAG,"onBindingFinished()")
        dataListener.onBindingFinished()
    }

    override fun onBindingError(err: Error) {
        Log.d(TAG,"onBindingError() err: $err")
        dataListener.onBindingError(err)
    }

    override fun onFirmwareInstallationFinished() {
        Log.d(TAG,"onFirmwareInstallationFinished()")
        dataListener.onFirmwareInstallationFinished()
    }

    override fun onFirmwareInstallationError(err: Error) {
        Log.d(TAG,"onFirmwareInstallationError() err: $err")
        dataListener.onFirmwareInstallationError(err)
    }

    fun onCompleted(device: AbstractDevice) {
        Log.d(TAG,"onCompleted()")
        deviceInterface = device
    }

    override fun onGotAvailableSubscriptions(subscriptions: AvailableSubscriptions) {
        Log.d(TAG,"onGotAvailableSubscriptions() subscriptions: $subscriptions")
        dataListener.onGotAvailableSubscriptions(subscriptions)
    }

    override fun onMessageFromDevice(message: String) {
        Log.d(TAG,"onMessageFromDevice() message: $message")
        dataListener.onMessageFromDevice(message)
    }

    /*
     * End of callback methods for RVD SDK
     */

    fun idrFuelEvent(scannerID: String, fuelConsumed: Double) {
        dataListener.idrFuelEvent(scannerID, fuelConsumed)
    }

    fun alarmEvent(alarm: Alarm) {
        dataListener.alarmEvent(alarm)
    }

    fun idrPidData(pidPackage: PidPackage) {
        dataListener.idrPidData(pidPackage)
    }

    fun ffData(ffPackage: FreezeFramePackage) {
        dataListener.ffData(ffPackage)
    }

    fun pidData(pidPackage: PidPackage) {
        dataListener.pidData(pidPackage)
    }

    fun parameterData(parameterPackage: ParameterPackage) {
        dataListener.parameterData(parameterPackage)
    }

    fun getConnectionState(): Int{
        Log.d(TAG, "getConnectionState()")
        return btConnectionState
    }

    fun connectToNextDevice(): Boolean{
        return regularBluetoothDeviceSearcher.connectToNextDevice()
    }

    fun setState(state: Int) {
        Log.d(TAG, "setState() state: $state")
        this.btConnectionState = state
        dataListener.getBluetoothState(state)

    }

    fun answerBindingQuestion(questionType: BindingQuestionType, answer: String): Boolean{
        rvdBluetoothDeviceSearcher.answerBindingQuestion(questionType,answer)
        return true
    }

    fun onGotVin(VIN: String, deviceID: String) {
        Log.d(TAG, "onGotVin: $VIN")
        dataListener!!.handleVinData(VIN, deviceID)
    }

    fun onGotSupportedPids(supportedPids: List<String>, deviceId: String){
        Log.d(TAG,"onGotSupportedPids() supportedPids: $supportedPids")
        val parameterPackage = ParameterPackage()
        parameterPackage.deviceId = deviceId
        parameterPackage.paramType = ParameterPackage.ParamType.SUPPORTED_PIDS
        parameterPackage.success = true
        parameterPackage.value = supportedPids.reduceIndexed { index, acc, s ->
            if(supportedPids.lastIndex != index){
                "$s,"
            }else{
                s
            }
        }
        dataListener!!.parameterData(parameterPackage)
    }

    fun onGotDtcData(dtcPackage: DtcPackage) {
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

    fun scanFinished(){
        Log.d(TAG,"scanFinished()")
        dataListener.scanFinished()
    }

    fun onDevicesFound(){
        Log.d(TAG,"onFoundDevices()")
        dataListener.onDevicesFound()
    }

    @Synchronized
    fun startScan(urgent: Boolean, ignoreVerification: Boolean, deviceType: DeviceType): Boolean {
        Log.d(TAG, "startScan() deviceType: " + deviceType + ", urgent: " + java.lang.Boolean.toString(urgent)
                + " ignoreVerification: " + java.lang.Boolean.toString(ignoreVerification))

        return when (deviceType) {
            DeviceType.ELM327, DeviceType.OBD212, DeviceType.OBD215 -> {
                regularBluetoothDeviceSearcher.startScan(urgent, ignoreVerification)
            }

            DeviceType.RVD -> {
                rvdBluetoothDeviceSearcher.start()
            }
        }
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
            if (deviceInterface != null && deviceInterface is LowLevelDevice) {
                (deviceInterface as LowLevelDevice).setCommunicatorState(state)
            }
        }
    }

    fun getAvailableSubscriptions(): Boolean {
        return rvdBluetoothDeviceSearcher.getAvailableSubscriptions()
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
        val ret = deviceInterface?.getVin()
        Log.d(TAG, "get vin returned $ret")
    }

    fun getRtc() {
        Log.d(TAG, "getRtc()")
        if (deviceInterface != null || deviceInterface !is CastelDevice
                ||  btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else{
            (deviceInterface as CastelDevice).getRtc()
            true
        }
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
            (deviceInterface as Device215B).clearDeviceMemory()

        }
    }

    fun moreDevicesLeft(): Boolean {
        return regularBluetoothDeviceSearcher.moreDevicesLeft()
    }

    fun resetDeviceToDefaults() {
        Log.d(TAG, "resetToDefualts() ")
        if (deviceInterface is Device215B) {
            (deviceInterface as Device215B).resetDeviceToDefaults()
        }
    }

    fun resetDevice() {
        Log.d(TAG, "resetDevice() ")
        if (deviceInterface is Device215B) {
            (deviceInterface as Device215B).resetDevice()
        }

    }

    fun setRtc(rtcTime: Long): Boolean {
        Log.d(TAG, "setRtc() rtc: $rtcTime")
        return if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else if (deviceInterface != null && deviceInterface is CastelDevice){
            (deviceInterface as CastelDevice).setRtc(rtcTime)
            true
        }else{
            false
        }
    }

    fun getPids(pids: String): Boolean {
        Log.d(TAG, "getPids() pids: $pids")
        return if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else if (deviceInterface != null && deviceInterface is LowLevelDevice){
            (deviceInterface as LowLevelDevice).getPids(pids.split(","))
            true
        }else {
            false
        }
    }

    fun onGotPids(pidPackage: PidPackage){
        Log.d(TAG,"onGotPids() pidPackage: $pidPackage")
        dataListener?.pidData(pidPackage)
    }

    fun clearDtcs(): Boolean {
        Log.d(TAG, "clearDTCs")
        return if (deviceInterface != null
                && (deviceInterface is LowLevelDevice)) {
            (deviceInterface as LowLevelDevice).clearDtcs()
            true
        }else{
            false
        }
    }

    fun getSupportedPids(): Boolean {
        Logger.getInstance()!!.logI(TAG, "Requested supported pid", DebugMessage.TYPE_BLUETOOTH)
        return if (btConnectionState != BluetoothCommunicator.CONNECTED || deviceInterface == null) {
            false
        }else{
            deviceInterface!!.getSupportedPids()
            true
        }
    }

    // sets pids to check and sets data interval
    fun setPidsToSend(pids: String, timeInterval: Int): Boolean {
        Logger.getInstance()!!.logI(TAG, "Set pids to be sent: $pids, interval: $timeInterval", DebugMessage.TYPE_BLUETOOTH)
        return if (deviceInterface == null || btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else{
            deviceInterface!!.setPidsToSend(pids.split(","), timeInterval)
            true
        }
    }

    fun getDtcs(): Boolean {
        Log.d(TAG, "getDtcs()")
        return if (deviceInterface == null || btConnectionState != BluetoothCommunicator.CONNECTED) {
            Log.d(TAG, " can't get Dtcs because my sate is not connected")
            false
        }else{
            deviceInterface!!.getDtcs()
            deviceInterface!!.getPendingDtcs()
            true
        }
    }

    fun getFreezeFrame(): Boolean {
        Log.d(TAG, "getFreezeFrame()")
        return if (deviceInterface == null || deviceInterface !is CastelDevice
                || btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else {
            (deviceInterface as CastelDevice).getFreezeFrame()
            true
        }
    }

    fun requestData(): Boolean {
        Log.d(TAG, "requestData()")
        return if (deviceInterface == null || deviceInterface !is Device215B
                || btConnectionState != BluetoothCommunicator.CONNECTED ) {
            false
        }else{
            (deviceInterface as Device215B).requestData()
            true
        }

    }

    fun requestSnapshot(): Boolean {
        Log.d(TAG, "requestSnapshot()")
        if (deviceInterface != null) {
            if ((deviceInterface is Device215B || deviceInterface is ELM327Device) && btConnectionState == BluetoothCommunicator.CONNECTED) {
                Log.d(TAG, "executing writeToOBD requestSnapshot()")
                (deviceInterface as Device215B).requestSnapshot()
                return true
            }
        }
        return false
    }

    fun requestDescribeProtocol(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            (deviceInterface as ELM327Device).requestDescribeProtocol()
            true
        } else {
            false
        }
    }

    fun request2141PID(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            (deviceInterface as ELM327Device).request2141PID()
            true
        } else {
            false
        }
    }

    fun requestStoredDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            (deviceInterface as ELM327Device).requestStoredTroubleCodes()
            true
        } else {
            false
        }
    }

    fun requestPendingDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            (deviceInterface as ELM327Device).requestPendingTroubleCodes()
            true
        } else {
            false
        }
    }

    fun requestSelectProtocol(p: ObdProtocols): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            (deviceInterface as ELM327Device).requestSelectProtocol(p)
            true
        } else {
            false
        }
    }

    fun getState(): Int {
        return btConnectionState
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
