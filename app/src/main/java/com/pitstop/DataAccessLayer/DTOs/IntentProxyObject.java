package com.pitstop.DataAccessLayer.DTOs;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Paul Soladoye on 3/26/2016.
 */
public class IntentProxyObject implements Parcelable {
    private List<Car> carList = new ArrayList<>();

    public IntentProxyObject() {
    }

    public List<Car> getCarList() {
        return carList;
    }

    public void setCarList(List<Car> carList) {
        this.carList = carList;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeTypedList(this.carList);
    }

    protected IntentProxyObject(Parcel in) {
        this.carList = in.createTypedArrayList(Car.CREATOR);
    }

    public static final Creator<IntentProxyObject> CREATOR = new Creator<IntentProxyObject>() {
        @Override
        public IntentProxyObject createFromParcel(Parcel source) {
            return new IntentProxyObject(source);
        }

        @Override
        public IntentProxyObject[] newArray(int size) {
            return new IntentProxyObject[size];
        }
    };
}
