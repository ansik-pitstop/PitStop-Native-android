package com.pitstop.ui.login.onboarding.chat

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.pitstop.R
import com.pitstop.application.GlobalApplication
import com.pitstop.ui.login.LoginActivity
import com.pitstop.utils.MixpanelHelper
import kotlinx.android.synthetic.main.layout_slide_reminders.*

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class PromotionsOnBoardingFragment: Fragment(), PromotionsOnBoardingView {

    private val TAG = PromotionsOnBoardingFragment::class.java.simpleName
    private var presenter: PromotionsOnBoardingPresenter? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.layout_slide_promotions,container,false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (presenter == null){
            presenter = PromotionsOnBoardingPresenter(MixpanelHelper(context?.applicationContext as GlobalApplication))
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

    override fun goToMainActivity() {
        Log.d(TAG,"goToNext()")
        try{
            (activity as LoginActivity).switchToMainActivity(true)
        }catch(e: Exception){
            e.printStackTrace()
        }
    }
}