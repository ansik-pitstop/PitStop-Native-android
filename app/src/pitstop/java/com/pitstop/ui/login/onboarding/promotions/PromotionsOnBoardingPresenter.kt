package com.pitstop.ui.login.onboarding.chat

/**
 * Created by Karol Zdebel on 6/20/2018.
 */
class PromotionsOnBoardingPresenter{

    private var view: PromotionsOnBoardingView? = null

    fun subscribe(view: PromotionsOnBoardingView){
        this.view = view
    }

    fun unsubscribe(){
        this.view = null
    }

    fun onNextClicked(){
        view?.goToMainActivity()
    }
}