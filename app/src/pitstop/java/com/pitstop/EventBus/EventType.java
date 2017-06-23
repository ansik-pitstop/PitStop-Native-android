package com.pitstop.EventBus;

/**
 * Created by Karol Zdebel on 6/14/2017.
 */

public interface EventType {
    public static String EVENT_CAR_ID = "event_car_id";
    public static String EVENT_MILEAGE = "event_mileage";
    public static String EVENT_SERVICES_NEW = "event_services_new";
    public static String EVENT_SERVICES_HISTORY = "event_services_history";
    public static String EVENT_CAR_DEALERSHIP = "event_car_dealership";
    public static String EVENT_DTC_NEW = "event_dtc_new";

    String getType();
}
