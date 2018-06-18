package com.pitstop.ui.login.signup.first_step

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.facebook.CallbackManager
import com.facebook.FacebookCallback
import com.facebook.FacebookException
import com.facebook.login.LoginManager
import com.facebook.login.LoginResult
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_signup_step_one.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class FirstStepSignUpFragment: Fragment() , FirstStepSignUpView {

    private val TAG = FirstStepSignUpFragment::class.java.simpleName

    private var presenter: FirstStepSignUpPresenter? = null
    private var facebookCallbackManager: CallbackManager? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_signup_step_one,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        facebookCallbackManager = CallbackManager.Factory.create()
        LoginManager.getInstance().registerCallback(facebookCallbackManager
                , object: FacebookCallback<LoginResult> {

            override fun onSuccess(result: LoginResult?) {
                Log.d(TAG,"onSuccess() result: $result")
                presenter?.onFacebookLoginSuccess(result)
            }

            override fun onCancel() {
                Log.d(TAG,"onCancel()")
                presenter?.onFacebookLoginCancel()
            }

            override fun onError(error: FacebookException?) {
                Log.d(TAG,"onError() err: $error")
                presenter?.onFacebookLoginError()
            }

        })

        signup_button.setOnClickListener {
            presenter?.onSignupPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context))
                    .build()
            presenter = FirstStepSignUpPresenter(useCaseComponent)
        }
        presenter?.subscribe(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.unsubscribe()
    }

    override fun getPassword(): String {
        Log.d(TAG,"getPassword()")
        return password_field.text.toString()

    }

    override fun getConfirmPassword(): String {
        Log.d(TAG,"getConfirmPassword()")
        return password_confirm_field.text.toString()
    }

    override fun getEmail(): String {
        Log.d(TAG,"getEmail()")
        return email_field.text.toString()
    }

    override fun displayErrorDialog(message: String) {
        Log.d(TAG,"displayErrorDialog() message: $message")
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(message)
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun displayToast(message: String) {
        Log.d(TAG,"displayToast() message: $message")
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }

    override fun switchToNextStep(username: String, password: String) {
        Log.d(TAG,"switchToNextStep() u:$username")
        try{
            (activity as LoginActivity).switchToSignupStepTwo(username, password)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun loginFacebook() {
        Log.d(TAG,"loginFacebook()")
        LoginManager.getInstance().logInWithReadPermissions(this
                , arrayListOf("public_profile,email"))
    }
}