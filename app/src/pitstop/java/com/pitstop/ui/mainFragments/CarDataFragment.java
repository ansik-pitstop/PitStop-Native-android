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
 * Created by Karol Zdebel on 9/5/2017.
 */

public abstract class CarDataFragment extends Fragment {

    final public static String TAG = CarDataFragment.class.getSimpleName();
    private List<EventType> updateConstraints = new ArrayList<>();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){

        /*Respond to event only if its EventType isn't being ignored
        * AND if it wasn't sent by this fragment*/
        if (!updateConstraints.contains(event.getEventType())
                && !event.getEventSource().equals(getSourceType())){
            onAppStateChanged();
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public abstract void onAppStateChanged();
    public abstract EventSource getSourceType();

}
