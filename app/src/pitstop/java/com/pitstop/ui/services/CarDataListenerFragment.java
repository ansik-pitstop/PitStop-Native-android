package com.pitstop.ui.services;

import android.support.v4.app.Fragment;
import android.util.Log;

import com.pitstop.EventBus.CarDataChangedEvent;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class CarDataListenerFragment extends Fragment {

    final public static String TAG = CarDataListenerFragment.class.getSimpleName();

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCarDataChangedEvent(CarDataChangedEvent event){
        updateUI();
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    public abstract void updateUI();
}
