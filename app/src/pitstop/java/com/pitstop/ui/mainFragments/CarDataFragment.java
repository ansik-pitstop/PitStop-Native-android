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
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class CarDataFragment extends Fragment implements CarDataChangedNotifier, EventTypes {

    final public static String TAG = CarDataFragment.class.getSimpleName();
    private boolean uiSynced = false;
    private boolean running = false;
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
        running = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!uiSynced){
            updateUI();
            uiSynced = true;
        }
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
