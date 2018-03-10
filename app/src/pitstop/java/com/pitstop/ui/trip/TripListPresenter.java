package com.pitstop.ui.trip;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;
import com.pitstop.ui.mainFragments.TabPresenter;

/**
 * Created by David C. on 10/3/18.
 */

public class TripListPresenter extends TabPresenter<TripListView> {
    @Override
    public EventType[] getIgnoredEventTypes() {
        return new EventType[0];
    }

    @Override
    public void onAppStateChanged() {

    }

    @Override
    public EventSource getSourceType() {
        return null;
    }
}
