package com.pitstop.ui.services;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pitstop.models.Car;

/**
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class SubServiceFragment extends Fragment {

    public static Car dashboardCar;
    private boolean isViewShown;
    private boolean onCreateViewReady;
    private boolean onStartFinished = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setUserVisibleHint(false);
        onStartFinished = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //Check whether onCreateView() has finished
        if (isVisibleToUser && getView() != null) {
            isViewShown = true;
        } else {
            isViewShown = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Check whether UI will be set inside OnCreateView or whether it will have to happen inside OnStart()
        if (isViewShown() || dashboardCar == null){
            onCreateViewReady = false;
        }
        else{
            onCreateViewReady = true;
            setUI();
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();

        /*If the fragment didn't receive the current car in OnCreateView or SetUserVisibilityHint
        then either the dashboard car will be available here or onDashboardCarUpdated()
        will be called once onStart() finishes*/
        if (!onCreateViewReady && dashboardCar != null){
            setUI();
        }

        onStartFinished = true;
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

    public void onDashboardCarUpdated(){
        //Check whether onStart() finished, otherwise don't update since it'll update inside onStart
        if (getView() != null){
            setUI();
        }
    }

    public abstract void onMainServiceTabReopened();

    public abstract void setUI();
}
