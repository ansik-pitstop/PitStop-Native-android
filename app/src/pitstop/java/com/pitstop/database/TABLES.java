package com.pitstop.database;

/**
 * Created by psola on 3/31/2016.
 */
public final class TABLES {

    public interface COMMON {
        //Common column names
        String KEY_ID = "id";
        String KEY_OBJECT_ID = "parseId";
        String KEY_CREATED_AT = "createdAt";
        String KEY_TIMESTAMP = "timestamp";
    }

    public interface DEBUG_MESSAGES extends COMMON {
        String TABLE_NAME = "debug_messages";
        String COLUMN_ID = "id";
        String COLUMN_TIMESTAMP = "phoneTimestamp";
        String COLUMN_TYPE = "type";
        String COLUMN_MESSAGE = "message";
        String COLUMN_LEVEL = "level";
        String COLUMN_TAG = "tag";
        String COLUMN_SENT = "sent";
    }

    public interface PID {
        String TABLE_NAME = "pidData";
        String TABLE_NAME_RESULT_4 = "pidResult4";
        String KEY_DATANUM = "dataNum";
        String KEY_DEVICE_ID = "deviceId";
        String KEY_RTCTIME = "bluetoothDeviceTime";
        String KEY_TIMESTAMP = "phoneTimestamp";
        String KEY_TRIP_ID_RAW = "tripIdRaw";
        String KEY_TRIP_ID = "tripId";
        String KEY_PIDS = "pids";
        String KEY_MILEAGE = "mileage";
        String KEY_CALCULATED_MILEAGE = "calculatedMileage";
    }

    public interface CAR {
        String TABLE_NAME = "car";
        String KEY_VIN = "vin";
        String KEY_SHOP_ID = "shopId";
        String KEY_MILEAGE = "totalMileage";
        String KEY_DISPLAYED_MILEAGE = "displayedMileage";
        String KEY_SCANNER_ID = "scannerId";
        String KEY_MAKE = "make";
        String KEY_MODEL = "model";
        String KEY_YEAR = "year";
        String KEY_ENGINE = "engine";
        String KEY_TRIM = "trimLevel";
        String KEY_USER_ID = "ownerId";
        String KEY_NUM_SERVICES = "numberOfServices";
        String KEY_IS_DASHBOARD_CAR = "isDashboardCar";
    }

    public interface CAR_PENDING {
        String TABLE_NAME = "car_pending";
        String KEY_CAR_ID = "car_id";
        String KEY_TYPE = "type";
        String KEY_VALUE = "value";
    }

    public interface APPOINTMENT {
        String TABLE_NAME = "appointment";
        String KEY_COMMENT = "comment";
        String KEY_DATE = "date";
        String KEY_STATE = "state";
        String KEY_SHOP_ID = "shopId";
    }

    public interface PENDING_TRIP_DATA_LOCATIONS {
        String TABLE_NAME = "pending_trip_data_locations";
        String KEY_LOCATION_ID = "location_id";
        String KEY_LONGITUDE = "longitude";
        String KEY_LATITUDE = "latitude";
        String KEY_TIME = "time";
    }

    public interface PENDING_TRIP_DATA {
        String TABLE_NAME = "pending_trip_data";
        String KEY_TRIP_ID = "trip_id";
        String KEY_START_TIMESTAMP = "start_timestamp";
        String KEY_END_TIMESTAMP = "start_timestamp";
        String KEY_VIN = "vin";
    }

    public interface ACTIVITY_DATA {
        String TABLE_NAME = "activity_data";
        String KEY_TIME = "time";
        String KEY_VIN = "vin";
        String KEY_CONFIDENCE = "confidence";
        String KEY_TYPE = "type";
    }

    public interface LOCATION_DATA {
        String TABLE_NAME = "location_data";
        String KEY_TIME = "time";
        String KEY_VIN = "vin";
        String KEY_CONFIDENCE = "confidence";
        String KEY_LONGITUDE = "longitude";
        String KEY_LATITUDE = "latitude";
    }

    public interface TRIP_DEVICE{
        String TABLE_NAME = "tripsDevice";
        String KEY_TRIP_ID_RAW = "tripIdRaw";
        String KEY_MILEAGE = "mileage";
        String KEY_RTC = "rtc";
        String KEY_TERMINAL_RTC = "terminalRtcTime";
        String KEY_DEVICE_ID = "deviceId";
        String KEY_TRIP_TYPE = "tripType";
    }

    public interface CAR_ISSUES {
        String TABLE_NAME = "carIssues";

        String KEY_CAR_ID = "carId";
        String KEY_STATUS = "status";
        String KEY_TIMESTAMP = "phoneTimestamp";
        String KEY_PRIORITY = "priority";
        String KEY_ISSUE_TYPE = "issueType";
        String KEY_ITEM = "item";
        String KEY_DESCRIPTION = "description";
        String KEY_ACTION = "action";
        String KEY_SYMPTOMS = "symptoms";
        String KEY_CAUSES = "causes";
        String KEY_UPCOMING_ISSUE_MILEAGE = "upcoming_issue_mileage";
    }

