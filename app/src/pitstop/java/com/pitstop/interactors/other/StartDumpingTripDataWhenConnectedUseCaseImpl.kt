package com.pitstop.interactors.other

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.repositories.TripRepository
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 3/21/2018.
 */
class StartDumpingTripDataWhenConnectedUseCaseImpl(private val tripRepository: TripRepository
                                                   , private val usecaseHandler: Handler
                                                   , private val mainHandler: Handler)
    : StartDumpingTripDataWhenConnecteUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var callback: StartDumpingTripDataWhenConnecteUseCase.Callback

    override fun execute(callback: StartDumpingTripDataWhenConnecteUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case started execution", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        tripRepository.dumpDataOnConnectedToNetwork()
        Logger.getInstance()!!.logI(TAG, "Use case execution finished: started dumping data"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post {
            callback.started()
        }
    }
}