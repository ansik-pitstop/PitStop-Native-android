package com.pitstop.EventBus;

/**
 * Used to notify observers on the EventBus that car data has been
 * updated in the repositories.
 *
 * Created by Karol Zdebel on 6/13/2017.
 */

public class CarDataChangedEvent {

    private boolean mileageChanged;

    private boolean servicesChanged;
    private boolean carIdChanged;

    public CarDataChangedEvent(boolean mileageChanged, boolean servicesChanged, boolean carIdChanged) {
        this.mileageChanged = mileageChanged;
        this.servicesChanged = servicesChanged;
        this.carIdChanged = carIdChanged;
    }

    public boolean isMileageChanged() {
        return mileageChanged;
    }

    public boolean isServicesChanged() {
        return servicesChanged;
    }

    public boolean isCarIdChanged() {
        return carIdChanged;
    }
}
