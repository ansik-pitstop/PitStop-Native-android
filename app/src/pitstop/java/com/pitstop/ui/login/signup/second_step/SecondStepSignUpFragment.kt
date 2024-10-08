package com.pitstop.ui.login.signup.first_step

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.login.LoginActivity
import com.pitstop.utils.MixpanelHelper
import kotlinx.android.synthetic.main.layout_signup_step_two.*

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class SecondStepSignUpFragment: Fragment() , SecondStepSignUpView {

    private val TAG = SecondStepSignUpFragment::class.java.simpleName

    private var presenter: SecondStepSignUpPresenter? = null
    private var username: String? = null //Set by activity, data is input by previous fragment
    private var password: String? = null //Set by activity, data is input by previous fragment

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
                    .contextModule(ContextModule(activity?.applicationContext))
                    .build()
            presenter = SecondStepSignUpPresenter(useCaseComponent
                    , MixpanelHelper(context?.applicationContext as GlobalApplication))
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

    override fun switchToOnBoarding() {
        Log.d(TAG,"switchToOnBoarding()")
        try{
            (activity as LoginActivity).switchToChatOnBoarding()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun setUsernameAndPassword(username: String, password: String) {
        Log.d(TAG,"setUsernameAndPassword")
        this.username = username
        this.password = password
    }

    override fun getPassword(): String {
        Log.d(TAG,"getPassword() password: $password")
        return password!!
    }

    override fun getUsername(): String {
        Log.d(TAG,"getUsername() username: $username")
        return username!!
    }

    override fun displayLoading() {
        load_view?.visibility = View.VISIBLE
        load_view?.bringToFront()
    }

    override fun hideLoading() {
        load_view?.visibility = View.GONE
    }
}