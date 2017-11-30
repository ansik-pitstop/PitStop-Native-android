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

    public static String formatDateToHistoryFormat(String rawDate) { // parse date that looks like "2009-07-28T20:12:29.533Z" to "Jul. 28, 2009"
        String[] splittedDate = rawDate.split("-");
        String[] months = new String[] {"null", "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};

        splittedDate[2] = splittedDate[2].substring(0, 2);

        return months[Integer.parseInt(splittedDate[1])] + ". " + splittedDate[2] + ", " + splittedDate[0];
    }

    public static int getHistoryDateToCompare(String rawDate) {
        if(rawDate == null || rawDate.isEmpty() || rawDate.equals("null")) {
            return 0;
        }

        String[] splittedDate = rawDate.split("-");
        splittedDate[2] = splittedDate[2].substring(0, 2);

        return Integer.parseInt(splittedDate[2])
                + Integer.parseInt(splittedDate[1]) * 30
                + Integer.parseInt(splittedDate[0]) * 365;
    }

    public static double historyFormatToDouble(String rawDate) { // parse date that looks like "2009-07-28T20:12:29.533Z" to "Jul. 28, 2009"
        String[] splitDate = rawDate.split(" ");
        String[] months = new String[] {"null", "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        int monthNum = 0;
        int counter = 0;
        for (String m: months){
            if (m.equals(splitDate[0])){
                monthNum = counter;
                break;
            }
            counter++;
        }

        int yearNum = Integer.valueOf(splitDate[1]);


        return yearNum + monthNum/12;
    }

    public static String formatToISO8601(Calendar calendar){
        if (calendar == null) return null;
        return ISO8601_FORMAT.format(calendar.getTime());
    }

    public static String rtcToIso(long rtc){
        return ISO8601_FORMAT.format(rtc);
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
