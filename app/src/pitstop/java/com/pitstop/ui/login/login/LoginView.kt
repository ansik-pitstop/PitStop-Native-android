package com.pitstop.ui.login.login

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
interface LoginView {
    fun getEmail(): String
    fun getPassword(): String
    fun displayToast(message: String)
    fun displayError(message: String)
    fun switchToMainActivity()
    fun switchToChangePassword(oldPassword: String)
    fun switchToResetPassword()
    fun switchToSignUp()
    fun loginFacebook()
    fun displayLoading()
    fun hideLoading()
}