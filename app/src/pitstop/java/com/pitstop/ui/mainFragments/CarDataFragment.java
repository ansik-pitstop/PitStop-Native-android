package com.pitstop.ui.mainFragments;

import android.support.v4.app.Fragment;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventTypes;

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

public abstract class CarDataFragment extends Fragment implements CarDataChangedNotifier, EventTypes {

    final public static String TAG = CarDataFragment.class.getSimpleName();
    private boolean uiSynced = false;   //ui is not set yet
    private boolean running = false;    //not running
    private boolean wasPaused = false;  //pause never occured yet
    private boolean wasStopped = true;  //start in a stopped state
    private List<String> updateConstraints = new ArrayList<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){
        if (!updateConstraints.contains(event.getEventType())){
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
    public void setNoUpdateOnEventTypes(String[] eventTypes){
        for (String s: eventTypes){
            if (!updateConstraints.contains(s)){
                updateConstraints.add(s);
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        running = true;
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
    public void notifyCarDataChanged(String eventType){
        EventBus.getDefault().post(new CarDataChangedEvent(eventType));
    }

    public abstract void updateUI();
}
