package com.pitstop.application;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.pitstop.utils.MixpanelHelper;

/**
 * Created by yifan on 16/10/24. <br>
 *
 * This class is currently used to track Mixpanel time event (App open). <br>
 * It is a callback for activity lifecycle, there could potentially be more usage in the future. <br>
 */
public class ActivityLifecycleObserver implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = ActivityLifecycleObserver.class.getSimpleName();

    private MixpanelHelper mMixpanelHelper;

    private boolean isForeground;
    private int activeActivityNumber = 0;
    private boolean trackStarted = false;

    public ActivityLifecycleObserver(Context context) {
        mMixpanelHelper = new MixpanelHelper((GlobalApplication)context.getApplicationContext());
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "Activity resumed: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "Activity paused: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "Activity created: " + activity.getLocalClassName());
    }

    @Override
    public void onActivityStarted(Activity activity) {
        activeActivityNumber++;
        isForeground = true;
        if (!trackStarted){
            Log.d(TAG, "Mixpanel start tracking");
            trackStarted = true;
            mMixpanelHelper.trackTimeEventStart(MixpanelHelper.TIME_EVENT_APP_OPEN);
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        activeActivityNumber--;
        isForeground = (activeActivityNumber != 0);
        if (!isForeground){
            Log.d(TAG, "Mixpanel end tracking");
            mMixpanelHelper.trackTimeEventEnd(MixpanelHelper.TIME_EVENT_APP_OPEN);
            trackStarted = false;
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "Activity destroyed: " + activity.getLocalClassName());
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

}
