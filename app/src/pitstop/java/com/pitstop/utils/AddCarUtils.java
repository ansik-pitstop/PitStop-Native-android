package com.pitstop.utils;

/**
 * Created by Karol Zdebel on 8/2/2017.
 */

public class AddCarUtils {

    public static final int MAX_MILEAGE = 3000000;
    public static final int MIN_MILEAGE = 0;

    public static boolean isVinValid(String vin){
        vin = removeWhitespace(vin);
        return vin != null && (vin.length() == 17);
    }

    public static String removeWhitespace(String s){
        return s.replace(" ","").replace("\n","").replace("\t","");
    }

    public static boolean isMileageValid(String mileage){
        try{
            int intMileage = Integer.valueOf(mileage);
            return intMileage >= MIN_MILEAGE && intMileage <= MAX_MILEAGE;
        }
        catch(NumberFormatException e){
            return false;
        }
    }

    public static boolean isMileageValid(int mileage){
        return mileage >= MIN_MILEAGE && mileage <= MAX_MILEAGE;
    }
}
