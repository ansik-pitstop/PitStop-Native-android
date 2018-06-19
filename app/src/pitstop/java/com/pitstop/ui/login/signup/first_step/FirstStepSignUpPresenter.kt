package com.pitstop.ui.login.signup.first_step

import android.os.Bundle
import android.util.Log
import com.facebook.GraphRequest
import com.facebook.login.LoginResult
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.models.User

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class FirstStepSignUpPresenter(private val useCaseComponent: UseCaseComponent) {

    private val TAG = FirstStepSignUpPresenter::class.java.simpleName

    private var view: FirstStepSignUpView? = null

    fun subscribe(view: FirstStepSignUpView){
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
        } else if (email.length > 50){
            view!!.displayErrorDialog("Email is too long, must be less than 50 characters.")
        } else if (password.length > 50){
            view!!.displayErrorDialog("Password is too long, must be less than 50 characters.")
        } else if (password.length < 8){
            view!!.displayErrorDialog("Password is too short, please use at least 8 characters.")
        } else if (password != confirmPassword){
            view!!.displayErrorDialog("Passwords do not match.")
        }else{
            view!!.switchToNextStep(email,password)
        }
    }

    fun onFacebookLoginSuccess(loginResult: LoginResult?){
        Log.d(TAG,"onFacebookLoginSuccess()")
        val user = User()
        val request = GraphRequest.newMeRequest(loginResult!!.accessToken) { responseObject, response ->
            try {
                // Application code
                Log.d(TAG,"response: $response, responseObject: $responseObject")
                val email = response?.jsonObject?.getString("email")
                Log.d(TAG,"got facebook email: $email")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val parameters = Bundle()
        parameters.putString("fields","id,name,email,gender,birthday")
        request.parameters = parameters
        request.executeAsync()
    }

    fun onFacebookLoginCancel(){
        Log.d(TAG,"onFacebookLoginCancel()")

    }

    fun onFacebookLoginError(){
        Log.d(TAG,"onFacebookLoginError()")

    }

    fun onFacebookSignUpPressed(){
        Log.d(TAG,"onFacebookSignUpPressed()")
        view?.loginFacebook()
    }
}