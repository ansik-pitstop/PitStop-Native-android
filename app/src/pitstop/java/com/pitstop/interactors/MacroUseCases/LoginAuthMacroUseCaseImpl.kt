package com.pitstop.interactors.MacroUseCases

import android.os.Handler
import com.pitstop.interactors.other.LoginUseCase
import com.pitstop.interactors.other.SmoochLoginUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
class LoginAuthMacroUseCaseImpl(private val loginUseCase: LoginUseCase
                                , private val smoochLoginUseCase: SmoochLoginUseCase
                                , private val mainHandler: Handler): LoginAuthMacroUseCase {

    private val TAG = LoginAuthMacroUseCaseImpl::class.java.simpleName
    private lateinit var callback: LoginAuthMacroUseCase.Callback

    override fun execute(username: String, password: String, smoochUser: io.smooch.core.User
                         , callback: LoginAuthMacroUseCase.Callback) {
        this.callback = callback
        loginUseCase.execute(username, password, object: LoginUseCase.Callback{
            override fun onSuccess(user: User, activated: Boolean) {
                smoochLoginUseCase.execute(smoochUser, object: SmoochLoginUseCase.Callback{
                    override fun onError(err: RequestError) {
                        this@LoginAuthMacroUseCaseImpl.onError(err)
                    }

                    override fun onLogin() {
                        this@LoginAuthMacroUseCaseImpl.onSuccess(activated)
                    }

                })
            }

            override fun onError(error: RequestError) {
                this@LoginAuthMacroUseCaseImpl.onError(error)
            }

        })
    }

    private fun onSuccess(activated: Boolean){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onSuccess(activated)}
    }

    private fun onError(requestError: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case error: err = ${requestError.message}"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onError(requestError)}
    }
}