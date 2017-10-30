package com.pitstop.ui.mainFragments;

import com.pitstop.EventBus.CarDataChangedEvent;
import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.ui.Presenter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 9/5/2017.
 */

public abstract class TabPresenter<T> implements Presenter<T> {

    final public static String TAG = TabPresenter.class.getSimpleName();
    private List<EventType> updateConstraints = new ArrayList<>();
    private T view;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){

        /*Respond to event only if its EventType isn't being ignored
        * AND if it wasn't sent by this fragment*/
        if (!updateConstraints.contains(event.getEventType())
                && !event.getEventSource().equals(getSourceType())){
            onAppStateChanged();
        }
    }
    //These event types will not trigger an updateMileage in the UI
    public void setNoUpdateOnEventTypes(EventType[] eventTypes){
        for (EventType e: eventTypes){
            if (!updateConstraints.contains(e)){
                updateConstraints.add(e);
            }
        }
    }

    protected T getView(){
        return view;
    }

    @Override
    public void subscribe(T view) {
        this.view = view;
        setNoUpdateOnEventTypes(getIgnoredEventTypes());
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void unsubscribe() {
        this.view = null;
        EventBus.getDefault().unregister(this);
    }

    public abstract EventType[] getIgnoredEventTypes();
    public abstract void onAppStateChanged();
    public abstract EventSource getSourceType();

}
