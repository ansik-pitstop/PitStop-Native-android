package com.pitstop.models.trip;

import java.util.List;
import java.util.ArrayList;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.SqlUtils;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "TRIP".
*/
public class TripDao extends AbstractDao<Trip, Long> {

    public static final String TABLENAME = "TRIP";

    /**
     * Properties of entity Trip.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, long.class, "id", true, "_id");
        public final static Property TripId = new Property(1, String.class, "tripId", false, "TRIP_ID");
        public final static Property MileageStart = new Property(2, double.class, "mileageStart", false, "MILEAGE_START");
        public final static Property MileageAccum = new Property(3, double.class, "mileageAccum", false, "MILEAGE_ACCUM");
        public final static Property FuelConsumptionAccum = new Property(4, double.class, "fuelConsumptionAccum", false, "FUEL_CONSUMPTION_ACCUM");
        public final static Property FuelConsumptionStart = new Property(5, double.class, "fuelConsumptionStart", false, "FUEL_CONSUMPTION_START");
        public final static Property TimeStart = new Property(6, String.class, "timeStart", false, "TIME_START");
        public final static Property TimeEnd = new Property(7, String.class, "timeEnd", false, "TIME_END");
        public final static Property Vin = new Property(8, String.class, "vin", false, "VIN");
        public final static Property LocationStart = new Property(9, Long.class, "locationStart", false, "LOCATION_START");
        public final static Property LocationEnd = new Property(10, Long.class, "locationEnd", false, "LOCATION_END");
    }

    private DaoSession daoSession;


    public TripDao(DaoConfig config) {
        super(config);
    }
    
    public TripDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"TRIP\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ," + // 0: id
                "\"TRIP_ID\" TEXT," + // 1: tripId
                "\"MILEAGE_START\" REAL NOT NULL ," + // 2: mileageStart
                "\"MILEAGE_ACCUM\" REAL NOT NULL ," + // 3: mileageAccum
                "\"FUEL_CONSUMPTION_ACCUM\" REAL NOT NULL ," + // 4: fuelConsumptionAccum
                "\"FUEL_CONSUMPTION_START\" REAL NOT NULL ," + // 5: fuelConsumptionStart
                "\"TIME_START\" TEXT," + // 6: timeStart
                "\"TIME_END\" TEXT," + // 7: timeEnd
                "\"VIN\" TEXT," + // 8: vin
                "\"LOCATION_START\" INTEGER," + // 9: locationStart
                "\"LOCATION_END\" INTEGER);"); // 10: locationEnd
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"TRIP\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Trip entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String tripId = entity.getTripId();
        if (tripId != null) {
            stmt.bindString(2, tripId);
        }
        stmt.bindDouble(3, entity.getMileageStart());
        stmt.bindDouble(4, entity.getMileageAccum());
        stmt.bindDouble(5, entity.getFuelConsumptionAccum());
        stmt.bindDouble(6, entity.getFuelConsumptionStart());
 
        String timeStart = entity.getTimeStart();
        if (timeStart != null) {
            stmt.bindString(7, timeStart);
        }
 
        String timeEnd = entity.getTimeEnd();
        if (timeEnd != null) {
            stmt.bindString(8, timeEnd);
        }
 
        String vin = entity.getVin();
        if (vin != null) {
            stmt.bindString(9, vin);
        }
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Trip entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String tripId = entity.getTripId();
        if (tripId != null) {
            stmt.bindString(2, tripId);
        }
        stmt.bindDouble(3, entity.getMileageStart());
        stmt.bindDouble(4, entity.getMileageAccum());
        stmt.bindDouble(5, entity.getFuelConsumptionAccum());
        stmt.bindDouble(6, entity.getFuelConsumptionStart());
 
        String timeStart = entity.getTimeStart();
        if (timeStart != null) {
            stmt.bindString(7, timeStart);
        }
 
        String timeEnd = entity.getTimeEnd();
        if (timeEnd != null) {
            stmt.bindString(8, timeEnd);
        }
 
        String vin = entity.getVin();
        if (vin != null) {
            stmt.bindString(9, vin);
        }
    }

    @Override
    protected final void attachEntity(Trip entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.getLong(offset + 0);
    }    

    @Override
    public Trip readEntity(Cursor cursor, int offset) {
        Trip entity = new Trip( //
            cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // tripId
            cursor.getDouble(offset + 2), // mileageStart
            cursor.getDouble(offset + 3), // mileageAccum
            cursor.getDouble(offset + 4), // fuelConsumptionAccum
            cursor.getDouble(offset + 5), // fuelConsumptionStart
            cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6), // timeStart
            cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7), // timeEnd
            cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8) // vin
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Trip entity, int offset) {
        entity.setId(cursor.getLong(offset + 0));
        entity.setTripId(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setMileageStart(cursor.getDouble(offset + 2));
        entity.setMileageAccum(cursor.getDouble(offset + 3));
        entity.setFuelConsumptionAccum(cursor.getDouble(offset + 4));
        entity.setFuelConsumptionStart(cursor.getDouble(offset + 5));
        entity.setTimeStart(cursor.isNull(offset + 6) ? null : cursor.getString(offset + 6));
        entity.setTimeEnd(cursor.isNull(offset + 7) ? null : cursor.getString(offset + 7));
        entity.setVin(cursor.isNull(offset + 8) ? null : cursor.getString(offset + 8));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Trip entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Trip entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Trip entity) {
        throw new UnsupportedOperationException("Unsupported for entities with a non-null key");
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
    private String selectDeep;

    protected String getSelectDeep() {
        if (selectDeep == null) {
            StringBuilder builder = new StringBuilder("SELECT ");
            SqlUtils.appendColumns(builder, "T", getAllColumns());
            builder.append(',');
            SqlUtils.appendColumns(builder, "T0", daoSession.getLocationStartDao().getAllColumns());
            builder.append(',');
            SqlUtils.appendColumns(builder, "T1", daoSession.getLocationEndDao().getAllColumns());
            builder.append(" FROM TRIP T");
            builder.append(" LEFT JOIN LOCATION_START T0 ON T.\"LOCATION_START\"=T0.\"_id\"");
            builder.append(" LEFT JOIN LOCATION_END T1 ON T.\"LOCATION_END\"=T1.\"_id\"");
            builder.append(' ');
            selectDeep = builder.toString();
        }
        return selectDeep;
    }
    
    protected Trip loadCurrentDeep(Cursor cursor, boolean lock) {
        Trip entity = loadCurrent(cursor, 0, lock);
        int offset = getAllColumns().length;

        LocationStart locationStart = loadCurrentOther(daoSession.getLocationStartDao(), cursor, offset);
        entity.setLocationStart(locationStart);
        offset += daoSession.getLocationStartDao().getAllColumns().length;

        LocationEnd locationEnd = loadCurrentOther(daoSession.getLocationEndDao(), cursor, offset);
        entity.setLocationEnd(locationEnd);

        return entity;    
    }

    public Trip loadDeep(Long key) {
        assertSinglePk();
        if (key == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(getSelectDeep());
        builder.append("WHERE ");
        SqlUtils.appendColumnsEqValue(builder, "T", getPkColumns());
        String sql = builder.toString();
        
        String[] keyArray = new String[] { key.toString() };
        Cursor cursor = db.rawQuery(sql, keyArray);
        
        try {
            boolean available = cursor.moveToFirst();
            if (!available) {
                return null;
            } else if (!cursor.isLast()) {
                throw new IllegalStateException("Expected unique result, but count was " + cursor.getCount());
            }
            return loadCurrentDeep(cursor, true);
        } finally {
            cursor.close();
        }
    }
    
    /** Reads all available rows from the given cursor and returns a list of new ImageTO objects. */
    public List<Trip> loadAllDeepFromCursor(Cursor cursor) {
        int count = cursor.getCount();
        List<Trip> list = new ArrayList<Trip>(count);
        
        if (cursor.moveToFirst()) {
            if (identityScope != null) {
                identityScope.lock();
                identityScope.reserveRoom(count);
            }
            try {
                do {
                    list.add(loadCurrentDeep(cursor, false));
                } while (cursor.moveToNext());
            } finally {
                if (identityScope != null) {
                    identityScope.unlock();
                }
            }
        }
        return list;
    }
    
    protected List<Trip> loadDeepAllAndCloseCursor(Cursor cursor) {
        try {
            return loadAllDeepFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }
    

    /** A raw-style query where you can pass any WHERE clause and arguments. */
    public List<Trip> queryDeep(String where, String... selectionArg) {
        Cursor cursor = db.rawQuery(getSelectDeep() + where, selectionArg);
        return loadDeepAllAndCloseCursor(cursor);
    }
 
}