    public interface USER {
        String TABLE_NAME = "user";

        String KEY_FIRST_NAME = "firstName";
        String KEY_LAST_NAME = "lastName";
        String KEY_EMAIL = "email";
        String KEY_PHONE = "phone";
        String KEY_CAR = "carId";
        String KEY_ALARMS_ENABLED = "alarmsEnabled";
        String KEY_FIRST_CAR_ADDED = "isFirstCarAdded";

    }

    public interface SHOP {
        String TABLE_NAME = "dealership";

        String KEY_NAME = "name";
        String KEY_ADDRESS = "address";
        String KEY_PHONE = "phone";
        String KEY_EMAIL = "email";
        String KEY_SHOP_ID = "shopId";
        String KEY_IS_CUSTOM = "isCustom";
    }

    public interface NOTIFICATION {
        String TABLE_NAME = "notifications";

        String KEY_ALERT = "alert";
        String KEY_TITLE = "title";
    }

    public interface SCANNER {
        String TABLE_NAME = "scanners";
        String KEY_CAR_ID = "carId";
        String KEY_DEVICE_NAME = "deviceName";
        String KEY_SCANNER_ID = "scannerId";
        String KEY_DATANUM = "datanum";
    }
    public interface LOCAL_SPECS_DATA{
        String TABLE_NAME = "localCarData";
        String KEY_CAR_ID = "carId";
        String LICENSE_PLATE = "licensePlate";

    }

    public interface LOCAL_ALARMS {
        String TABLE_NAME = "localAlarms";
        String ID = "id";
        String CAR_ID = "carId";
        String RTC_TIME = "bluetoothDeviceTime";
        String ALARM_EVENT = "alarmEvent";
        String ALARM_VALUE = "alarmValue";
    }

    public interface LOCAL_FUEL_CONSUMPTION{
        String TABLE_NAME = "LocalFuelConsumption";
        String SCANNER_ID = "scannerID";
        String FUEL_CONSUMED = "fuelConsumed";
    }

    // Sensor Data tables //
    public interface SENSOR_DATA{
        String TABLE_NAME = "SensorData";
        String DEVICE_ID = "deviceId";
        String RTC_TIME = "bluetoothDeviceTime";
        String VIN = "vin";
        String DEVICE_TIMESTAMP = "deviceTimestamp";
        String DEVICE_TYPE = "deviceType";
    }

    public interface SENSOR_DATA_POINT{
        String TABLE_NAME = "SensorDataPoint";
        String DATA_ID = "dataId";
        String DATA_VALUE = "dataValue";
    }

    // TRIP tables //

    public interface TRIP {
        String TABLE_NAME = "Trip";
        String OLD_ID = "_id";
        String TRIP_ID = "tripId";
        String FK_LOCATION_START_ID = "locationStartId";
        String FK_LOCATION_END_ID = "locationEndId";
        String MILEAGE_START = "mileageStart";
        String MILEAGE_ACCUM = "mileageAccum";
        String FUEL_CONSUMPTION_START = "fuelConsumptionStart";
        String FUEL_CONSUMPTION_ACCUM = "fuelConsumptionAccum";
        String TIME_START = "timeStart";
        String TIME_END = "timeEnd";
        String VIN = "vin";
    }

    public interface LOCATION_START {
        String TABLE_NAME = "LocationStart";
        String ALTITUDE = "altitude";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String START_LOCATION = "startLocation";
        String START_CITY_LOCATION = "startCityLocation";
        String START_STREET_LOCATION = "startStreetLocation";
        String FK_TRIP_ID = "tripId";
        String FK_CAR_VIN = "carVin";
    }

    public interface LOCATION_END {
        String TABLE_NAME = "LocationEnd";
        String ALTITUDE = "altitude";
        String LATITUDE = "latitude";
        String LONGITUDE = "longitude";
        String END_LOCATION = "endLocation";
        String END_CITY_LOCATION = "endCityLocation";
        String END_STREET_LOCATION = "endStreetLocation";
        String FK_TRIP_ID = "tripId";
        String FK_CAR_VIN = "carVin";
    }

    public interface LOCATION_POLYLINE {
        String TABLE_NAME = "LocationPolyline";
        String TIMESTAMP = "phoneTimestamp";
        String FK_TRIP_ID = "tripId";
        String FK_CAR_VIN = "carVin";
    }

    public interface LOCATION {
        String TABLE_NAME = "Location";
        String TYPE_ID = "typeId";
        String DATA = "data";
        String FK_LOCATION_POLYLINE_ID = "locationPolylineId";
        String FK_TRIP_ID = "tripId";
        String FK_CAR_VIN = "carVin";
    }

}
