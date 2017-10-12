package com.pitstop.utils

import android.os.Handler
import android.util.Log
import com.pitstop.bluetooth.dataPackages.DtcPackage
import com.pitstop.interactors.add.AddDtcUseCase
import com.pitstop.interactors.add.AddDtcUseCaseImpl
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 10/12/2017.
 */
class PeriodicDtcSender(usecase: AddDtcUseCaseImpl) {

    companion object {
        private var INSTANCE: PeriodicDtcSender? = null
        fun getInstance(usecase: AddDtcUseCaseImpl): PeriodicDtcSender {
            if (INSTANCE == null){
                INSTANCE = PeriodicDtcSender(usecase)
            }
            return INSTANCE as PeriodicDtcSender
        }
    }

    private val TAG = javaClass.simpleName
    private val pendingDtcPackageList = ArrayList<DtcPackage>()
    private val periodicHandler = Handler()
    private val SEND_INTERVAL: Long = 30000
    private var posted = false

    private val periodicPendingDtcSender = object : Runnable {
        override fun run() {
            Log.d(TAG, "Sending dtc issue to server periodically.")
            pendingDtcPackageList.iterator().forEach { pendingDtcPackage ->
                usecase.execute(pendingDtcPackage, object : AddDtcUseCase.Callback {
                    override fun onDtcPackageAdded(dtc: DtcPackage) {
                        Log.d(TAG,"pending dtc added dtc: "+dtc)
                        pendingDtcPackageList.remove(dtc)
                    }

                    override fun onError(requestError: RequestError) {
                        Log.d(TAG,"error adding pending dtc err: "+requestError.message)
                    }

                })
            }
            periodicHandler!!.postDelayed(this, SEND_INTERVAL)
        }
    }

    fun addPendingDtcPackage(dtcPackage: DtcPackage){
        pendingDtcPackageList.add(dtcPackage)
        if (!posted){
            periodicHandler.post(periodicPendingDtcSender)
            posted = false
        }
    }

    fun hasPendingDtcPakcage(dtcPackage: DtcPackage): Boolean
            = pendingDtcPackageList.contains(dtcPackage)
}