package com.pitstop.ui.services;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.pitstop.models.Car;

/**
 * Created by Work on 5/5/2017.
 */

public abstract class SubServiceFragment extends Fragment {

    public static Car dashboardCar;
    private boolean isViewShown;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUserVisibleHint(false);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser && getView() != null) {
            isViewShown = true;
        } else {
            isViewShown = false;
        }
    }

    /*Returns whether the view was being shown at the time of setUserVisibilityHint() being called,
    * if it wasn't then this means that OnCreateView() hasn't finished yet*/
    public boolean isViewShown(){
        return isViewShown;
    }

    public static void setDashboardCar(Car c){
        Log.d("KAROL","SubServiceFragment, setDashboardCar, car.id:"+c.getId());
        dashboardCar = c;
    }

    public abstract void onMainServiceTabReopened();
}
