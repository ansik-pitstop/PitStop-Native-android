package com.pitstop.ui.login.login

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R

/**
 * Created by Karol Zdebel on 6/14/2018.
 */
class LoginFragment: Fragment(), LoginView {

    private var presenter: LoginPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_signin,container,false)
        return view
    }

    override fun onStart() {
        super.onStart()
        if (presenter == null){
            presenter = LoginPresenter()
        }
        presenter?.subscribe(this)

    }

    override fun onDestroy() {
        super.onDestroy()
        presenter?.unsubscribe()
    }
}