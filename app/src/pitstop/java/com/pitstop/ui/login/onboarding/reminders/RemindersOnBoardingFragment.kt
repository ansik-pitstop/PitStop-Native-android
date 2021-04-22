package com.pitstop.ui.login.onboarding.reminders

/**
 * Created by Karol Zdebel on 6/20/2018.
 */

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.ui.login.LoginActivity
import com.pitstop.ui.login.onboarding.chat.RemindersOnBoardingPresenter
import com.pitstop.ui.login.onboarding.chat.RemindersOnBoardingView
import com.pitstop.utils.MixpanelHelper
import kotlinx.android.synthetic.main.layout_slide_chat.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class RemindersOnBoardingFragment: Fragment(), RemindersOnBoardingView {

    private val TAG = RemindersOnBoardingFragment::class.java.simpleName
    private var presenter: RemindersOnBoardingPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_slide_reminders,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (presenter == null){
            presenter = RemindersOnBoardingPresenter(MixpanelHelper(context?.applicationContext as GlobalApplication))
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

    override fun goToPromotions() {
        Log.d(TAG,"goToNext()")
        try{
            (activity as LoginActivity).switchToPromotionsOnBoarding()
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}