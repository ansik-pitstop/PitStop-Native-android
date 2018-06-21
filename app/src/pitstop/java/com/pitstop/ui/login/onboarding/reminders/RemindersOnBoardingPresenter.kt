package com.pitstop.ui.login.onboarding.chat

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class RemindersOnBoardingPresenter{

    private var view: RemindersOnBoardingView? = null

    fun subscribe(view: RemindersOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToPromotions()
    }
}