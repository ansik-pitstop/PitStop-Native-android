package com.pitstop.ui.login.signup.first_step

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
interface FirstStepSignUpView {
    fun getPassword(): String
    fun getConfirmPassword(): String
    fun getEmail(): String
    fun displayErrorDialog(message: String)
    fun displayErrorDialog(error: Int)
    fun displayToast(message: String)
    fun switchToNextStep(username: String, password: String)
    fun switchToOnBoarding()
    fun loginFacebook()
    fun displayLoading()
    fun hideLoading()
}