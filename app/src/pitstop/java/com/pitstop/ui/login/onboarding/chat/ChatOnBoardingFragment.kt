package com.pitstop.ui.login.onboarding.chat

import android.os.Bundle
import android.support.v4.app.Fragment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.pitstop.R
import com.pitstop.ui.login.LoginActivity
import kotlinx.android.synthetic.main.layout_slide_chat.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class ChatOnBoardingFragment: Fragment(), ChatOnBoardingView {

    private val TAG = PromotionsOnBoardingFragment::class.java.simpleName
    private var presenter: ChatOnboardingPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_slide_chat,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (presenter == null){
            presenter = ChatOnboardingPresenter()
        }
        presenter?.subscribe(this)
        next_button.setOnClickListener {
            presenter?.onNextClicked()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter?.unsubscribe()
    }

    override fun goToReminders() {
        Log.d(TAG,"goToNext()")
        try{
            (activity as LoginActivity).switchToRemindersOnBoarding()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}