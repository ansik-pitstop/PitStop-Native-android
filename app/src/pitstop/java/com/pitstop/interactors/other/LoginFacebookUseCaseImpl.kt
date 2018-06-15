package com.pitstop.interactors.other

import android.os.Handler
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import com.pitstop.utils.LoginManager
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
class LoginFacebookUseCaseImpl(private val loginManager: LoginManager
                               , private val userRepository: UserRepository
                               , private val useCaseHandler: Handler
                               , private val mainHandler: Handler): LoginFacebookUseCase {

    private val TAG = LoginFacebookUseCaseImpl::class.java.simpleName
    private lateinit var facebookAuthToken: String
    private lateinit var callback: LoginFacebookUseCase.Callback

    override fun execute(facebookAuthToken: String, callback: LoginFacebookUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.facebookAuthToken = facebookAuthToken
        useCaseHandler.post(this)
    }

    override fun run() {
       userRepository.loginFacebook(facebookAuthToken)
               .subscribeOn(Schedulers.computation())
               .observeOn(Schedulers.io())
               .subscribe({next ->
                   LoginFacebookUseCaseImpl@onSuccess()
               }, {error ->
                   LoginFacebookUseCaseImpl@onError(RequestError(error))
               })
    }

    private fun onSuccess(){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onSuccess()})
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in error=$err"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}