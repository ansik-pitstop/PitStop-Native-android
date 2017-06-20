package com.pitstop.ui.mainFragments;

import com.pitstop.EventBus.EventSource;
import com.pitstop.EventBus.EventType;

/**
 * Created by Karol Zdebel on 6/14/2017.
 */

public interface CarDataChangedNotifier {
    void notifyCarDataChanged(EventType eventType, EventSource eventSource);
}
