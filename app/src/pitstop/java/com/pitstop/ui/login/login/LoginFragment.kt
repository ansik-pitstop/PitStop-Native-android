package com.pitstop.ui.login.login

import android.content.Intent
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
import kotlinx.android.synthetic.main.layout_signin.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginFragment: Fragment(), LoginView {

    private val TAG = LoginFragment::class.java.simpleName

    private var presenter: LoginPresenter? = null
    private var facebookCallbackManager: CallbackManager? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_signin,container,false)

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

            override fun onError(error: FacebookException) {
                Log.d(TAG,"onError() err: $error")
                presenter?.onFacebookLoginError(error)
            }

        })

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        login_button.setOnClickListener { presenter?.onLoginPressed() }
        facebook_signin_button.setOnClickListener { presenter?.onFacebookLoginPressed() }
        signup_text.setOnClickListener { presenter?.onSignupPressed() }
        forgot_password_text.setOnClickListener { presenter?.onForgotPasswordPressed() }
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(activity?.applicationContext))
                    .build()
            presenter = LoginPresenter(useCaseComponent)
        }
        presenter?.subscribe(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.unsubscribe()
    }

    override fun getEmail(): String {
        Log.d(TAG,"getEmail()")
        return email_field.text.toString()
    }

    override fun getPassword(): String {
        Log.d(TAG,"getEmail()")
        return password_field.text.toString()
    }

    override fun displayToast(message: String) {
        Log.d(TAG,"getEmail()")
        Toast.makeText(context,message,Toast.LENGTH_SHORT).show()
    }

    override fun displayError(message: String) {
        Log.d(TAG,"getEmail()")
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(message)
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun switchToMainActivity() {
        Log.d(TAG,"switchToMainActivity()")
        try{
            (activity as LoginActivity).switchToMainActivity(false)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun switchToSignUp() {
        Log.d(TAG,"switchToSignUp()")
        try{
            (activity as LoginActivity).switchToSignupStepOne()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun switchToChangePassword(oldPassword: String) {
        Log.d(TAG,"switchToChangePassword()")
        try{
            (activity as LoginActivity).switchToChangePassword(oldPassword)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun switchToResetPassword() {
        Log.d(TAG,"switchToResetPassword()")
        try{
            (activity as LoginActivity).switchToResetPassword()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG,"onActivityResult()")
        facebookCallbackManager?.onActivityResult(requestCode,resultCode,data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun loginFacebook() {
        Log.d(TAG,"loginFacebook()")
        LoginManager.getInstance().logInWithReadPermissions(this
                , mutableListOf("public_profile","email"))
    }

    override fun displayLoading() {
        Log.d(TAG,"displayLoading()")
        load_view?.visibility = View.VISIBLE
        load_view?.bringToFront()
    }

    override fun hideLoading() {
        Log.d(TAG,"hideLoading()")
        load_view?.visibility = View.GONE
    }
}