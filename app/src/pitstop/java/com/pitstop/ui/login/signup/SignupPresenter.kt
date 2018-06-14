package com.pitstop.ui.login.signup

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SignupPresenter {

    private var view: SignupView? = null

    fun subscribe(view: SignupView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }
}