package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger

/**
 * Created by ishan on 2017-11-07.
 */
class GetAlarmCountUseCaseImpl (  val localAlarmStorage: LocalAlarmStorage,val userRepository: UserRepository
                                , val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmCountUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmCountUseCase.Callback? = null
    private var carId: Int = 0

    override fun execute(carId: Int, callback: GetAlarmCountUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carId = carId
        useCaseHandler.post(this)
    }

    override fun run() {
        localAlarmStorage.getAlarmCount(this.carId, object: Repository.Callback<Int>{
            override fun onSuccess(data: Int?) {
                Logger.getInstance().logI(TAG, "Use case finished: result="+data
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onAlarmCountGot(data!!)})
            }
            override fun onError(error: RequestError?) {
                Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onError(error!!)})
            }
        })
    }

    }