package com.pitstop.interactors.other

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.repositories.TripRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 3/28/2018.
 */
class EndTripUseCaseImpl(private val tripRepository: TripRepository
                         , private val usecaseHandler: Handler
                         , private val mainHandler: Handler): EndTripUseCase {

    private val tag = javaClass.simpleName
    private lateinit var callback: EndTripUseCase.Callback

    override fun execute(callback: EndTripUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)
    }

    override fun run() {
        tripRepository.completeTripData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.from(usecaseHandler.looper))
                .subscribe({
                    finished(it)
                },{
                    it.printStackTrace()
                    Logger.getInstance()!!.logE(tag, "Use case returned error: err=${it.message}"
                            , DebugMessage.TYPE_USE_CASE)
                })
    }

    private fun finished(rows: Int){
        Logger.getInstance()!!.logI(tag
                , "Use case finished: success", DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.finished()}
    }
}