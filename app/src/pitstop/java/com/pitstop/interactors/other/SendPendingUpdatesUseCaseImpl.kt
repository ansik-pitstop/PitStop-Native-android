package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.PendingUpdate
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.utils.Logger
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 5/28/2018.
 */
class SendPendingUpdatesUseCaseImpl(private val carRepository: CarRepository
                                    , private val usecaseHandler: Handler
                                    , private val mainHandler: Handler): SendPendingUpdatesUseCase {

    private val tag = SendPendingUpdatesUseCaseImpl::class.java.simpleName

    private lateinit var callback: SendPendingUpdatesUseCase.Callback

    override fun execute(callback: SendPendingUpdatesUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        carRepository.sendPendingUpdates()
                .observeOn(Schedulers.io(),true)
                .subscribeOn(Schedulers.computation())
                .subscribe({
                    Log.d(tag,"sendPendingUpdates() response: $it")
                    updatesSent(it)
                },{
                    Log.e(tag,"error sending pending updates: $it")
                    errorSending(it)
                })
    }

    private fun updatesSent(pendingUpdates: List<PendingUpdate>){
        Logger.getInstance()!!.logI(tag, "Use case finished: updates sent: $pendingUpdates"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.updatesSent(pendingUpdates)})
    }

    private fun errorSending(err: Throwable){
        Logger.getInstance()!!.logI(tag, "Use case returned error: err=$err"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.errorSending(RequestError(err))})
    }
}