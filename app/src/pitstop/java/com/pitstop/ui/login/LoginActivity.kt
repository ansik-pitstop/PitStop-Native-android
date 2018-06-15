package com.pitstop.ui.login

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pitstop.R
import com.pitstop.ui.login.login.LoginFragment
import com.pitstop.ui.login.login_signup.LoginSignupFragment
import com.pitstop.ui.login.signup.first_step.FirstStepSignUpFragment
import com.pitstop.ui.login.signup.first_step.SecondStepSignUpFragment
import com.pitstop.ui.main_activity.MainActivity

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginActivity: AppCompatActivity() {
    companion object {
        const val USER_SIGNED_UP = "user_signed_up"
    }

    private val TAG = LoginActivity::class.java.simpleName

    private val stepOneSignUpFragment = FirstStepSignUpFragment()
    private val secondStepSignUpFragment = SecondStepSignUpFragment()
    private val loginFragment = LoginFragment()
    private val signupLoginFragment = LoginSignupFragment()

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        switchToLoginSignup()
    }

    override fun onStart() {
        Log.d(TAG,"onStart()")
        super.onStart()
    }

    fun switchToSignupStepOne(){
        Log.d(TAG,"switchToSignupStepOne()")
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, stepOneSignUpFragment)
                .addToBackStack("signup")
                .commit()
    }

    fun switchToSignupStepTwo(username: String, password: String){
        Log.d(TAG,"switchToSignupStepTwo() u:$username")
        secondStepSignUpFragment.setUsernameAndPassword(username,password)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, secondStepSignUpFragment)
                .addToBackStack("signup")
                .commit()
    }

    fun switchToLogin(){
        Log.d(TAG,"switchToLogin()")
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack("login")
                .commit()
    }

    fun switchToLoginSignup(){
        Log.d(TAG,"switchToLoginSignup()")
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, signupLoginFragment)
                .addToBackStack("signup_login")
                .commit()
    }

    fun switchToMainActivity(){
        Log.d(TAG,"switchToMainActivity")
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }
}