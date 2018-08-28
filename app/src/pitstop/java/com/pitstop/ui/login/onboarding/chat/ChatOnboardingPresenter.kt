package com.pitstop.ui.login.onboarding.chat

import com.pitstop.utils.MixpanelHelper

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class ChatOnboardingPresenter(private val mixpanelHelper: MixpanelHelper){

    private var view: ChatOnBoardingView? = null

    fun subscribe(view: ChatOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToReminders()
        mixpanelHelper.trackSignUpProcess(MixpanelHelper.STEP_ONBOARDING_CHAT, MixpanelHelper.SUCCESS)
    }
}