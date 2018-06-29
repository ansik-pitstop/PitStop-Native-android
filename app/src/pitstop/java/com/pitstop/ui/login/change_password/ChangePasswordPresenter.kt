package com.pitstop.ui.login.change_password

import android.util.Log
import com.pitstop.R
import com.pitstop.dependency.UseCaseComponent

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ChangePasswordPresenter(private val useCaseComponent: UseCaseComponent) {

    private val TAG = ChangePasswordPresenter::class.java.simpleName

    private var view: ChangePasswordView? = null

    fun subscribe(view: ChangePasswordView){
        Log.d(TAG,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(TAG,"unsubscribe()")
        this.view = null
    }

    fun onChangePasswordPressed(){
        Log.d(TAG,"onChangePasswordPressed()")

        val newPassword = view!!.getNewPassword()
        val newPasswordConfirmation = view!!.getNewPasswordConfirmation()
        val oldPassword = view!!.getOldPassword()


        if (newPassword.isEmpty() || newPasswordConfirmation.isEmpty()){
            view!!.showErrorDialog(R.string.provide_all_fields)
        } else if (newPassword == newPasswordConfirmation){
            view?.showErrorDialog(R.string.passwords_do_not_match)
        } else if (newPassword.length > 50){
            view!!.showErrorDialog(R.string.password_too_long)
        } else if (newPassword.length < 8){
            view!!.showErrorDialog(R.string.password_too_short)
        } else if (newPassword != newPasswordConfirmation){
            view!!.showErrorDialog(R.string.passwords_do_not_match)
        }else if (oldPassword == null || oldPassword.isEmpty()){
            view!!.showErrorDialog(R.string.unknown_error_contact_support)
        }
        else{
            //Launch use case

        }

    }
}