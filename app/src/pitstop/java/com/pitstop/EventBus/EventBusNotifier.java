package com.pitstop.EventBus;

import org.greenrobot.eventbus.EventBus;

/**
 * Created by Karol Zdebel on 9/5/2017.
 */

public class EventBusNotifier {
    public static void notifyCarDataChanged(EventType eventType, EventSource eventSource){
        EventBus.getDefault().post(new CarDataChangedEvent(eventType, eventSource));
    }
}
