package com.pitstop.interactors.other

import android.annotation.SuppressLint
import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.schedulers.Schedulers

class SendFleetManagerSmsUseCaseImpl(private val userRepository: UserRepository,
                                     private val mainHandler: Handler,
                                     private val useCaseHandler: Handler): SendFleetManagerSmsUseCase {

    private val tag = ResetPasswordUseCaseImpl::class.java.simpleName

    private lateinit var text: String
    private lateinit var callback: SendFleetManagerSmsUseCase.Callback

    override fun execute(text: String, callback: SendFleetManagerSmsUseCase.Callback) {
        Logger.getInstance().logI(tag,
                "Use case started execution",
                DebugMessage.TYPE_USE_CASE)
        this.text = text
        this.callback = callback
        useCaseHandler.post(this)
    }

    private fun onSuccess(){
        Logger.getInstance().logI(tag, "Sms sent with success"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onSuccess()}
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(tag, "Use case returned error: error=$err"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onError(err)}
    }

    @SuppressLint("CheckResult")
    override fun run() {
        userRepository.getCurrentUser(object : Repository.Callback<User> {
            override fun onSuccess(data: User?) {
                userRepository.sendFleetManagerSms(data!!.id, text)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({next ->
                            this@SendFleetManagerSmsUseCaseImpl.onSuccess()
                        }, {error->
                            this@SendFleetManagerSmsUseCaseImpl.onError(RequestError(error))
                        })
            }

            override fun onError(error: RequestError?) {
                this@SendFleetManagerSmsUseCaseImpl.onError(error!!)
            }
        })

    }
}