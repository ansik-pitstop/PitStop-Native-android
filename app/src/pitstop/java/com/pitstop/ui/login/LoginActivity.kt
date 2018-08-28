package com.pitstop.ui.login

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.pitstop.R
import com.pitstop.ui.login.change_password.ChangePasswordFragment
import com.pitstop.ui.login.login.LoginFragment
import com.pitstop.ui.login.login_signup.LoginSignupFragment
import com.pitstop.ui.login.onboarding.chat.ChatOnBoardingFragment
import com.pitstop.ui.login.onboarding.chat.PromotionsOnBoardingFragment
import com.pitstop.ui.login.onboarding.reminders.RemindersOnBoardingFragment
import com.pitstop.ui.login.reset_password.ResetPasswordFragment
import com.pitstop.ui.login.signup.first_step.FirstStepSignUpFragment
import com.pitstop.ui.login.signup.first_step.SecondStepSignUpFragment
import com.pitstop.ui.main_activity.MainActivity
import com.pitstop.utils.LoginManager

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginActivity: AppCompatActivity() {
    companion object {
        const val USER_SIGNED_UP = "user_signed_up"
        const val ONBOARDING = "onboarding"
        const val USER_WAS_INACTIVE = "user_was_inactive" //Whether the user was inactive after logging in (created through dashboard)
    }

    private val TAG = LoginActivity::class.java.simpleName

    private val stepOneSignUpFragment = FirstStepSignUpFragment()
    private val secondStepSignUpFragment = SecondStepSignUpFragment()
    private val loginFragment = LoginFragment()
    private val signupLoginFragment = LoginSignupFragment()
    private val chatOnBoardingFragment = ChatOnBoardingFragment()
    private val remindersOnBoardingFragment = RemindersOnBoardingFragment()
    private val promotionsOnBoardingFragment = PromotionsOnBoardingFragment()
    private val changePasswordFragment = ChangePasswordFragment()
    private val resetPasswordFragment = ResetPasswordFragment()

    private var sharedPreferences: SharedPreferences? = null

    public override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG,"onCreate()")
        super.onCreate(savedInstanceState)

        sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(this)

        setContentView(R.layout.activity_login)
        if ((application as LoginManager).isLoggedIn()
                && !sharedPreferences!!.getBoolean(ONBOARDING,false)){
            switchToMainActivity(false)
        }else if (sharedPreferences!!.getBoolean(ONBOARDING,false)){
            switchToChatOnBoarding()
        }else{
            switchToLoginSignup()
        }
    }

    fun setUserWasInactive(userWasInactive: Boolean){
        Log.d(TAG,"setUserWasInactive() userWasInactive: $userWasInactive")
        sharedPreferences?.edit()?.putBoolean(USER_WAS_INACTIVE,userWasInactive)?.apply()
    }

    fun switchToSignupStepOne(){
        Log.d(TAG,"switchToSignupStepOne()")
        sharedPreferences?.edit()?.putBoolean(USER_WAS_INACTIVE,false)?.apply()
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
        sharedPreferences?.edit()?.putBoolean(USER_WAS_INACTIVE,false)?.apply()
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, loginFragment)
                .addToBackStack("login")
                .commit()
    }

    fun switchToLoginSignup(){
        Log.d(TAG,"switchToLoginSignup()")
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, signupLoginFragment)
                .commit()
    }

    fun switchToMainActivity(signedUp: Boolean){
        Log.d(TAG,"switchToMainActivity")
        val intent = Intent(LoginActivity@this, MainActivity::class.java)
        intent.putExtra(USER_SIGNED_UP,signedUp)
        intent.putExtra(USER_WAS_INACTIVE
                ,sharedPreferences?.getBoolean(USER_WAS_INACTIVE,false) ?: false)
        sharedPreferences?.edit()?.putBoolean(ONBOARDING,false)?.apply()
        startActivity(intent)
    }

    fun switchToChatOnBoarding(){
        Log.d(TAG,"switchToChatOnBoarding()")
        val count = supportFragmentManager.backStackEntryCount
        for (i in 1..count) {
            supportFragmentManager.popBackStack()
        }
        sharedPreferences?.edit()?.putBoolean(ONBOARDING,true)?.apply()
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, chatOnBoardingFragment)
                .commit()

    }

    fun switchToRemindersOnBoarding(){
        Log.d(TAG,"switchToRemindersOnBoarding()")
        supportFragmentManager.beginTransaction()
                .addToBackStack("reminders")
                .replace(R.id.fragment_container, remindersOnBoardingFragment)
                .commit()

    }

    fun switchToPromotionsOnBoarding(){
        Log.d(TAG,"switchToPromotionsOnBoarding()")
        supportFragmentManager.beginTransaction()
                .addToBackStack("promotions")
                .replace(R.id.fragment_container, promotionsOnBoardingFragment)
                .commit()

    }

    fun switchToChangePassword(oldPassword: String){
        Log.d(TAG,"switchToChangePassword()")
        val count = supportFragmentManager.backStackEntryCount
        for (i in 1..count) {
            supportFragmentManager.popBackStack()
        }
        changePasswordFragment.setOldPassword(oldPassword)
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, changePasswordFragment)
                .commit()

    }

    fun switchToResetPassword(){
        Log.d(TAG,"switchToResetPassword()")
        supportFragmentManager.beginTransaction()
                .addToBackStack("reset_password")
                .replace(R.id.fragment_container, resetPasswordFragment)
                .commit()

    }
}
