package com.pitstop.interactors.MacroUseCases

import android.os.Handler
import com.pitstop.interactors.other.*
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import io.smooch.core.User

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
class FacebookSignUpAuthMacroUseCaseImpl(private val signupFacebookUseCase: FacebookSignUpUseCaseImpl
                                         , private val loginFacebookUseCase: LoginFacebookUseCaseImpl
                                         , private val smoochLoginUseCase: SmoochLoginUseCaseImpl
                                         , private val mainHandler: Handler)
    : FacebookSignUpAuthMacroUseCase {

    private val TAG = FacebookSignUpUseCaseImpl::class.java.simpleName
    private lateinit var callback: FacebookSignUpAuthMacroUseCase.Callback

    override fun execute(facebookAuthToken: String, smoochUser: User
                         , callback: FacebookSignUpAuthMacroUseCase.Callback) {
        this.callback = callback
        Logger.getInstance()!!.logI(TAG, "Macro use case execution started", DebugMessage.TYPE_USE_CASE)
        signupFacebookUseCase.execute(object: FacebookSignUpUseCase.Callback{
            override fun onSuccess() {
                loginFacebookUseCase.execute(facebookAuthToken
                        , object: LoginFacebookUseCase.Callback{
                    override fun onSuccess() {
                        smoochLoginUseCase.execute(smoochUser, object: SmoochLoginUseCase.Callback{
                            override fun onError(err: RequestError) {
                                this@FacebookSignUpAuthMacroUseCaseImpl.onError(err)
                            }

                            override fun onLogin() {
                                this@FacebookSignUpAuthMacroUseCaseImpl.onSuccess()
                            }

                        })
                    }

                    override fun onError(err: RequestError) {
                        this@FacebookSignUpAuthMacroUseCaseImpl.onError(err)
                    }
                })
            }

            override fun onError(err: RequestError) {
                this@FacebookSignUpAuthMacroUseCaseImpl.onError(err)
            }

        })
    }

    private fun onSuccess(){
        Logger.getInstance()!!.logI(TAG, "Use case finished: logged in successfully"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onSuccess()}
    }

    private fun onError(requestError: RequestError){
        Logger.getInstance()!!.logE(TAG, "Use case error: err = ${requestError.message}"
                , DebugMessage.TYPE_USE_CASE)
        mainHandler.post{callback.onError(requestError)}
    }
}