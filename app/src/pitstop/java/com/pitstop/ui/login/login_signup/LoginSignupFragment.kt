package com.pitstop.ui.login.login_signup

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_signup_login.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginSignupFragment: Fragment(), LoginSignupView {

    private val TAG = LoginSignupFragment::class.java.simpleName
    private var presenter: LoginSignupPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Log.d(TAG,"onCreateView()")
        val view = inflater.inflate(R.layout.layout_signup_login,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sign_up_button.setOnClickListener {
            presenter?.onSignupPressed()
        }
        login_button.setOnClickListener {
            presenter?.onLoginPressed()
        }
    }

    override fun onStart() {
        Log.d(TAG,"onStart()")
        super.onStart()
        if (presenter == null){
            presenter = LoginSignupPresenter()
        }
        presenter?.subscribe(this)

    }

    override fun onDestroy() {
        Log.d(TAG,"onDestroy()")
        super.onDestroy()
        presenter?.unsubscribe()
    }

    override fun switchToLogin() {
        Log.d(TAG,"switchToLogin()")
        try{
            (activity as LoginActivity).switchToLogin()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun switchToSignup() {
        Log.d(TAG,"switchToSignup()")
        try{
            (activity as LoginActivity).switchToSignup()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}