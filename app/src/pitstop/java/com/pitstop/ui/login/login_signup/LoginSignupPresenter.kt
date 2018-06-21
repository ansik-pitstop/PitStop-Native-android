package com.pitstop.ui.login.login_signup

import android.util.Log

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginSignupPresenter {

    private val TAG = LoginSignupPresenter::class.java.simpleName

    private var view: LoginSignupView? = null

    fun subscribe(view: LoginSignupView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }

    fun onSignupPressed(){
        Log.d(TAG,"onSignupPressed()")
        view?.switchToSignup()
    }

    fun onLoginPressed(){
        Log.d(TAG,"onLoginPressed()")
        view?.switchToLogin()
    }
}