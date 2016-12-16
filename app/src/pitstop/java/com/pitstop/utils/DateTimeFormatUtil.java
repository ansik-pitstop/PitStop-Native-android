package com.pitstop.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class DateTimeFormatUtil {

    private final static SimpleDateFormat sISO8601DateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US);

    public static String formatToISO8601(Calendar calendar){
        if (calendar == null) return null;
        return sISO8601DateFormat.format(calendar.getTime());
    }

    // 2016-08-02T17:48:21.457Z
    private final static SimpleDateFormat sIssueDoneAtDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public static Calendar formatISO8601ToCalendar(String dateString) throws ParseException{
        if (dateString == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(sIssueDoneAtDateFormat.parse(dateString));
        return calendar;
    }

    private final static SimpleDateFormat sReadableDateFormat = new SimpleDateFormat("EEE, MMMM dd, yyyy", Locale.US);

    public static String formatToReadableDate(Calendar calendar){
        if (calendar == null) return null;
        return sReadableDateFormat.format(calendar.getTime());
    }

    private final static SimpleDateFormat sReadableTimeFormat = new SimpleDateFormat("kk:mm aa", Locale.US);

    public static String formatToReadableTime(Calendar calendar){
        if (calendar == null) return null;
        return sReadableTimeFormat.format(calendar.getTime());
    }
}
