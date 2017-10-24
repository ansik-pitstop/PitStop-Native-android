package com.pitstop.EventBus;

/**
 * Created by Karol Zdebel on 6/19/2017.
 */

public class EventSourceImpl implements EventSource {

    private String eventSource;

    public EventSourceImpl(String eventSource){

        if (!eventSource.equals(EventSource.SOURCE_APPOINTMENTS)
                && !eventSource.equals(EventSource.SOURCE_DASHBOARD)
                && !eventSource.equals(EventSource.SOURCE_NOTIFICATIONS)
                && !eventSource.equals(EventSource.SOURCE_REQUEST_SERVICE)
                && !eventSource.equals(EventSource.SOURCE_SCAN)
                && !eventSource.equals(EventSource.SOURCE_SERVICES_CURRENT)
                && !eventSource.equals(EventSource.SOURCE_SERVICES_HISTORY)
                && !eventSource.equals(EventSource.SOURCE_SERVICES_UPCOMING)
                && !eventSource.equals(EventSource.SOURCE_TRIPS)
                && !eventSource.equals(EventSource.SOURCE_SETTINGS)
                && !eventSource.equals(EventSource.SOURCE_ADD_CAR)
                && !eventSource.equals(EventSource.SOURCE_BLUETOOTH_AUTO_CONNECT)
                && !eventSource.equals(EventSource.SOURCE_MY_GARAGE)
                && !eventSource.equals(EventSource.SOURCE_DRAWER)
        ){

            throw new IllegalArgumentException();
        }

        this.eventSource = eventSource;
    }

    @Override
    public String getSource() {
        return eventSource;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EventSource)){
            return false;
        }
        EventSource other = (EventSource)obj;

        return other.getSource().equals(eventSource);

    }
}
