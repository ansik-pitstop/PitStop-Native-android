package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.NotNull;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;

/**
 * Created by David C. on 9/3/18.
 */

@Entity
public class Location2 {

    @Id(autoincrement = true)
    private long id;

    @ToOne(joinProperty = "")
    private Trip trip;

    @SerializedName("altitude")
    @Expose
    private double altitude;
    @NotNull
    @SerializedName("latitude")
    @Expose
    private double latitude;
    @NotNull
    @SerializedName("longitude")
    @Expose
    private double longitude;
    @NotNull
    @SerializedName("timestamp")
    @Expose
    private long timestamp;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1363085965)
    private transient Location2Dao myDao;
    @Generated(hash = 1078733656)
    public Location2(long id, double altitude, double latitude, double longitude,
            long timestamp) {
        this.id = id;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }
    @Generated(hash = 1673084908)
    public Location2() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public double getAltitude() {
        return this.altitude;
    }
    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
    public double getLatitude() {
        return this.latitude;
    }
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }
    public double getLongitude() {
        return this.longitude;
    }
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }
    public long getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    @Generated(hash = 955026413)
    private transient boolean trip__refreshed;
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 865380166)
    public Trip getTrip() {
        if (trip != null || !trip__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            TripDao targetDao = daoSession.getTripDao();
            targetDao.refresh(trip);
            trip__refreshed = true;
        }
        return trip;
    }
    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1643156524)
    public Trip peakTrip() {
        return trip;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1544724637)
    public void setTrip(Trip trip) {
        synchronized (this) {
            this.trip = trip;
            trip__refreshed = true;
        }
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#delete(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 128553479)
    public void delete() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.delete(this);
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#refresh(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 1942392019)
    public void refresh() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.refresh(this);
    }
    /**
     * Convenient call for {@link org.greenrobot.greendao.AbstractDao#update(Object)}.
     * Entity must attached to an entity context.
     */
    @Generated(hash = 713229351)
    public void update() {
        if (myDao == null) {
            throw new DaoException("Entity is detached from DAO context");
        }
        myDao.update(this);
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1969523506)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getLocation2Dao() : null;
    }



}
