package com.pitstop.EventBus;

/**
 * Used to notify observers on the EventBus that car data has been
 * updated in the repositories.
 *
 * Created by Karol Zdebel on 6/13/2017.
 */

public class CarDataChangedEvent{

    private EventType eventType;
    private EventSource eventSource;

    public CarDataChangedEvent(EventType eventType, EventSource eventSource){
        this.eventType = eventType;
        this.eventSource = eventSource;
    }

    public EventType getEventType(){
        return eventType;
    }

    public EventSource getEventSource() {
        return eventSource;
    }
}
