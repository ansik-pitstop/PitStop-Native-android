package com.pitstop.ui.login.signup.first_step

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.MacroUseCases.SignUpAuthMacroUseCase
import com.pitstop.models.User
import com.pitstop.network.RequestError
import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SecondStepSignUpPresenter(private val useCaseComponent: UseCaseComponent
                                , private val mixpanelHelper: MixpanelHelper) {

    private val TAG = SecondStepSignUpPresenter::class.java.simpleName
    private var view: SecondStepSignUpView? = null

    fun subscribe(view: SecondStepSignUpView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }

    fun onSignupPressed() {
        Log.d(TAG, "onSignupPressed()")

        val firstName = view!!.getFirstName()
        val lastName = view!!.getLastName()
        val phoneNumber = view!!.getPhoneNumber()

        if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty()) {
            view!!.displayErrorDialog("Please provide all the fields.")
        } else if (!android.util.Patterns.PHONE.matcher(phoneNumber).matches()) {
            view!!.displayErrorDialog("Invalid phone number.")
        } else if (phoneNumber.length > 15){
            view!!.displayErrorDialog("Phone number is too long.")
        } else if (firstName.length > 30){
            view!!.displayErrorDialog("First name is too long")
        } else if (lastName.length > 30){
            view!!.displayErrorDialog("Last name is too long")
        } else{
            val user = User()
            user.firstName = firstName
            user.lastName = lastName
            user.phone = phoneNumber
            user.password = view?.getPassword()
            user.email = view?.getUsername()
            user.userName = view?.getUsername()

            view?.displayLoading()

            useCaseComponent.signUpAuthMacroUseCase().execute(user, io.smooch.core.User.getCurrentUser()
                    , object: SignUpAuthMacroUseCase.Callback{
                override fun onSuccess() {
                    Log.d(TAG,"SignUpUseCase() returned success!")
                    if (view == null) return
                    view!!.switchToOnBoarding()
                    view?.hideLoading()
                    mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_SIGNUP_NAME_AND_NUMBER, MixpanelHelper.SUCCESS)
                    mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_SIGNUP_SIGNED_UP, MixpanelHelper.SIGN_UP_RESULT_SUCCESS_REGULAR)
                }

                override fun onError(err: RequestError) {
                    Log.d(TAG,"SignUpUseCase() returned error! err=$err")
                    if (view == null) return
                    view?.hideLoading()
                    view!!.displayErrorDialog(err.message)
                }

            })

        }
    }
}