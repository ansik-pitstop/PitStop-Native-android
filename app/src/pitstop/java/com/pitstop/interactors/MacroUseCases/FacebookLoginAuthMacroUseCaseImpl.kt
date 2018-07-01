package com.pitstop.interactors.MacroUseCases

import android.os.Handler
import android.util.Log
import com.facebook.login.LoginManager
import com.pitstop.interactors.other.LoginFacebookUseCase
import com.pitstop.interactors.other.SmoochLoginUseCase
import com.pitstop.models.DebugMessage
import com.pitstop.network.RequestError
import com.pitstop.utils.Logger
import io.smooch.core.User

/**
 * Created by Karol Zdebel on 6/22/2018.
 */
class FacebookLoginAuthMacroUseCaseImpl(private val loginFacebookUseCase: LoginFacebookUseCase
                                        , private val smoochLoginUseCase: SmoochLoginUseCase
                                        , private val mainHandler: Handler): FacebookLoginAuthMacroUseCase {

    private val TAG = FacebookLoginAuthMacroUseCaseImpl::class.java.simpleName

    private lateinit var callback: FacebookLoginAuthMacroUseCase.Callback

    override fun execute(facebookAuthToken: String, smoochUser: User, callback: FacebookLoginAuthMacroUseCase.Callback) {
        this.callback = callback

        loginFacebookUseCase.execute(facebookAuthToken, object: LoginFacebookUseCase.Callback{
            override fun onSuccess(user: com.pitstop.models.User) {
                Log.d(TAG,"Facebook login use case returned success.")
                smoochLoginUseCase.execute(smoochUser, object: SmoochLoginUseCase.Callback{
                    override fun onError(err: RequestError) {
                        Log.d(TAG,"Smooch login use case returned error.")
                        this@FacebookLoginAuthMacroUseCaseImpl.onSuccess()
                    }

                    override fun onLogin() {
                        Log.d(TAG,"Smooch use case returned success.")
                        this@FacebookLoginAuthMacroUseCaseImpl.onSuccess()
                    }

                })
            }

            override fun onError(err: RequestError) {
                Log.d(TAG,"Facebook login use case returned error.")
                this@FacebookLoginAuthMacroUseCaseImpl.onError(err)
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
        LoginManager.getInstance().logOut()
        mainHandler.post{callback.onError(requestError)}
    }
}