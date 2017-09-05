package com.pitstop.ui.mainFragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.View;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * This base class takes care of keeping the UI in sync with the most recent
 * version of car data.
 *
 * Updates in the following cases:
 *  -Car data change alert takes place and the Fragment is not ignoring that event type
 *  -Activity start
 *  -Activity resume only if was paused.
 *
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class CarDataFragmentOld extends Fragment implements CarDataChangedNotifier {

    final public static String TAG = CarDataFragmentOld.class.getSimpleName();
    private boolean uiSynced = false;   //ui is not set yet
    private boolean running = false;    //not running2
    private boolean wasPaused = false;  //pause never occured yet
    private boolean wasStopped = true;  //start in a stopped state
    private boolean firstStart = true; //whether its the first time onStart() called
    private List<EventType> updateConstraints = new ArrayList<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){

        /*Respond to event only if its EventType isn't being ignored
        * AND if it wasn't sent by this fragment*/
        if (!updateConstraints.contains(event.getEventType())
                && !event.getEventSource().equals(getSourceType())){
            if (running){
                updateUI();
                uiSynced = true;
            }
            else{
                uiSynced = false;
            }
        }
    }

    //These event types will not trigger an update in the UI
    public void setNoUpdateOnEventTypes(EventType[] eventTypes){
        for (EventType e: eventTypes){
            if (!updateConstraints.contains(e)){
                updateConstraints.add(e);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        //Update UI on every onStart() other than the first one
        //since the UI will be initialized in the onCreate() in that case
        if (!firstStart && !uiSynced){
            updateUI();
            uiSynced = true;
        }

        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        firstStart = false;
        running = true;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        wasStopped = true;
        running = false;
    }

    @Override
    public void onPause() {
        super.onPause();
        wasPaused = true;
    }

    @Override
    public void onResume() {
        super.onResume();

        //Only update UI if onStart() hasn't
        if (!uiSynced && wasPaused && !wasStopped){
            updateUI();
            uiSynced = true;
        }
        wasStopped = false;
        wasPaused = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void notifyCarDataChanged(EventType eventType ,EventSource eventSource){
        EventBus.getDefault().post(new CarDataChangedEvent(eventType, eventSource));
    }

    public abstract void updateUI();

    public abstract EventSource getSourceType();
}
