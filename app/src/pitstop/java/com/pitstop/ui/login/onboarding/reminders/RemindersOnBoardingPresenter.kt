package com.pitstop.ui.login.onboarding.chat

import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class RemindersOnBoardingPresenter(private val mixpanelHelper: MixpanelHelper){

    private var view: RemindersOnBoardingView? = null

    fun subscribe(view: RemindersOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToPromotions()
        mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_ONBOARDING_REMINDERS, MixpanelHelper.SUCCESS)
    }
}