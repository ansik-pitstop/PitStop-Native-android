package com.pitstop.bluetooth.dataPackages;

import java.util.HashMap;

/**
 * Created by zohaibhussain on 2017-05-15.
 */

public class MultiParameterPackage extends ParameterPackage {
    public HashMap<ParamType, String> mParamsValueMap = new HashMap<>();

    @Override
    public String toString() {
        return "ParameterPackage{" +
                "success=" + success +
                ", deviceId='" + deviceId + '\'' +
                "," + mParamsValueMap.toString()+
                '}';
    }
}
