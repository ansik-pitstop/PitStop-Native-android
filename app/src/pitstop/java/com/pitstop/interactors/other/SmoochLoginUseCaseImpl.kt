package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopSmoochApi
import com.pitstop.utils.Logger
import io.smooch.core.Smooch
import java.net.SocketTimeoutException

/**
 * Created by Karol Zdebel on 2/26/2018.
 */
class SmoochLoginUseCaseImpl(val smoochApi: PitstopSmoochApi, val usecaseHandler: Handler
                             , val mainHandler: Handler): SmoochLoginUseCase {
    private val tag = javaClass.simpleName

    private lateinit var callback: SmoochLoginUseCase.Callback
    private lateinit var userId: String
    private val again = 60000L

    override fun execute(userId: String, callback: SmoochLoginUseCase.Callback) {
        Logger.getInstance()!!.logI(tag, "Use case execution started: userId=" + userId
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.userId = userId
        usecaseHandler.post(this)

    }

    override fun run() {
        Log.d(tag,"run()")
        try{
            val call = smoochApi.getSmoochToken(userId.toInt()).execute()
            if (call.isSuccessful){
                Log.d(tag,"call successful")
                val body = call.body()
                if (body != null){
                    val smoochToken = body.get("smoochToken").asString
                    Log.d(tag,"smooch token: "+smoochToken)
                    if (smoochToken != null){
                        Smooch.login(userId,smoochToken, {
                            Log.d(tag,"login response err: "+it.error)
                            if (it.error == null) onLogin()
                            else onError(it.error)
                        })
                    }else{
                        Log.d(tag,"err smooch token null")
                        onError("err smooch token null")
                    }
                }else{
                    Log.d(tag,"err body null")
                    onError("err body null")
                }
            }else{
                Log.d(tag,"call unsuccessful")
                onError(call.message())
            }
        }catch(e: SocketTimeoutException){
            onError(RequestError.ERR_OFFLINE)
            Handler().postDelayed(this,again)
        }

    }

    private fun onError(err: String){
        Logger.getInstance()!!.logI(tag, "Use case returned error: err=$err, trying again in ${again/1000} seconds"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }

    private fun onLogin(){
        Logger.getInstance()!!.logI(tag, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onLogin()})
    }
}