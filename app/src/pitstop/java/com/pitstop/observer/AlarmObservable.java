package com.pitstop.observer;

import com.pitstop.models.Alarm;

/**
 * Created by ishan on 2017-10-30.
 */

public interface AlarmObservable extends Subject {

    void notifyAlarmAdded(Alarm alarm);
}
