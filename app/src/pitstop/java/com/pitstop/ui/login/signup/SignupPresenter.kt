package com.pitstop.ui.login.signup

import android.util.Log

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SignupPresenter {

    private val TAG = SignupPresenter::class.java.simpleName

    private var view: SignupView? = null

    fun subscribe(view: SignupView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }

    fun onSignupPressed(){
        Log.d(TAG,"onSignupPressed()")

        //Check email
        val email = view!!.getEmail()
        val password = view!!.getPassword()
        val confirmPassword = view!!.getConfirmPassword()

        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()){
            view!!.displayErrorDialog("Please provide all the fields.")
        }else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            view!!.displayErrorDialog("Invalid email address.")
        }else if (password.length < 8){
            view!!.displayErrorDialog("Password too short, please use at least 8 characters.")
        } else if (password != confirmPassword){
            view!!.displayErrorDialog("Passwords do not match.")
        }else{
            view!!.displayToast("Sign up success!")
            view!!.switchToMainActivity()
        }
    }
}