package com.pitstop.bluetooth.handler;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.pitstop.dependency.UseCaseComponent;
import com.pitstop.interactors.add.AddAlarmUseCase;
import com.pitstop.models.Alarm;
import com.pitstop.network.RequestError;
import com.pitstop.observer.AlarmObservable;

import org.jetbrains.annotations.NotNull;

/**
 * Created by ishan on 2017-11-02.
 */

public class AlarmHandler {

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final String TAG = AlarmHandler.class.getSimpleName();
    private AlarmObservable alarmObservable;
    private UseCaseComponent useCaseComponent;

    public AlarmHandler(AlarmObservable alarmObservable, UseCaseComponent useCaseComponent){
        this.alarmObservable = alarmObservable;
        this.useCaseComponent = useCaseComponent;
    }

    public void handleAlarm(Alarm alarm){
        useCaseComponent.addAlarmUseCase().execute(alarm, new AddAlarmUseCase.Callback() {
            @Override
            public void onAlarmAdded(@NotNull Alarm alarm) {
                alarmObservable.notifyAlarmAdded(alarm);
            }

            @Override
            public void onError(@NotNull RequestError requestError) {
                Log.d(TAG, "alarm added error");
            }

            @Override
            public void onAlarmsDisabled() {
                // do nothing with the alarm
                Log.d(TAG, "alarmsAreDisabled()");

            }
        });

    }

}
