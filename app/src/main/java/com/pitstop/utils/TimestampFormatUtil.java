package com.pitstop.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Created by yifan on 16/8/31.
 */
public class TimestampFormatUtil {

    private static SimpleDateFormat sSimpleDateFormat = new SimpleDateFormat();

    public final static int ISO8601 = 1077;

    private final static String ISO8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZZZ";

    public static String format(Calendar calendar, int format) {
        switch (format){
            case ISO8601:
                sSimpleDateFormat = new SimpleDateFormat(ISO8601_DATE_FORMAT, Locale.US);
                return sSimpleDateFormat.format(calendar.getTime());

            default:
                return null;
        }

    }
}
