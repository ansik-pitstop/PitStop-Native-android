package com.pitstop.ui.login.login

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginPresenter {

    private var view: LoginView? = null

    fun subscribe(view: LoginView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }
}