package com.pitstop.EventBus;

/**
 * Used to notify observers on the EventBus that car data has been
 * updated in the repositories.
 *
 * Created by Karol Zdebel on 6/13/2017.
 */

public class CarDataChangedEvent implements EventTypes {

    private String eventType;

    public CarDataChangedEvent(String eventType){
        if (!eventType.equals(EventTypes.EVENT_CAR_ID)
                && !eventType.equals(EventTypes.EVENT_MILEAGE)
                && !eventType.equals(EventTypes.EVENT_SERVICES_HISTORY)
                && !eventType.equals(EventTypes.EVENT_SERVICES_NEW)){

            throw new IllegalArgumentException();
        }

        this.eventType = eventType;
    }

    public String getEventType(){
        return eventType;
    }
}
