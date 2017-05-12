package com.pitstop.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;


public class DateTimeFormatUtil {

    public final static SimpleDateFormat ISO8601_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZZZZZ", Locale.US);
    public final static SimpleDateFormat READABLE_DATE_FORMAT = new SimpleDateFormat("EEE, MMMM dd, yyyy", Locale.US);
    public final static SimpleDateFormat READABLE_TIME_FORMAT = new SimpleDateFormat("kk:mm aa", Locale.US);
    public final static SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    public final static SimpleDateFormat ISSUE_DONE_AT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);

    public static String format(Calendar calendar, SimpleDateFormat format){
        if (calendar == null) return null;
        return format.format(calendar.getTime());
    }

    public static String formatToISO8601(Calendar calendar){
        if (calendar == null) return null;
        return ISO8601_FORMAT.format(calendar.getTime());
    }

    public static Calendar formatISO8601ToCalendar(String dateString) throws ParseException{
        if (dateString == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ISSUE_DONE_AT_DATE_FORMAT.parse(dateString));
        return calendar;
    }

    public static String formatToReadableDate(Calendar calendar){
        if (calendar == null) return null;
        return READABLE_DATE_FORMAT.format(calendar.getTime());
    }

    public static String formatToReadableTime(Calendar calendar){
        if (calendar == null) return null;
        return READABLE_TIME_FORMAT.format(calendar.getTime());
    }
}
