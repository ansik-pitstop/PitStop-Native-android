package com.pitstop.EventBus;

/**
 * Created by Karol Zdebel on 6/19/2017.
 */

public interface EventSource {
    public static String SOURCE_DASHBOARD = "source_dashboard";
    public static String SOURCE_SERVICES_CURRENT = "source_services_current";
    public static String SOURCE_SERVICES_UPCOMING = "source_services_upcoming";
    public static String SOURCE_SERVICES_HISTORY = "source_services_history";
    public static String SOURCE_SCAN = "source_scan";
    public static String SOURCE_NOTIFICATIONS = "source_notifications";
    public static String SOURCE_SETTINGS = "source_settings";
    public static String SOURCE_REQUEST_SERVICE = "source_request_service";
    public static String SOURCE_APPOINTMENTS = "source_appointments";
    public static String SOURCE_TRIPS = "source_trips";
    public static String SOURCE_ADD_CAR = "source_add_car";
    public static String SOURCE_BLUETOOTH_AUTO_CONNECT = "source_bluetooth_auto_connect";

    String getSource();

}
