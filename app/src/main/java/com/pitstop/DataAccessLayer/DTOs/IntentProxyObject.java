package com.pitstop.DataAccessLayer.DTOs;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/26/2016.
 */
public class IntentProxyObject implements Serializable {
    private List<Car> carList = new ArrayList<>();

    public IntentProxyObject() {
    }

    public List<Car> getCarList() {
        return carList;
    }

    public void setCarList(List<Car> carList) {
        this.carList = carList;
    }
}
