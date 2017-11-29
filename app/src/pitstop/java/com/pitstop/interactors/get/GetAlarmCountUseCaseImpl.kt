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
class GetAlarmCountUseCaseImpl (  val localAlarmStorage: LocalAlarmStorage,
                                val useCaseHandler: Handler, val userRepository: UserRepository
                                  , val mainHandler: Handler): GetAlarmCountUseCase {

    private val TAG = javaClass.simpleName;
    private var callback: GetAlarmCountUseCase.Callback? = null

    override fun execute(callback: GetAlarmCountUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }



    override fun run() {
        userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
            override fun onSuccess(data: Settings?) {
                localAlarmStorage.getAlarmCount(data!!.carId, object: Repository.Callback<Int>{
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

            override fun onError(error: RequestError?) {
                Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                        , DebugMessage.TYPE_USE_CASE)
                mainHandler.post({callback?.onError(error!!)})
            }

        })

    }

    }