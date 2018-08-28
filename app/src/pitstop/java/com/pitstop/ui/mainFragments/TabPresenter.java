package com.pitstop.ui.mainFragments;

import android.util.Log;

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
 *
 * Abstract class which provides core logic for the classes responsible for
 * Presenting information in the tabs. EventBus is used to listen for events
 * which represent changes in the state of the model, therefore requiring
 * a refresh of the UI.
 *
 * Created by Karol Zdebel on 9/5/2017.
 */

public abstract class TabPresenter<T> implements Presenter<T> {

    final public static String TAG = TabPresenter.class.getSimpleName();
    private List<EventType> updateConstraints = new ArrayList<>();
    private T view;

    /**
     * Invoked when an event is received representing a change in the state of information/model
     * of the app
     *
     * @param event
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){
        Log.d(TAG,"onCarDataChangedEvent() event: "+event);

        /*Respond to event only if its EventType isn't being ignored
        * AND if it wasn't sent by this fragment*/
        if (!updateConstraints.contains(event.getEventType())
                && !event.getEventSource().equals(getSourceType())){
            onAppStateChanged();
        }
    }

    /**
     *
     * @param eventTypes events to be ignored when received through the event bus therefore, not
     *                   triggering the onAppStateChanged() method call
     */
    public void setNoUpdateOnEventTypes(EventType[] eventTypes){
        for (EventType e: eventTypes){
            if (!updateConstraints.contains(e)){
                updateConstraints.add(e);
            }
        }
    }

    /**
     *
     * @return view associated with the presenter
     */
    protected T getView(){
        return view;
    }

    /**
     *
     * @param view view that is subscribing for updates
     */
    @Override
    public void subscribe(T view) {
        this.view = view;
        setNoUpdateOnEventTypes(getIgnoredEventTypes());
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    /**
     * unsubscribe from updates from the presenter
     */
    @Override
    public void unsubscribe() {
        this.view = null;
        EventBus.getDefault().unregister(this);
    }

    /**
     *
     * @return list of events that are supposed to be ignored upon return from the event bus
     */
    public abstract EventType[] getIgnoredEventTypes();

    /**
     * Logic to execute if an event is received from the EventBus which isn't being ignored, typically
     * this will include updating the view
     */
    public abstract void onAppStateChanged();

    /**
     *
     * @return the source listening to events, this is used to make sure events sent by the source
     * aren't also captured by it triggering onAppStateChanged()
     */
    public abstract EventSource getSourceType();

}
