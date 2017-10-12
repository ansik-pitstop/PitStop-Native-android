package com.pitstop.bluetooth.handler

import android.os.Handler
import android.util.Log
import com.pitstop.EventBus.*
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.add.AddDtcUseCase
import com.pitstop.network.RequestError
import org.greenrobot.eventbus.EventBus
import java.util.*

/**
 * Created by Karol Zdebel on 8/15/2017.
 */

class DtcDataHandler(private val bluetoothDataHandlerManager: BluetoothDataHandlerManager
                     , private val useCaseComponent: UseCaseComponent) {

    private val eventSource = EventSourceImpl(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT)
    private val pendingDtcPackages = ArrayList<DtcPackage>()
    private val tag = javaClass.simpleName
    private val dtcToSend = mutableListOf<DtcPackage>() //dtc from offline error that havent been sent
    private val periodicHandler = Handler()
    private val sendInterval: Long = 30000
    private var isSending = false

    private val periodicPendingDtcSender = object : Runnable {
        override fun run() {
            Log.d(tag, "Sending dtc issue to server periodically.")
            sendLocalDtc()
            periodicHandler.postDelayed(this, sendInterval)
        }
    }

    init{
        periodicHandler.post(periodicPendingDtcSender)
        isSending = false
    }

    fun sendLocalDtc(){
        Log.d(tag, "sendLocalDtc() localDtc: $dtcToSend, isSending? $isSending")
        if (isSending || dtcToSend.isEmpty()) return
        isSending = true
        val dtcListCopy = ArrayList(dtcToSend)
        for (dtc in dtcListCopy){
            useCaseComponent.addDtcUseCase().execute(dtc, object : AddDtcUseCase.Callback {
                override fun onDtcPackageAdded(addedDtc: DtcPackage) {
                    Log.d(tag,"pending addedDtc added addedDtc: "+ addedDtc)
                    this@DtcDataHandler.dtcToSend.remove(addedDtc)
                    isSending = false
                }

                override fun onError(requestError: RequestError) {
                    Log.d(tag,"error adding pending dtc err: "+requestError.message)
                    if (requestError.error != RequestError.ERR_OFFLINE)
                        this@DtcDataHandler.dtcToSend.clear()
                    isSending = false
                }

            })
        }
    }

    fun handleDtcData(dtcPackage: DtcPackage) {
        Log.d(tag, "handleDtcData() dtcPackage: " + dtcPackage)
        val deviceId = dtcPackage.deviceId

        pendingDtcPackages.add(dtcPackage)
        if (!bluetoothDataHandlerManager.isDeviceVerified || deviceId.isEmpty()) {
            Log.d(tag, "Dtc data added to pending list, device not verified!")
            return
        }

        for (p in pendingDtcPackages) {

            if (dtcPackage.dtcs.isNotEmpty()) {
                useCaseComponent.addDtcUseCase().execute(dtcPackage, object : AddDtcUseCase.Callback {
                    override fun onDtcPackageAdded(dtc: DtcPackage) {
                        notifyEventBus(EventTypeImpl(
                                EventType.EVENT_DTC_NEW))
                    }

                    override fun onError(requestError: RequestError) {
                        if (requestError.error == RequestError.ERR_OFFLINE
                                && !dtcToSend.contains(dtcPackage)){
                            dtcToSend.add(dtcPackage)
                        }
                    }
                })
                bluetoothDataHandlerManager.requestFreezeData()
            }
        }
        pendingDtcPackages.clear()
    }

    private fun notifyEventBus(eventType: EventType) {
        val carDataChangedEvent = CarDataChangedEvent(eventType, eventSource)
        EventBus.getDefault().post(carDataChangedEvent)
    }

    fun clearPendingData() {
        pendingDtcPackages.clear()
    }
}
