package com.pitstop.ui.mainFragments;

import android.support.v4.app.Fragment;

import com.pitstop.database.LocalCarAdapter;
import com.pitstop.models.Car;

import java.util.List;

/**
 * Base class used for fragments that require local car database
 *  functionality
 *
 * Created by Karol Zdebel on 5/5/2017.
 */

public abstract class CarDataFragment extends Fragment{

    private LocalCarAdapter localCarStorage;
    private List<Car> carList;

    //Get the current/dashboard car
    protected Car getCurrentCar(){

        if (localCarStorage == null){
            localCarStorage = new LocalCarAdapter(getActivity());
        }
        carList = localCarStorage.getAllCars();

        for (Car c: carList){
            if (c.isCurrentCar()){
                return c;
            }
        }

        return null;
    }

}
