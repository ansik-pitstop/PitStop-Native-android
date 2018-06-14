package com.pitstop.ui.login

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity
import com.pitstop.R
import com.pitstop.ui.login.login.LoginFragment
import com.pitstop.ui.login.login_signup.LoginSignupFragment
import com.pitstop.ui.login.signup.SignupFragment

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginActivity: AppCompatActivity() {
    companion object {
        const val USER_SIGNED_UP = "user_signed_up"
    }

    private val signupFragment = SignupFragment()
    private val loginFragment = LoginFragment()
    private val signupLoginFragment = LoginSignupFragment()

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.activity_login)
    }

    override fun onStart() {
        switchToLoginSignup()
        super.onStart()
    }

    fun switchToSignup(){
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, signupFragment)
                .commit()
    }

    fun switchToLogin(){
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, loginFragment)
                .commit()
    }

    fun switchToLoginSignup(){
        supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, signupLoginFragment)
                .commit()
    }
}