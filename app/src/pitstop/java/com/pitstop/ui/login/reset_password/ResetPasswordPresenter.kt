package com.pitstop.ui.login.reset_password

import android.util.Log
import com.pitstop.R
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.other.ResetPasswordUseCase
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ResetPasswordPresenter(private val useCaseComponent: UseCaseComponent
                             , private val mixpanelHelper: MixpanelHelper) {

    private val TAG = ResetPasswordPresenter::class.java.simpleName

    private var view: ResetPasswordView? = null

    fun subscribe(view: ResetPasswordView){
        Log.d(TAG,"subscribe()")
        this.view = view
    }

    fun unsubscribe(){
        Log.d(TAG,"unsubscribe()")
        this.view = null
    }

    fun onPromptClosed(){
        Log.d(TAG,"onPromptClosed()")
        view?.switchToLogin()
    }

    fun onResetPasswordPressed(){
        Log.d(TAG,"onResetPasswordPressed()")
        val email = view!!.getEmail()
        if (email.isEmpty() ){
            view!!.displayErrorDialog(R.string.provide_all_fields)
        }else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            view!!.displayErrorDialog(R.string.email_invalid)
        } else if (email.length > 50){
            view!!.displayErrorDialog(R.string.email_too_long)
        }else{
            view?.displayLoading()
            useCaseComponent.resetPasswordUseCase().execute(email, object: ResetPasswordUseCase.Callback{
                override fun onSuccess() {
                    Log.d(TAG,"Reset password use case returned success")
                    view?.hideLoading()
                    view?.displaySuccessDialog(R.string.reset_password_instructions)
                    mixpanelHelper.trackLoginProcess(MixpanelHelper.STEP_LOGIN,MixpanelHelper.LOGIN_RESULT_RESET_PASSWORD)
                }

                override fun onError(err: RequestError) {
                    Log.d(TAG,"Reset password use case returned error")
                    view?.hideLoading()
                    view?.displayErrorDialog(err.message)
                }

            })
        }
    }


}