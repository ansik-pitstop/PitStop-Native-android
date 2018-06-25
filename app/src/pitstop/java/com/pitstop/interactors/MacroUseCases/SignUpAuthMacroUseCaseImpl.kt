package com.pitstop.interactors.MacroUseCases

import android.os.Handler
import android.util.Log
import com.pitstop.interactors.other.LoginUseCase
import com.pitstop.interactors.other.SignUpUseCase
import com.pitstop.interactors.other.SmoochLoginUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
class SignUpAuthMacroUseCaseImpl(private val signUpUseCase: SignUpUseCase
                                 , private val loginUseCase: LoginUseCase
                                 , private val smoochLoginUseCase: SmoochLoginUseCase
                                 , private val mainHandler: Handler)
    : SignUpAuthMacroUseCase {

    private val TAG = SignUpAuthMacroUseCaseImpl::class.java.simpleName

    private lateinit var callback: SignUpAuthMacroUseCase.Callback
    private lateinit var user: User
    private lateinit var smoochUser: io.smooch.core.User

    override fun execute(user: User, smoochUser: io.smooch.core.User, callback: SignUpAuthMacroUseCase.Callback) {
        this.callback = callback
        this.user = user
        this.smoochUser = smoochUser

        signUpUseCase.execute(user, object: SignUpUseCase.Callback{
            override fun onSignedUp() {
                Log.d(TAG,"Sign up use case returned success.")
                loginUseCase.execute(user.email, user.password, object: LoginUseCase.Callback{
                    override fun onSuccess() {
                        Log.d(TAG,"Login use case returned success.")
                        smoochLoginUseCase.execute(smoochUser, object: SmoochLoginUseCase.Callback{
                            override fun onError(err: RequestError) {
                                Log.d(TAG,"Smooch login use case returned error.")
                                this@SignUpAuthMacroUseCaseImpl.onError(err)
                            }

                            override fun onLogin() {
                                Log.d(TAG,"Smooch login use case returned success.")
                                this@SignUpAuthMacroUseCaseImpl.onSuccess()
                            }
                        })
                    }

                    override fun onError(error: RequestError) {
                        Log.d(TAG,"Login use case returned error.")
                        this@SignUpAuthMacroUseCaseImpl.onError(error)
                    }
                })
            }

            override fun onError(err: RequestError) {
                Log.d(TAG,"Sign up use case returned success.")
                this@SignUpAuthMacroUseCaseImpl.onError(err)
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