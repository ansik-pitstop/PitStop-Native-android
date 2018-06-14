package com.pitstop.ui.login.login_signup

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginSignupPresenter {

    private var view: LoginSignupView? = null

    fun subscribe(view: LoginSignupView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }
}