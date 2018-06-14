package com.pitstop.ui.login.signup

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
interface SignupView {
    fun getPassword(): String
    fun getConfirmPassword(): String
    fun getEmail(): String
    fun goToMainActivity()
    fun displayErrorDialog(message: String)
    fun displayToast(message: String)
    fun switchToMainActivity()
}