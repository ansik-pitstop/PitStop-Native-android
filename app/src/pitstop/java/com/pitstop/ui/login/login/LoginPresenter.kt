package com.pitstop.ui.login.login

import android.util.Log
import com.facebook.login.LoginResult
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.MacroUseCases.FacebookLoginAuthMacroUseCase
import com.pitstop.interactors.MacroUseCases.LoginAuthMacroUseCase
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginPresenter(private val useCaseComponent: UseCaseComponent) {

    private val TAG = LoginPresenter::class.java.simpleName

    private var view: LoginView? = null

    fun subscribe(view: LoginView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }

    fun onLoginPressed(){
        val email = view!!.getEmail()
        val password = view!!.getPassword()

        if (email.isEmpty() || password.isEmpty()){
            view!!.displayError("Please provide all the fields.")
        }else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            view!!.displayError("Invalid email address.")
        }else if (email.length > 50){
            view!!.displayError("Email is too long, please use less than 50 characters")
        } else if (password.length < 8){
            view!!.displayError("Password too short, please use at least 8 characters.")
        } else if (password.length > 50){
            view!!.displayError("Password too long, must be less than 50 characters.")
        } else{
            view?.displayLoading()
            useCaseComponent.loginAuthMacroUseCase().execute(email,password
                    , io.smooch.core.User.getCurrentUser(), object: LoginAuthMacroUseCase.Callback{
                override fun onSuccess(activated: Boolean) {
                    Log.d(TAG,"LoginUseCase.onSuccess() activated: $activated")
                    if (view == null) return
                    if (activated){
                        view?.switchToMainActivity()
                    }else{
                        view?.switchToChangePassword(password)
                    }
                    view?.hideLoading()
                }

                override fun onError(error: RequestError) {
                    Log.d(TAG,"LoginUseCase.onError() err: $error")
                    if (view == null) return
                    view?.displayError(error.message)
                    view?.hideLoading()
                }

            })
        }
    }

    fun onForgotPasswordPressed(){
        Log.d(TAG,"onForgotPasswordPressed()")
        view?.switchToResetPassword()
    }

    fun onFacebookLoginPressed(){
        Log.d(TAG,"onFacebookLoginPressed()")

        view?.loginFacebook()
    }

    fun onFacebookLoginSuccess(result: LoginResult?){
        Log.d(TAG,"onFacebookLoginSuccess()")
        view?.displayLoading()
        useCaseComponent.facebookLoginAuthMacroUseCase().execute(
                result!!.accessToken.token
                , io.smooch.core.User.getCurrentUser()
                , object: FacebookLoginAuthMacroUseCase.Callback{

            override fun onSuccess() {
                Log.d(TAG,"FacebookLoginUseCase.onSuccess()")
                if (view == null) return
                view?.hideLoading()
                view?.switchToMainActivity()
            }

            override fun onError(err: RequestError) {
                Log.d(TAG,"FacebookLoginUseCase.onError() err: $err")
                if (view == null) return
                view?.hideLoading()
                view?.displayError(err.message)
            }

        })
    }

    fun onFacebookLoginCancel(){
        Log.d(TAG,"onFacebookLoginCancel()")
    }

    fun onFacebookLoginError(){
        Log.d(TAG,"onFacebookLoginError()")
    }

    fun onSignupPressed(){
        Log.d(TAG,"onSignupPressed()")
        view?.switchToSignUp()
    }
}