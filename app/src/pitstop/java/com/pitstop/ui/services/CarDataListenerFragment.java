package com.pitstop.ui.services;

import android.support.v4.app.Fragment;

import com.pitstop.EventBus.CarDataChangedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * This base class takes care of keeping the UI in sync with the most recent
 * version of car data.
 *
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class CarDataListenerFragment extends Fragment {

    final public static String TAG = CarDataListenerFragment.class.getSimpleName();
    private boolean uiSynced = false;
    private boolean running = false;

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){
        if (running){
            updateUI();
            uiSynced = true;
        }
        else{
            uiSynced = false;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        running = true;
        if (!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        running = false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!uiSynced){
            updateUI();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public abstract void updateUI();
}
