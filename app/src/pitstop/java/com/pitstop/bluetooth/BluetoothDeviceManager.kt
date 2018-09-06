package com.pitstop.bluetooth

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.util.Log
import com.continental.rvd.mobile_sdk.BindingQuestion
import com.continental.rvd.mobile_sdk.EBindingQuestionType
import com.continental.rvd.mobile_sdk.SDKIntentService
import com.continental.rvd.mobile_sdk.internal.api.binding.model.Error
import com.pitstop.application.GlobalApplication
import com.pitstop.bluetooth.bleDevice.*
import com.pitstop.bluetooth.communicator.BluetoothCommunicator
import com.pitstop.bluetooth.communicator.IBluetoothCommunicator
import com.pitstop.bluetooth.communicator.ObdManager
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.bluetooth.dataPackages.ParameterPackage
import com.pitstop.bluetooth.dataPackages.PidPackage
import com.pitstop.bluetooth.elm.enums.ObdProtocols
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcher
import com.pitstop.bluetooth.searcher.RVDBluetoothDeviceSearcherStatusListener
import com.pitstop.bluetooth.searcher.RegularBluetoothDeviceSearcher
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger

/**
 *
 * Created by Ben!
 */
class BluetoothDeviceManager(private val mContext: Context
                             , sdkIntentService: SDKIntentService
                             , private val dataListener: ObdManager.IBluetoothDataListener)
    : RVDBluetoothDeviceSearcherStatusListener {

    private val TAG = BluetoothDeviceManager::class.java.simpleName

    private val application: GlobalApplication = mContext.applicationContext as GlobalApplication
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

        regularBluetoothDeviceSearcher = RegularBluetoothDeviceSearcher(useCaseComponent
                , dataListener, mContext, this)
        rvdBluetoothDeviceSearcher = RVDBluetoothDeviceSearcher(sdkIntentService, this,this)
    }

    fun cancelBinding(){
        if (deviceInterface is RVDDevice){
            deviceInterface.cancelBinding()
        }
    }

    /*
     * Callback methods for RVD SDK device searcher interface
     */
    override fun onBindingRequired() {
        Log.d(TAG, "onBindingRequired()")
        dataListener?.onBindingRequired()
    }

    override fun onBindingQuestionPrompted(question: BindingQuestion) {
        Log.d(TAG, "onBindingQuestionPrompted() question: $question")
        dataListener?.onBindingQuestionPrompted(question)
    }

    override fun onFirmwareInstallationRequired() {
        Log.d(TAG, "onFirmwareInstallationRequired()")
        dataListener?.onFirmwareInstallationRequired()
    }

    override fun onFirmwareInstallationProgress(progress: Float) {
        Log.d(TAG, "onFirmwareInstallationProgress() progress:$progress")
        dataListener?.onFirmwareInstallationProgress(progress)
    }

    override fun onConnectionFailure(err: Error) {
        Log.d(TAG,"onConnectionFailure() err: $err")
    }

    override fun onConnectionCompleted() {
        Log.d(TAG,"onConnectionCompleted()")
    }

    override fun onBindingProgress(progress: Float) {
        Log.d(TAG,"onBindingProgress() progress: $progress")
        dataListener?.onBindingProgress(progress)
    }

    override fun onBindingFinished() {
        Log.d(TAG,"onBindingFinished()")
        dataListener?.onBindingFinished()
    }

    override fun onBindingError(err: Error) {
        Log.d(TAG,"onBindingError() err: $err")
        dataListener?.onBindingError(err)
    }

    override fun onFirmwareInstallationFinished() {
        Log.d(TAG,"onFirmwareInstallationFinished()")
        dataListener?.onFirmwareInstallationFinished()
    }

    override fun onFirmwareInstallationError(err: Error) {
        Log.d(TAG,"onFirmwareInstallationError() err: $err")
        dataListener?.onFirmwareInstallationError(err)
    }

    override fun onCompleted(device: AbstractDevice) {
        Log.d(TAG,"onCompleted()")
    }

    /*
     * End of callback methods for RVD SDK
     */

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
        dataListener!!.getBluetoothState(state)

    }

    fun answerBindingQuestion(questionType: EBindingQuestionType, answer: String): Boolean{
        return if (deviceInterface != null && deviceInterface is RVDDevice){
            deviceInterface.answerBindingQuestion(questionType,answer)
            true
        }else{
            false
        }
    }

    fun onGotVin(VIN: String, deviceID: String) {
        Log.d(TAG, "onGotVin: $VIN")
        dataListener!!.handleVinData(VIN, deviceID)
    }

    fun onGotDTC(dtcPackage: DtcPackage){
        Log.d(TAG,"onGotDTC() dtcPackage: $dtcPackage")
        dataListener?.dtcData(dtcPackage)
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

    @Synchronized
    fun startScan(urgent: Boolean, ignoreVerification: Boolean, deviceType: DeviceType): Boolean {
        Log.d(TAG, "startScan() deviceType: " +deviceType+ ", urgent: " + java.lang.Boolean.toString(urgent)
                + " ignoreVerification: " + java.lang.Boolean.toString(ignoreVerification))
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
            if (deviceInterface != null && deviceInterface is LowLevelDevice) {
                deviceInterface.setCommunicatorState(state)
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
        val ret = deviceInterface!!.getVin()
        Log.d(TAG, "get vin returned $ret")
    }

    fun getRtc() {
        Log.d(TAG, "getRtc()")
        if (deviceInterface != null || deviceInterface !is CastelDevice
                ||  btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else{
            deviceInterface.getRtc()
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
            deviceInterface.clearDeviceMemory()

        }
    }

    fun moreDevicesLeft(): Boolean {
        return regularBluetoothDeviceSearcher.moreDevicesLeft()
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

    fun setRtc(rtcTime: Long): Boolean {
        Log.d(TAG, "setRtc() rtc: $rtcTime")
        return if (btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else if (deviceInterface != null && deviceInterface is CastelDevice){
            deviceInterface.setRtc(rtcTime)
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
            deviceInterface.getPids(pids.split(","))
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
            deviceInterface.clearDtcs()
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
            deviceInterface.getSupportedPids()
            true
        }
    }

    // sets pids to check and sets data interval
    fun setPidsToSend(pids: String, timeInterval: Int): Boolean {
        Logger.getInstance()!!.logI(TAG, "Set pids to be sent: $pids, interval: $timeInterval", DebugMessage.TYPE_BLUETOOTH)
        return if (deviceInterface == null || btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else{
            deviceInterface.setPidsToSend(pids.split(","), timeInterval)
            true
        }
    }

    fun getDtcs(): Boolean {
        Log.d(TAG, "getDtcs()")
        return if (deviceInterface == null || btConnectionState != BluetoothCommunicator.CONNECTED) {
            Log.d(TAG, " can't get Dtcs because my sate is not connected")
            false
        }else{
            deviceInterface.getDtcs()
            deviceInterface.getPendingDtcs()
            true
        }
    }

    fun getFreezeFrame(): Boolean {
        Log.d(TAG, "getFreezeFrame()")
        return if (deviceInterface == null || deviceInterface !is CastelDevice || btConnectionState != BluetoothCommunicator.CONNECTED) {
            false
        }else {
            deviceInterface.getFreezeFrame()
            true
        }
    }

    fun requestData(): Boolean {
        Log.d(TAG, "requestData()")
        return if (deviceInterface == null || deviceInterface !is Device215B || btConnectionState != BluetoothCommunicator.CONNECTED ) {
            false
        }else{
            deviceInterface.requestData()
            true
        }

    }

    fun requestSnapshot(): Boolean {
        Log.d(TAG, "requestSnapshot()")
        if (deviceInterface != null) {
            if ((deviceInterface is Device215B || deviceInterface is ELM327Device) && btConnectionState == BluetoothCommunicator.CONNECTED) {
                Log.d(TAG, "executing writeToOBD requestSnapshot()")
                deviceInterface.requestSnapshot()
                return true
            }
        }
        return false
    }

    fun requestDescribeProtocol(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestDescribeProtocol()
            true
        } else {
            false
        }
    }

    fun request2141PID(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.request2141PID()
            true
        } else {
            false
        }
    }

    fun requestStoredDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestStoredTroubleCodes()
            true
        } else {
            false
        }
    }

    fun requestPendingDTC(): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestPendingTroubleCodes()
            true
        } else {
            false
        }
    }

    fun requestSelectProtocol(p: ObdProtocols): Boolean {
        Log.d(TAG, "requestDescribeProtocol")
        return if (deviceInterface != null && deviceInterface is ELM327Device) {
            deviceInterface.requestSelectProtocol(p)
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
