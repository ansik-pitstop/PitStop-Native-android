package com.pitstop.ui.login.signup

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.pitstop.R
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_signup.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SignupFragment: Fragment() ,SignupView {

    private val TAG = SignupFragment::class.java.simpleName

    private var presenter: SignupPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_signup,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        signup_button.setOnClickListener {
            presenter?.onSignupPressed()
        }
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null){
            presenter = SignupPresenter()
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

    override fun goToMainActivity() {
        Log.d(TAG,"goToMainActivity()")
        try{
            (activity as LoginActivity).switchToMainActivity()
        }catch(e: Exception){
            e.printStackTrace()
        }
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

    override fun switchToMainActivity() {
        Log.d(TAG,"switchToMainActivity()")
        try{
            (activity as LoginActivity).switchToMainActivity()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}