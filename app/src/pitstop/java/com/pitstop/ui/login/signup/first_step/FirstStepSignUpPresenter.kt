package com.pitstop.ui.login.signup.first_step

import android.util.Log
import com.facebook.login.LoginResult
import com.pitstop.R
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.MacroUseCases.FacebookSignUpAuthMacroUseCase
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class FirstStepSignUpPresenter(private val useCaseComponent: UseCaseComponent
                               , private val mixpanelHelper: MixpanelHelper) {

    private val TAG = FirstStepSignUpPresenter::class.java.simpleName

    private var view: FirstStepSignUpView? = null

    fun subscribe(view: FirstStepSignUpView){
        this.view = view
        mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_SIGNUP_OPENED,MixpanelHelper.SUCCESS)
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
            view!!.displayErrorDialog(R.string.provide_all_fields)
        }else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()){
            view!!.displayErrorDialog(R.string.email_invalid)
        } else if (email.length > 50){
            view!!.displayErrorDialog(R.string.email_too_long)
        } else if (password.length > 50){
            view!!.displayErrorDialog(R.string.password_too_long)
        } else if (password.length < 8){
            view!!.displayErrorDialog(R.string.password_too_short)
        } else if (password != confirmPassword){
            view!!.displayErrorDialog(R.string.passwords_do_not_match)
        }else{
            view!!.switchToNextStep(email,password)
            mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_SIGNUP_EMAIL_AND_PASSWORD,MixpanelHelper.SUCCESS)
        }
    }

    fun onFacebookLoginSuccess(loginResult: LoginResult?){
        Log.d(TAG,"onFacebookLoginSuccess() token: ${loginResult!!.accessToken.token}")
        view?.displayLoading()
        useCaseComponent.facebookSignUpAuthMacroUseCase().execute(
                loginResult!!.accessToken.token
                ,io.smooch.core.User.getCurrentUser()
                ,object: FacebookSignUpAuthMacroUseCase.Callback{
            override fun onSuccess() {
                Log.d(TAG,"FacebookSignUpUseCase.onSuccess()")
                if (view == null) return
                view?.hideLoading()
                view?.switchToOnBoarding()
                mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_SIGNUP_SIGNED_UP
                        ,MixpanelHelper.SIGN_UP_RESULT_SUCCESS_FACEBOOK)
            }

            override fun onError(err: RequestError) {
                Log.d(TAG,"FacebookSignUpUseCase.onError() err:$err")
                if (view == null) return
                view?.hideLoading()
                view?.displayErrorDialog(err.message)
            }

        })
    }

    fun onFacebookLoginCancel(){
        Log.d(TAG,"onFacebookLoginCancel()")

    }

    fun onFacebookLoginError(){
        Log.d(TAG,"onFacebookLoginError()")
        view?.displayToast("Please connect to the internet")
    }

    fun onFacebookSignUpPressed(){
        Log.d(TAG,"onFacebookSignUpPressed()")
        view?.loginFacebook()
    }
}