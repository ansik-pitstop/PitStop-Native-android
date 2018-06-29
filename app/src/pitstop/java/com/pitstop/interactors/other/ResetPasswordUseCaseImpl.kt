package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ResetPasswordUseCaseImpl(private val userRepository: UserRepository
                               , private val mainHandler: Handler
                               , private val useCaseHandler: Handler): ResetPasswordUseCase {

    private val tag = ResetPasswordUseCaseImpl::class.java.simpleName

    private lateinit var email: String
    private lateinit var callback: ResetPasswordUseCase.Callback

    override fun execute(email: String, callback: ResetPasswordUseCase.Callback) {
        Logger.getInstance().logI(tag, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE)
        this.email = email
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onSuccess(){
        Logger.getInstance().logI(tag, "Use case finished: success"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onSuccess()}
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(tag, "Use case returned error: error=$err"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onError(err)}
    }

    override fun run() {
        userRepository.resetPassword(email)
                .subscribeOn(Schedulers.computation())
                .observeOn(Schedulers.io())
                .subscribe({next ->
                    Log.d(tag,"reset password returned: $next")
                    this@ResetPasswordUseCaseImpl.onSuccess()
                }, {error->
                    this@ResetPasswordUseCaseImpl.onError(RequestError(error))
                })
    }
}