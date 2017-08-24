package com.pitstop.utils;

import com.pitstop.bluetooth.dataPackages.PidPackage;
import com.pitstop.bluetooth.dataPackages.TripInfoPackage;

/**
 * Created by Karol Zdebel on 8/24/2017.
 */

public class DeviceDataUtils {
    public static TripInfoPackage pidPackageToTripInfoPackage(PidPackage pidPackage){
        TripInfoPackage tripInfoPackage = new TripInfoPackage();
        try{
            tripInfoPackage.tripId = Integer.valueOf(pidPackage.tripId);
        }catch(NumberFormatException e){
            e.printStackTrace();
            tripInfoPackage.tripId = -1;
        }
        try{
            tripInfoPackage.mileage = Double.valueOf(pidPackage.tripMileage);
        }catch (NumberFormatException e){
            e.printStackTrace();
            tripInfoPackage.mileage = 0;
        }
        try{
            tripInfoPackage.rtcTime = Long.valueOf(pidPackage.rtcTime);
        }catch (NumberFormatException e){
            e.printStackTrace();
            tripInfoPackage.rtcTime = 0;
        }
        tripInfoPackage.flag = TripInfoPackage.TripFlag.END;
        try{
            tripInfoPackage.terminalRtcTime = Long.valueOf(pidPackage.rtcTime);
        }catch(NumberFormatException e){
            e.printStackTrace();
            tripInfoPackage.terminalRtcTime = -1;
        }
        tripInfoPackage.deviceId = pidPackage.deviceId;

        return tripInfoPackage;
    }
}
