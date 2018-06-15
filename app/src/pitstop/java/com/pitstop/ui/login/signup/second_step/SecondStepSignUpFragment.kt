package com.pitstop.ui.login.signup.first_step

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_signup_step_two.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SecondStepSignUpFragment: Fragment() , SecondStepSignUpView {

    private val TAG = SecondStepSignUpFragment::class.java.simpleName

    private var presenter: SecondStepSignUpPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_signup_step_two,container,false)
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
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context))
                    .build()
            presenter = SecondStepSignUpPresenter(useCaseComponent)
        }
        presenter?.subscribe(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.unsubscribe()
    }

    override fun getPhoneNumber(): String {
        Log.d(TAG,"getPhoneNumber()")
        return phone_number_field.text.toString()
    }

    override fun getLastName(): String {
        Log.d(TAG,"getLastName()")
        return last_name_field.text.toString()
    }

    override fun getFirstName(): String {
        Log.d(TAG,"getFirstName()")
        return first_name_field.text.toString()
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

    override fun setUsernameAndPassword(username: String, password: String) {
        Log.d(TAG,"setUsernameAndPassword")
        presenter?.setEmailAndPassword(username,password)
    }

    override fun displayLoading() {
        load_view.visibility = View.VISIBLE
    }

    override fun hideLoading() {
        load_view.visibility = View.GONE
    }
}