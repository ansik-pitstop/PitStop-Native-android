package com.pitstop.EventBus;

/**
 * Created by Karol Zdebel on 6/19/2017.
 */

public class EventTypeImpl implements EventType {

    private String eventType;

    public EventTypeImpl(String eventType){

        if (!eventType.equals(EventType.EVENT_CAR_ID)
                && !eventType.equals(EventType.EVENT_MILEAGE)
                && !eventType.equals(EventType.EVENT_SERVICES_HISTORY)
                && !eventType.equals(EventType.EVENT_SERVICES_NEW)
                && !eventType.equals(EventType.EVENT_CAR_DEALERSHIP)
                && !eventType.equals(EventType.EVENT_DTC_NEW)){

            throw new IllegalArgumentException();
        }

        this.eventType = eventType;
    }

    @Override
    public String getType() {
        return eventType;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EventType)){
            return false;
        }
        EventType other = (EventType)obj;

        return other.getType().equals(eventType);

    }
}
