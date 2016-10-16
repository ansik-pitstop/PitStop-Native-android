package com.pitstop.database;

/**
 * Created by yifan on 16/10/14.
 */

public final class TABLES {

    public interface COMMON {
        //Common column names
        String KEY_ID = "id";
        String KEY_OBJECT_ID = "parseId";
        String KEY_CREATED_AT = "createdAt";
    }

    public interface PID{
        String TABLE_NAME = "pidArray";

        String KEY_DATANUM = "dataNum";
        String KEY_RTCTIME = "rtcTime";
        String KEY_PIDS = "pids";
    }

}
