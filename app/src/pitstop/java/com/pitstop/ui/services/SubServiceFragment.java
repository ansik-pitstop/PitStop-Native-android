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
    private boolean uiUpdated = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uiUpdated = false;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        //Check whether onCreateView() has finished
        if (isVisibleToUser && getView() != null && dashboardCar != null) {
            setUI();
            uiUpdated = true;
        } else {
            uiUpdated = false;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Check whether UI will be set inside OnCreateView or whether it will have to happen inside OnStart()
        if (!uiUpdated && dashboardCar != null){
            setUI();
            uiUpdated = true;
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static void setDashboardCar(Car c){
        Log.d("KAROL","SubServiceFragment, setDashboardCar, car.id:"+c.getId());
        dashboardCar = c;
    }

    public void onDashboardCarUpdated(){
        //Check whether onStart() finished, otherwise don't update since it'll update inside onStart
        if (getView() != null && !uiUpdated){
            setUI();
            uiUpdated = true;
        }
    }

    public void onMainServiceTabReopened(){
        if (!uiUpdated){
            setUI();
            uiUpdated = true;
        }
    }

    public abstract void setUI();
}
