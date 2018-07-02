package com.pitstop.ui.login.change_password

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
interface ChangePasswordView {
    fun getNewPassword(): String
    fun getNewPasswordConfirmation(): String
    fun getOldPassword(): String?
    fun showErrorDialog(err: Int)
    fun showErrorDialog(err: String)
    fun switchToOnboarding()
    fun setUserWasInactiveFlag()
    fun showLoading()
    fun hideLoading()
}