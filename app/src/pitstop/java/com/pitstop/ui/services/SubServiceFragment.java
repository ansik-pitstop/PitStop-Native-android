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

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        //Check whether UI will be set inside OnCreateView or whether it will have to happen inside OnStart()
        if (dashboardCar != null){
            setUI();
        }

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public static void setDashboardCar(Car c){
        Log.d("KAROL","SubServiceFragment, setDashboardCar, car.id:"+c.getId());
        dashboardCar = c;
    }

    public void onDashboardCarUpdated(){
        //Check whether onStart() finished, otherwise don't updateCarIssue since it'll updateCarIssue inside onStart
        if (getView() != null){
            setUI();
        }
    }

//    public void onMainServiceTabReopened(){
//        if (!uiUpdated){
//            setUI();
//            uiUpdated = true;
//        }
//    }

    public abstract void setUI();
}
