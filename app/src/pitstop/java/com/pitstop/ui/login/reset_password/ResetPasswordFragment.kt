package com.pitstop.ui.login.reset_password

import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.dependency.ContextModule
import com.pitstop.dependency.DaggerUseCaseComponent
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_reset_password.*

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ResetPasswordFragment: Fragment(), ResetPasswordView {

    private val TAG = ResetPasswordFragment::class.java.simpleName

    private var presenter: ResetPasswordPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_reset_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (presenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context))
                    .build()
            presenter = ResetPasswordPresenter(useCaseComponent)
        }
        presenter?.subscribe(this)

        reset_password_button.setOnClickListener { presenter?.onResetPasswordPressed() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.unsubscribe()
    }

    override fun getEmail(): String {
        return email.text.toString()
    }

    override fun displayErrorDialog(message: Int) {
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(resources.getString(message))
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun displayErrorDialog(message: String) {
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(message)
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun displaySuccessDialog(message: Int) {
        AlertDialog.Builder(context!!)
                .setTitle("Check Email")
                .setMessage(resources.getString(message))
                .setNeutralButton("Okay",{ dialogInterface: DialogInterface, i: Int -> presenter?.onPromptClosed()})
                .create()
                .show()
    }

    override fun switchToLogin() {
        Log.d(TAG,"switchToLogin()")
        try{
            (activity as LoginActivity).switchToLogin()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun displayLoading() {
        load_view.visibility = View.VISIBLE
        load_view.bringToFront()
    }

    override fun hideLoading() {
        load_view.visibility = View.GONE
    }
}