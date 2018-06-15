package com.pitstop.ui.login.login

import android.util.Log
import com.pitstop.dependency.UseCaseComponent

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
        }else if (password.length < 8){
            view!!.displayError("Password too short, please use at least 8 characters.")
        }else{
            view!!.switchToMainActivity()
        }
    }

    fun onFacebookLoginPressed(){
        Log.d(TAG,"onFacebookLoginPressed()")


        view?.loginFacebook()
    }

    fun onFacebookLoginSuccess(){
        Log.d(TAG,"onFacebookLoginSuccess()")
        view?.switchToMainActivity()
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