package com.pitstop.models.trip;

import java.util.List;
import android.database.Cursor;
import android.database.sqlite.SQLiteStatement;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.Property;
import org.greenrobot.greendao.internal.DaoConfig;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseStatement;
import org.greenrobot.greendao.query.Query;
import org.greenrobot.greendao.query.QueryBuilder;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.
/** 
 * DAO for table "LOCATION_POLYLINE".
*/
public class LocationPolylineDao extends AbstractDao<LocationPolyline, Long> {

    public static final String TABLENAME = "LOCATION_POLYLINE";

    /**
     * Properties of entity LocationPolyline.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, long.class, "id", true, "_id");
        public final static Property Timestamp = new Property(1, String.class, "timestamp", false, "TIMESTAMP");
        public final static Property LocationPolylineId = new Property(2, long.class, "locationPolylineId", false, "LOCATION_POLYLINE_ID");
    }

    private DaoSession daoSession;

    private Query<LocationPolyline> trip_LocationPolylineQuery;

    public LocationPolylineDao(DaoConfig config) {
        super(config);
    }
    
    public LocationPolylineDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(Database db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "\"LOCATION_POLYLINE\" (" + //
                "\"_id\" INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL ," + // 0: id
                "\"TIMESTAMP\" TEXT," + // 1: timestamp
                "\"LOCATION_POLYLINE_ID\" INTEGER NOT NULL );"); // 2: locationPolylineId
    }

    /** Drops the underlying database table. */
    public static void dropTable(Database db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "\"LOCATION_POLYLINE\"";
        db.execSQL(sql);
    }

    @Override
    protected final void bindValues(DatabaseStatement stmt, LocationPolyline entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String timestamp = entity.getTimestamp();
        if (timestamp != null) {
            stmt.bindString(2, timestamp);
        }
        stmt.bindLong(3, entity.getLocationPolylineId());
    }

    @Override
    protected final void bindValues(SQLiteStatement stmt, LocationPolyline entity) {
        stmt.clearBindings();
        stmt.bindLong(1, entity.getId());
 
        String timestamp = entity.getTimestamp();
        if (timestamp != null) {
            stmt.bindString(2, timestamp);
        }
        stmt.bindLong(3, entity.getLocationPolylineId());
    }

    @Override
    protected final void attachEntity(LocationPolyline entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.getLong(offset + 0);
    }    

    @Override
    public LocationPolyline readEntity(Cursor cursor, int offset) {
        LocationPolyline entity = new LocationPolyline( //
            cursor.getLong(offset + 0), // id
            cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1), // timestamp
            cursor.getLong(offset + 2) // locationPolylineId
        );
        return entity;
    }
     
    @Override
    public void readEntity(Cursor cursor, LocationPolyline entity, int offset) {
        entity.setId(cursor.getLong(offset + 0));
        entity.setTimestamp(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setLocationPolylineId(cursor.getLong(offset + 2));
     }
    
    @Override
    protected final Long updateKeyAfterInsert(LocationPolyline entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }
    
    @Override
    public Long getKey(LocationPolyline entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    @Override
    public boolean hasKey(LocationPolyline entity) {
        throw new UnsupportedOperationException("Unsupported for entities with a non-null key");
    }

    @Override
    protected final boolean isEntityUpdateable() {
        return true;
    }
    
    /** Internal query to resolve the "locationPolyline" to-many relationship of Trip. */
    public List<LocationPolyline> _queryTrip_LocationPolyline(long locationPolylineId) {
        synchronized (this) {
            if (trip_LocationPolylineQuery == null) {
                QueryBuilder<LocationPolyline> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.LocationPolylineId.eq(null));
                trip_LocationPolylineQuery = queryBuilder.build();
            }
        }
        Query<LocationPolyline> query = trip_LocationPolylineQuery.forCurrentThread();
        query.setParameter(0, locationPolylineId);
        return query.list();
    }

}
