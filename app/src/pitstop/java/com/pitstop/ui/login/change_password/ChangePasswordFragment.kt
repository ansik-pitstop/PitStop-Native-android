package com.pitstop.ui.login.change_password

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
import kotlinx.android.synthetic.main.layout_update_password.*

/**
 * Created by Karol Zdebel on 6/29/2018.
 */
class ChangePasswordFragment: Fragment(), ChangePasswordView {

    private val TAG = ChangePasswordFragment::class.java.simpleName

    private var oldPassword: String? = null
    private var presenter: ChangePasswordPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_update_password, container, false)
    }

    fun setOldPassword(oldPassword: String){
        Log.d(TAG,"setOldPassword: $oldPassword")
        this.oldPassword = oldPassword
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (presenter == null){
            val useCaseComponent = DaggerUseCaseComponent.builder()
                    .contextModule(ContextModule(context))
                    .build()
            presenter = ChangePasswordPresenter(useCaseComponent)
        }
        presenter?.subscribe(this)
        change_password_button?.setOnClickListener { presenter?.onChangePasswordPressed() }
    }

    override fun getNewPassword(): String {
        return new_password.text.toString()
    }

    override fun getNewPasswordConfirmation(): String {
        return new_password_confirm_password.text.toString()
    }

    override fun getOldPassword(): String? {
        return oldPassword
    }

    override fun showErrorDialog(err: Int) {
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(resources.getString(err))
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun showErrorDialog(err: String) {
        AlertDialog.Builder(context!!)
                .setTitle("Error")
                .setMessage(err)
                .setNeutralButton("Okay",null)
                .create()
                .show()
    }

    override fun switchToOnboarding() {
        Log.d(TAG,"switchToOnboarding()")
        try{
            (activity as LoginActivity).switchToChatOnBoarding()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }

    override fun showLoading() {
        load_view.visibility = View.VISIBLE
        load_view?.bringToFront()
    }

    override fun hideLoading() {
        load_view?.visibility = View.GONE
    }

}