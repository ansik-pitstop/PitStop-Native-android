package com.pitstop.interactors.other

import android.os.Bundle
import android.os.Handler
import android.util.Log
import com.facebook.AccessToken
import com.facebook.GraphRequest
import com.pitstop.database.LocalDatabaseHelper
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/19/2018.
 */
class FacebookSignUpUseCaseImpl(private val userRepository: UserRepository
                                , private val localDatabaseHelper: LocalDatabaseHelper
                                , private val useCaseHandler: Handler
                                ,private val mainHandler: Handler): FacebookSignUpUseCase {

    private val TAG = FacebookSignUpUseCase::class.java.simpleName

    private lateinit var callback: FacebookSignUpUseCase.Callback
    private val compositeDisposable = CompositeDisposable()

    override fun execute(callback: FacebookSignUpUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started"
                , DebugMessage.TYPE_USE_CASE)
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        localDatabaseHelper.deleteAllData()

        if (AccessToken.getCurrentAccessToken() == null){
            onError(RequestError.getUnknownError())
            return
        }
        val request = GraphRequest.newMeRequest(AccessToken.getCurrentAccessToken()) { responseObject, response ->
            try {
                // Application code
                Log.d(TAG,"response: $response, responseObject: $responseObject")
                val email = response?.jsonObject?.getString("email")
                val name = response?.jsonObject?.getString("name")
                val firstName = name?.split(" ")?.get(0)
                val lastName = name?.split(" ")?.get(1)

                val user = User()
                user.email = email
                user.userName = email
                user.firstName = firstName
                user.lastName = lastName

                val disposable = userRepository.insert(user,true)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({next ->
                            Log.d(TAG,"next: $next")
                            this@FacebookSignUpUseCaseImpl.onSuccess(next)
                        },{err ->
                            if (err is com.jakewharton.retrofit2.adapter.rxjava2.HttpException)
                            else Log.d(TAG,"err: ${err.message}")
                            this@FacebookSignUpUseCaseImpl.onError(RequestError(err))
                        })
                compositeDisposable.add(disposable)

                Log.d(TAG,"got facebook email: $email")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val parameters = Bundle()
        parameters.putString("fields","id,name,email,gender,birthday")
        request.parameters = parameters
        request.executeAsync()
    }

    private fun onSuccess(user: User){
        Logger.getInstance()!!.logI(TAG, "Use case finished: signed up successfully"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post({callback.onSuccess(user)})
    }

    private fun onError(error: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case returned error: err=$error"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(error)})
    }
}