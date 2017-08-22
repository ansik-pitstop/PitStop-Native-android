package com.pitstop.ui.vehicle_health_report.emissions_test_progress.in_progress_view;

import com.pitstop.ui.vehicle_health_report.emissions_test_progress.EmissionsProgressCallback;

/**
 * Created by Matt on 2017-08-14.
 */

public class InProgressPresenter {

    private InProgressView view;
    private EmissionsProgressCallback callback;

    private boolean readyToStart;

    private final int LAST_CARD = 5;


    public InProgressPresenter(EmissionsProgressCallback callback){
        readyToStart = false;
        this.callback = callback;
    }

    public void subscribe(InProgressView view){
        this.view = view;
    }

    public void unsubscribe(){
        this.view = null;
    }

    public void onBackPressed(){
        if(view.getCardNumber() == 0 || view == null){return;}
        view.back();
    }
    public void onNextPressed(){
        if(view.getCardNumber() == LAST_CARD || view == null){return;}
        view.next();
        if(view.getCardNumber() == LAST_CARD && !readyToStart){
            setReady();
        }
    }

    public void setReady(){
        if(view == null){return;}
        view.setReady();
        readyToStart = true;
    }

    public void onBigButtonPressed(){
        if(view == null){return;}
        if (readyToStart){
            view.switchToProgress();
            view.startTimer();
        }else {
            view.bounceCards();
        }
    }
    public void showReport(){
        callback.setViewReport();
    }
}
