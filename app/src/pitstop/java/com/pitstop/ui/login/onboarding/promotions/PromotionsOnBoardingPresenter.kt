package com.pitstop.ui.login.onboarding.chat

import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class PromotionsOnBoardingPresenter(private val mixpanelHelper: MixpanelHelper){

    private var view: PromotionsOnBoardingView? = null

    fun subscribe(view: PromotionsOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToMainActivity()
        mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_ONBOARDING_PROMOTIONS, MixpanelHelper.SUCCESS)
    }
}