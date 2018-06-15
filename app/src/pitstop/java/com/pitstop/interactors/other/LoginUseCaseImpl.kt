package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import com.pitstop.utils.LoginManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
class LoginUseCaseImpl(private val userRepository: UserRepository
                       , private val loginManager: LoginManager
                       , private val useCaseHandler: Handler
                       , private val mainHandler: Handler): LoginUseCase {

    private val TAG = LoginUseCaseImpl::class.java.simpleName

    private lateinit var username: String
    private lateinit var password: String
    private lateinit var callback: LoginUseCase.Callback
    private lateinit var compositeDisposable: CompositeDisposable

    override fun execute(username: String, password: String, callback: LoginUseCase.Callback) {
        this.username = username
        this.password = password
        this.callback = callback
        compositeDisposable = CompositeDisposable()
        useCaseHandler.post(this)
    }

    override fun run() {

        val disposable = userRepository.login(username,password)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({next ->
                    Log.d(TAG,"login response: $next")
                    loginManager.loginUser(next.accessToken,next.refreshToken,next.user)
                    LoginUseCaseImpl@onSuccess()
                }, {err ->
                    Log.d(TAG,"login error response: $err")
                    LoginUseCaseImpl@onError(RequestError(err))

                })
        compositeDisposable.add(disposable)
    }

    private fun onSuccess(){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback.onSuccess()})
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in error=$err"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback.onError(err)})
    }
}