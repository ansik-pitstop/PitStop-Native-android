package com.pitstop.interactors.check

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger
import com.pitstop.utils.NetworkHelper

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
class CheckNetworkConnectionUseCaseImpl(val networkHelper: NetworkHelper
                                        , val useCaseHandler: Handler, val mainHandler: Handler) : CheckNetworkConnectionUseCase {

    private var callback: CheckNetworkConnectionUseCase.Callback? = null
    private val tag = javaClass.simpleName

    override fun execute(callback: CheckNetworkConnectionUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started", DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        val isConnected = networkHelper.isConnected
        mainHandler.post({callback!!.onGotConnectionStatus(isConnected)})
        Logger.getInstance()!!.logI(tag, "Use case finished: isConnected="+isConnected
                , DebugMessage.TYPE_USE_CASE)
    }

}