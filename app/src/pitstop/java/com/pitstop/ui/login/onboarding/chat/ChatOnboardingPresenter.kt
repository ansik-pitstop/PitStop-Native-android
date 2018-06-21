package com.pitstop.ui.login.onboarding.chat

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class ChatOnboardingPresenter{

    private var view: ChatOnBoardingView? = null

    fun subscribe(view: ChatOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToReminders()
    }
}