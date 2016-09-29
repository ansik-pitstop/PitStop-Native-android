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
    }

    public interface PID {
        String TABLE_NAME = "pidData";
        String TABLE_NAME_RESULT_4 = "pidResult4";

        String KEY_DATANUM = "dataNum";
        String KEY_RTCTIME = "rtcTime";
        String KEY_TIMESTAMP = "timestamp";
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

    public interface CAR_ISSUES {
        String TABLE_NAME = "carIssues";

        String KEY_CAR_ID = "carId";
        String KEY_STATUS = "status";
        String KEY_TIMESTAMP = "timestamp";
        String KEY_PRIORITY = "priority";
        String KEY_ISSUE_TYPE = "issueType";
        String KEY_ITEM = "item";
        String KEY_DESCRIPTION = "description";
        String KEY_ACTION = "action";
    }

    public interface USER {
        String TABLE_NAME = "user";

        String KEY_FIRST_NAME = "firstName";
        String KEY_LAST_NAME = "lastName";
        String KEY_EMAIL = "email";
        String KEY_PHONE = "phone";

    }

    public interface SHOP {
        String TABLE_NAME = "dealership";

        String KEY_NAME = "name";
        String KEY_ADDRESS = "address";
        String KEY_PHONE = "phone";
        String KEY_EMAIL = "email";
        String KEY_SHOP_ID = "shopId";
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

    public interface PRESET_ISSUES {
        String TABLE_NAME = "presetIssues";

        String KEY_CAR_ID = "carId";
        String KEY_TYPE = "type";
        String KEY_ITEM = "item";
        String KEY_ACTION = "action";
        String KEY_DESCRIPTION = "description";
    }

}
