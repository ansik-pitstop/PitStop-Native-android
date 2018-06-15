package com.pitstop.ui.login.signup.first_step

import android.util.Log
import com.pitstop.dependency.UseCaseComponent
import com.pitstop.interactors.other.SignUpUseCase
import com.pitstop.models.User
import com.pitstop.network.RequestError

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SecondStepSignUpPresenter(private val useCaseComponent: UseCaseComponent) {

    private val TAG = SecondStepSignUpPresenter::class.java.simpleName

    private var email: String? = null
    private var password: String? = null

    private var view: SecondStepSignUpView? = null

    fun subscribe(view: SecondStepSignUpView){
        this.view = view
    }

    fun unsubscribe(){
        view = null
    }

    fun setEmailAndPassword(email: String, password: String){
        Log.d(TAG,"setEmailAndPassword")
        this.email = email
        this.password = password
    }

    fun onSignupPressed(){
        Log.d(TAG,"onSignupPressed()")

        val firstName = view!!.getFirstName()
        val lastName = view!!.getLastName()
        val phoneNumber = view!!.getPhoneNumber()

        if (firstName.isEmpty() || lastName.isEmpty() || phoneNumber.isEmpty()){
            view!!.displayErrorDialog("Please provide all the fields.")
        }else if (!android.util.Patterns.PHONE.matcher(phoneNumber).matches()){
            view!!.displayErrorDialog("Invalid phone number.")
        }else{
            val user = User()
            user.firstName = firstName
            user.lastName = lastName
            user.phone = phoneNumber
            user.password = password
            user.email = email
            user.userName = email

            view?.displayLoading()

            useCaseComponent.signUpUseCase().execute(user, object: SignUpUseCase.Callback{
                override fun onSuccess() {
                    Log.d(TAG,"SignUpUseCase() returned success!")
                    view!!.displayToast("Sign up success!")
                    view!!.switchToMainActivity()
                    view?.hideLoading()
                }

                override fun onError(err: RequestError) {
                    Log.d(TAG,"SignUpUseCase() returned error! err=$err")
                    view?.hideLoading()
                    view!!.displayErrorDialog(err.message)
                }

            })

        }
    }
}