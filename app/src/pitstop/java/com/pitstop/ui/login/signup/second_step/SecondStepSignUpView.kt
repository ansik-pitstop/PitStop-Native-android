package com.pitstop.ui.login.signup.first_step

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
interface SecondStepSignUpView {
    fun getFirstName(): String
    fun getLastName(): String
    fun getPhoneNumber(): String
    fun displayErrorDialog(message: String)
    fun displayToast(message: String)
    fun switchToMainActivity()
    fun displayLoading()
    fun hideLoading()
    fun setUsernameAndPassword(username: String, password: String)
}