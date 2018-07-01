package com.pitstop.interactors.other

import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import com.pitstop.utils.SmoochUtil
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ChangePasswordUseCaseImpl(private val userRepository: UserRepository
                                , private val mainHandler: Handler
                                , private val useCaseHandler: Handler): ChangePasswordUseCase {

    private val tag = ChangePasswordUseCaseImpl::class.java.simpleName

    private lateinit var oldPassword: String
    private lateinit var newPassword: String
    private var activateUser: Boolean = false
    private lateinit var callback: ChangePasswordUseCase.Callback

    override fun execute(oldPassword: String, newPassword: String, activateUser: Boolean
                         , callback: ChangePasswordUseCase.Callback) {
        Logger.getInstance().logI(tag, "Use case started execution"
                , DebugMessage.TYPE_USE_CASE)
        this.oldPassword = oldPassword
        this.newPassword = newPassword
        this.callback = callback
        this.activateUser = activateUser
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
        userRepository.getCurrentUser(object: Repository.Callback<User>{
            override fun onSuccess(data: User?) {
                userRepository.changeUserPassword(data!!.id, oldPassword, newPassword)
                        .subscribeOn(Schedulers.computation())
                        .observeOn(Schedulers.io())
                        .subscribe({next ->
                            Log.d(tag,"after changing password user: ${data.isActivated}")
                            if (activateUser){
                                SmoochUtil.sendSignedUpSmoochMessage(data!!.firstName ?: "", data!!.lastName ?: "")
                                userRepository.setUserActive(data!!.id)
                                        .subscribeOn(Schedulers.computation())
                                        .observeOn(Schedulers.io())
                                        .subscribe({next ->
                                            this@ChangePasswordUseCaseImpl.onSuccess()
                                        },{error ->
                                            this@ChangePasswordUseCaseImpl.onSuccess()
                                        })
                            }else{
                                this@ChangePasswordUseCaseImpl.onSuccess()
                            }
                        }, {error->
                            this@ChangePasswordUseCaseImpl.onError(RequestError(error))
                        })
            }

            override fun onError(error: RequestError) {
                this@ChangePasswordUseCaseImpl.onError(error)
            }

        })
    }
}