package com.pitstop.interactors.get

import android.os.Handler
import com.pitstop.database.LocalAlarmStorage
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.utils.Logger

/**
 * Created by ishan on 2017-11-07.
 */
class GetAlarmCountUseCaseImpl (  val localAlarmStorage: LocalAlarmStorage,
                                val useCaseHandler: Handler, val mainHandler: Handler): GetAlarmCountUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmCountUseCase.Callback? = null
    private var carid:Int = 0

    override fun execute(carid:Int,  callback: GetAlarmCountUseCase.Callback) {
        Logger.getInstance().logE(TAG,"Use case execution started, input carId= "+carid
                ,false, DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.carid = carid
        useCaseHandler.post(this)
    }

    override fun run() {
        localAlarmStorage.getAlarmCount(this.carid, object:  Repository.Callback<Int>{
            override fun onSuccess(data: Int?) {
                Logger.getInstance().logE(TAG,"Use case finished: result="+data
                        ,false, DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onAlarmCountGot(data!!)})
            }
            override fun onError(error: RequestError?) {
                Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                        ,false, DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onError(error!!)})
            }
        })
    }

    }