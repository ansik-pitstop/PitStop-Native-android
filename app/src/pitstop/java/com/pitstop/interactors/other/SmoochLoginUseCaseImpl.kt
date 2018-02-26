package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 2/26/2018.
 */
class SmoochLoginUseCaseImpl(val usecaseHandler: Handler
                             , val mainHandler: Handler): SmoochLoginUseCase {
    private val tag = javaClass.simpleName

    private lateinit var callback: SmoochLoginUseCase.Callback

    override fun execute(userId: String, callback: SmoochLoginUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started: userId=" + userId
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        usecaseHandler.post(this)

    }

    override fun run() {
        Log.d(tag,"run()")
    }

    private fun onError(err: String){
        Logger.getInstance()!!.logI(tag, "Use case returned error: err=" + err
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }

    private fun onLogin(){
        Logger.getInstance()!!.logI(tag, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onLogin()})
    }
}