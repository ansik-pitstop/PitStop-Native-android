package com.pitstop.interactors.check

import android.os.Handler
import com.pitstop.utils.NetworkHelper

/**
 * Created by Karol Zdebel on 10/16/2017.
 */
class CheckNetworkConnectionUseCaseImpl(val networkHelper: NetworkHelper
                                        , val useCaseHandler: Handler, val mainHandler: Handler) : CheckNetworkConnectionUseCase {

    private var callback: CheckNetworkConnectionUseCase.Callback? = null


    override fun execute(callback: CheckNetworkConnectionUseCase.Callback) {
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        mainHandler.post({callback!!.onGotConnectionStatus(networkHelper.isConnected)})
    }

}