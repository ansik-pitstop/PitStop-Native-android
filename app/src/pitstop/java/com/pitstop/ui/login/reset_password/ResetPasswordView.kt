package com.pitstop.ui.login.reset_password

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
interface ResetPasswordView {
    fun getEmail(): String
    fun displayErrorDialog(message: Int)
    fun displayErrorDialog(message: String)
    fun displaySuccessDialog(message: Int)
    fun switchToLogin()
    fun displayLoading()
    fun hideLoading()
}