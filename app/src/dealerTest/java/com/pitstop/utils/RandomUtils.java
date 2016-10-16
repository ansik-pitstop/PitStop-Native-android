package com.pitstop.utils;

import java.util.Random;

/**
 * Created by yifan on 16/10/14.
 */

public class RandomUtils {

    public static final int VIN_LENGTH = 17;

    private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";

    public static String getRandomStringAsVin(int count) {
        StringBuilder builder = new StringBuilder();
        while (count-- != 0) {
            int character = (int) (Math.random() * ALPHA_NUMERIC_STRING.length());
            builder.append(ALPHA_NUMERIC_STRING.charAt(character));
        }
        return builder.toString();
    }

    public static Random randomGenerator = new Random();

    public static int getRandomTripId(){
        return randomGenerator.nextInt(1000000000);
    }

}
