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
 * DAO for table "LOCATION".
*/
public class LocationDao extends AbstractDao<Location2, Long> {

    public static final String TABLENAME = "LOCATION";

    /**
     * Properties of entity Location2.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, long.class, "id", true, "_id");
        public final static Property Altitude = new Property(1, double.class, "altitude", false, "ALTITUDE");
        public final static Property Latitude = new Property(2, double.class, "latitude", false, "LATITUDE");
        public final static Property Longitude = new Property(3, double.class, "longitude", false, "LONGITUDE");
        public final static Property Timestamp = new Property(4, long.class, "timestamp", false, "TIMESTAMP");
        public final static Property Trip = new Property(5, Long.class, "trip", false, "TRIP");
    }

    private DaoSession daoSession;


    public LocationDao(DaoConfig config) {
        super(config);
    }
    
    public LocationDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"LOCATION\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ," + // 0: id
                "\"ALTITUDE\" REAL NOT NULL ," + // 1: altitude
                "\"LATITUDE\" REAL NOT NULL ," + // 2: latitude
                "\"LONGITUDE\" REAL NOT NULL ," + // 3: longitude
                "\"TIMESTAMP\" INTEGER NOT NULL ," + // 4: timestamp
                "\"TRIP\" INTEGER);"); // 5: trip
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"LOCATION\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, Location2 entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
        stmt.bindDouble(2, entity.getAltitude());
        stmt.bindDouble(3, entity.getLatitude());
        stmt.bindDouble(4, entity.getLongitude());
        stmt.bindLong(5, entity.getTimestamp());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, Location2 entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
        stmt.bindDouble(2, entity.getAltitude());
        stmt.bindDouble(3, entity.getLatitude());
        stmt.bindDouble(4, entity.getLongitude());
        stmt.bindLong(5, entity.getTimestamp());
    }

    @Override
    protected final void attachEntity(Location2 entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.getLong(offset + 0);
    }    

    @Override
    public Location2 readEntity(Cursor cursor, int offset) {
        Location2 entity = new Location2( //
            cursor.getLong(offset + 0), // id
            cursor.getDouble(offset + 1), // altitude
            cursor.getDouble(offset + 2), // latitude
            cursor.getDouble(offset + 3), // longitude
            cursor.getLong(offset + 4) // timestamp
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, Location2 entity, int offset) {
        entity.setId(cursor.getLong(offset + 0));
        entity.setAltitude(cursor.getDouble(offset + 1));
        entity.setLatitude(cursor.getDouble(offset + 2));
        entity.setLongitude(cursor.getDouble(offset + 3));
        entity.setTimestamp(cursor.getLong(offset + 4));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(Location2 entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(Location2 entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(Location2 entity) {
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
            SqlUtils.appendColumns(builder, "T0", daoSession.getTripDao().getAllColumns());
            builder.append(" FROM LOCATION T");
            builder.append(" LEFT JOIN TRIP T0 ON T.\"TRIP\"=T0.\"_id\"");
            builder.append(' ');
            selectDeep = builder.toString();
        }
        return selectDeep;
    }
    
    protected Location2 loadCurrentDeep(Cursor cursor, boolean lock) {
        Location2 entity = loadCurrent(cursor, 0, lock);
        int offset = getAllColumns().length;

        Trip trip = loadCurrentOther(daoSession.getTripDao(), cursor, offset);
        entity.setTrip(trip);

        return entity;    
    }

    public Location2 loadDeep(Long key) {
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
    public List<Location2> loadAllDeepFromCursor(Cursor cursor) {
        int count = cursor.getCount();
        List<Location2> list = new ArrayList<Location2>(count);
        
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
    
    protected List<Location2> loadDeepAllAndCloseCursor(Cursor cursor) {
        try {
            return loadAllDeepFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }
    

    /** A raw-style query where you can pass any WHERE clause and arguments. */
    public List<Location2> queryDeep(String where, String... selectionArg) {
        Cursor cursor = db.rawQuery(getSelectDeep() + where, selectionArg);
        return loadDeepAllAndCloseCursor(cursor);
    }
 
}
