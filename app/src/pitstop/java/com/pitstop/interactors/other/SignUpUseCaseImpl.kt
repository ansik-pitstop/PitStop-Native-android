package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/15/2018.
 */
class SignUpUseCaseImpl(private val userRepository: UserRepository
                        , private val useCaseHandler: Handler
                        , private val mainHandler: Handler): SignUpUseCase {

    private val TAG = SignUpUseCaseImpl::class.java.simpleName

    private lateinit var callback: SignUpUseCase.Callback
    private lateinit var user: User
    private lateinit var compositeDisposable: CompositeDisposable

    override fun execute(user: User, callback: SignUpUseCase.Callback) {
        Logger.getInstance()!!.logI(TAG, "Use case execution started: user=$user"
                , DebugMessage.TYPE_USE_CASE)
        this.user = user
        this.callback = callback
        this.compositeDisposable = CompositeDisposable()
        useCaseHandler.post(this)
    }

    override fun run() {

        val disposable = userRepository.insert(user)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({next ->
                    Log.d(TAG,"insert user response: $next")
                    SignUpUseCaseImpl@onSuccess()
                }, {err ->
                    Log.d(TAG,"insert user error: $err")
                    SignUpUseCaseImpl@onError(RequestError(err))
                })
        compositeDisposable.add(disposable)

    }

    private fun onSuccess(){
        Logger.getInstance()!!.logI(TAG, "Use case finished: signed up successfully"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post { callback.onSuccess() }
    }

    private fun onError(err: RequestError){
        Logger.getInstance()!!.logI(TAG, "Use case returned error: err=$err"
                , DebugMessage.TYPE_USE_CASE)
        compositeDisposable.clear()
        mainHandler.post { callback.onError(err) }
    }
}