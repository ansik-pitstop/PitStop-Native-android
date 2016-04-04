package com.pitstop.DataAccessLayer.DataRetrievers;

/**
 * Created by psola on 3/31/2016.
 */
public final class TABLES {

    interface COMMON {
        //Common column names
        String KEY_ID = "id";
        String KEY_PARSE_ID = "parseId";
        String KEY_CREATED_AT = "createdAt";
    }

    interface PID {
        String TABLE_NAME = "pidData";

        String KEY_DATANUM = "dataNum";
        String KEY_RTCTIME = "rtcTime";
        String KEY_TIMESTAMP = "timestamp";
        String KEY_PIDS = "pids";
    }

    interface CAR {
        String TABLE_NAME = "car";

        String KEY_VIN = "vin";
        String KEY_DEALERSHIP_ID = "shopId";
        String KEY_MILEAGE = "totalMileage";
        String KEY_SCANNER_ID = "scannerId";
        String KEY_MAKE = "make";
        String KEY_MODEL = "model";
        String KEY_YEAR = "year";
        String KEY_ENGINE = "engine";
        String KEY_TRIM = "trimLevel";
        String KEY_OWNER_ID = "ownerId";
        String KEY_NUM_SERVICES = "numberOfServices";
        String KEY_IS_DASHBOARD_CAR = "isDashboardCar";
    }

    interface DEALERSHIP {
        String TABLE_NAME = "dealership";

        String KEY_NAME = "name";
        String KEY_ADDRESS = "address";
        String KEY_PHONE = "phone";
        String KEY_EMAIL = "email";
        String KEY_CAR_ID = "carId";
    }

    interface DTC {
        String TABLE_NAME = "dtc";

        String KEY_CAR_ID = "carId";
        String KEY_CODE = "dtcCode";
        String KEY_DESCRIPTION = "codeDescription";
    }

    interface RECALL {
        String TABLE_NAME = "recall";

        String KEY_CAR_ID = "carId";
        String KEY_NAME = "name";
        String KEY_DESCRIPTION = "description";
        String KEY_REMEDY = "remedy";
        String KEY_RISK = "risk";
        String KEY_EFFECTIVE_DATE = "effectiveDate";
        String KEY_OEM_ID = "oemId";
        String KEY_REIMBURSEMENT = "reimbursement";
        String KEY_STATE = "state";
        String KEY_RISK_RANK = "riskRank";
    }

    interface SERVICE {
        String TABLE_NAME = "service";

        String KEY_CAR_ID = "carId";
        String KEY_TYPE = "serviceType";
        String KEY_DESCRIPTION = "description";
        String KEY_ITEM = "item";
        String KEY_ACTION = "action";
        String KEY_INTERVAL_MONTH = "intervalMonth";
        String KEY_INTERVAL_MILEAGE = "intervalMileage";
        String KEY_INTERVAL_FIXED = "intervalFixed";
        String KEY_PRIORITY = "priority";
        String KEY_DEALERSHIP = "dealership";
    }
}
