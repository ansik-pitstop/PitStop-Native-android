package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.ToOne;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.DaoException;

/**
 * Created by David C. on 9/3/18.
 *
 * Temporal. Should be replaced by Location2 object
 */

@Entity
public class LocationEnd {

    @Id(autoincrement = true)
    private long id;

    @ToOne(joinProperty = "")
    private Trip trip;

    @SerializedName("altitude")
    @Expose
    private String altitude;
    @SerializedName("latitude")
    @Expose
    private String latitude;
    @SerializedName("longitude")
    @Expose
    private String longitude;
    @SerializedName("endLocation")
    @Expose
    private String endLocation;
    @SerializedName("endCityLocation")
    @Expose
    private String endCityLocation;
    @SerializedName("endStreetLocation")
    @Expose
    private String endStreetLocation;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;

    /** Used for active entity operations. */
    @Generated(hash = 1801346410)
    private transient LocationEndDao myDao;
    @Generated(hash = 257631893)
    public LocationEnd(long id, String altitude, String latitude, String longitude,
            String endLocation, String endCityLocation, String endStreetLocation) {
        this.id = id;
        this.altitude = altitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.endLocation = endLocation;
        this.endCityLocation = endCityLocation;
        this.endStreetLocation = endStreetLocation;
    }
    @Generated(hash = 778354779)
    public LocationEnd() {
    }
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getAltitude() {
        return this.altitude;
    }
    public void setAltitude(String altitude) {
        this.altitude = altitude;
    }
    public String getLatitude() {
        return this.latitude;
    }
    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }
    public String getLongitude() {
        return this.longitude;
    }
    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }
    public String getEndLocation() {
        return this.endLocation;
    }
    public void setEndLocation(String endLocation) {
        this.endLocation = endLocation;
    }
    public String getEndCityLocation() {
        return this.endCityLocation;
    }
    public void setEndCityLocation(String endCityLocation) {
        this.endCityLocation = endCityLocation;
    }
    public String getEndStreetLocation() {
        return this.endStreetLocation;
    }
    public void setEndStreetLocation(String endStreetLocation) {
        this.endStreetLocation = endStreetLocation;
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
    @Generated(hash = 1695338602)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getLocationEndDao() : null;
    }

}
