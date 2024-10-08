package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalDatabaseHelper
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import com.pitstop.utils.LoginManager
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
class LoginFacebookUseCaseImpl(private val loginManager: LoginManager
                               , private val userRepository: UserRepository
                               , private val localDatabaseHelper: LocalDatabaseHelper
                               , private val useCaseHandler: Handler
                               , private val mainHandler: Handler): LoginFacebookUseCase {

    private val TAG = LoginFacebookUseCaseImpl::class.java.simpleName
    private lateinit var facebookAuthToken: String
    private lateinit var callback: LoginFacebookUseCase.Callback
    private val compositeDisposable = CompositeDisposable()

    override fun execute(facebookAuthToken: String, callback: LoginFacebookUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started, facebookAuthToken: $facebookAuthToken"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        this.facebookAuthToken = facebookAuthToken
        useCaseHandler.post(this)
    }

    override fun run() {
        localDatabaseHelper.deleteAllData()
        val disposable = userRepository.loginFacebook(facebookAuthToken)
               .subscribeOn(Schedulers.computation())
               .observeOn(Schedulers.io())
               .subscribe({next ->
                   Log.d(TAG,"next: $next")
                   loginManager.loginUser(next.accessToken,next.refreshToken,next.user)
                   LoginFacebookUseCaseImpl@onSuccess(next.user)
               }, {error ->
                   Log.e(TAG,"loginFacebook err: $error")
                   LoginFacebookUseCaseImpl@onError(RequestError(error))
               })
        compositeDisposable.add(disposable)

    }

    private fun onSuccess(user: User){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback.onSuccess(user)})
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case finished: logged in error=$err"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback.onError(err)})
    }
}