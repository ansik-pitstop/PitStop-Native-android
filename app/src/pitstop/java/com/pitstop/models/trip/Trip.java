package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.JoinProperty;
import org.greenrobot.greendao.annotation.ToMany;
import org.greenrobot.greendao.annotation.ToOne;

import java.util.List;

/**
 * Created by David C. on 9/3/18.
 */

@Entity
public class Trip {

    @Id
    @SerializedName("_id")
    @Expose
    private Long id;
    @SerializedName("tripId")
    @Expose
    private String tripId;
    @ToOne//(joinProperty = "locationStartId")
    @SerializedName("locationStart")
    @Expose
    private LocationStart locationStart;
    @ToOne//(joinProperty = "locationEndId")
    @SerializedName("locationEnd")
    @Expose
    private LocationEnd locationEnd;
    @SerializedName("mileageStart")
    @Expose
    private double mileageStart;
    @SerializedName("mileageAccum")
    @Expose
    private double mileageAccum;
    @SerializedName("fuelConsumptionAccum")
    @Expose
    private double fuelConsumptionAccum;
    @SerializedName("fuelConsumptionStart")
    @Expose
    private double fuelConsumptionStart;
    @SerializedName("timeStart")
    @Expose
    private String timeStart;
    @SerializedName("timeEnd")
    @Expose
    private String timeEnd;
    @SerializedName("vin")
    @Expose
    private String vin;
    @SerializedName("location_polyline")
    @Expose
    @ToMany(joinProperties = {
            @JoinProperty(name = "tripId", referencedName = "tripId")
    })
    private List<LocationPolyline> locationPolyline = null;
    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1659297594)
    private transient TripDao myDao;
    @Generated(hash = 1568624946)
    public Trip(Long id, String tripId, double mileageStart, double mileageAccum,
            double fuelConsumptionAccum, double fuelConsumptionStart,
            String timeStart, String timeEnd, String vin) {
        this.id = id;
        this.tripId = tripId;
        this.mileageStart = mileageStart;
        this.mileageAccum = mileageAccum;
        this.fuelConsumptionAccum = fuelConsumptionAccum;
        this.fuelConsumptionStart = fuelConsumptionStart;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.vin = vin;
    }
    @Generated(hash = 1047475835)
    public Trip() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTripId() {
        return this.tripId;
    }
    public void setTripId(String tripId) {
        this.tripId = tripId;
    }
    public double getMileageStart() {
        return this.mileageStart;
    }
    public void setMileageStart(double mileageStart) {
        this.mileageStart = mileageStart;
    }
    public double getMileageAccum() {
        return this.mileageAccum;
    }
    public void setMileageAccum(double mileageAccum) {
        this.mileageAccum = mileageAccum;
    }
    public double getFuelConsumptionAccum() {
        return this.fuelConsumptionAccum;
    }
    public void setFuelConsumptionAccum(double fuelConsumptionAccum) {
        this.fuelConsumptionAccum = fuelConsumptionAccum;
    }
    public double getFuelConsumptionStart() {
        return this.fuelConsumptionStart;
    }
    public void setFuelConsumptionStart(double fuelConsumptionStart) {
        this.fuelConsumptionStart = fuelConsumptionStart;
    }
    public String getTimeStart() {
        return this.timeStart;
    }
    public void setTimeStart(String timeStart) {
        this.timeStart = timeStart;
    }
    public String getTimeEnd() {
        return this.timeEnd;
    }
    public void setTimeEnd(String timeEnd) {
        this.timeEnd = timeEnd;
    }
    public String getVin() {
        return this.vin;
    }
    public void setVin(String vin) {
        this.vin = vin;
    }
    @Generated(hash = 457471430)
    private transient boolean locationStart__refreshed;
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 609145131)
    public LocationStart getLocationStart() {
        if (locationStart != null || !locationStart__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationStartDao targetDao = daoSession.getLocationStartDao();
            targetDao.refresh(locationStart);
            locationStart__refreshed = true;
        }
        return locationStart;
    }
    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1785442233)
    public LocationStart peakLocationStart() {
        return locationStart;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1577625810)
    public void setLocationStart(LocationStart locationStart) {
        synchronized (this) {
            this.locationStart = locationStart;
            locationStart__refreshed = true;
        }
    }
    @Generated(hash = 1522237955)
    private transient boolean locationEnd__refreshed;
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 269389826)
    public LocationEnd getLocationEnd() {
        if (locationEnd != null || !locationEnd__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationEndDao targetDao = daoSession.getLocationEndDao();
            targetDao.refresh(locationEnd);
            locationEnd__refreshed = true;
        }
        return locationEnd;
    }
    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 503602238)
    public LocationEnd peakLocationEnd() {
        return locationEnd;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 105818733)
    public void setLocationEnd(LocationEnd locationEnd) {
        synchronized (this) {
            this.locationEnd = locationEnd;
            locationEnd__refreshed = true;
        }
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 222647382)
    public List<LocationPolyline> getLocationPolyline() {
        if (locationPolyline == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationPolylineDao targetDao = daoSession.getLocationPolylineDao();
            List<LocationPolyline> locationPolylineNew = targetDao
                    ._queryTrip_LocationPolyline(tripId);
            synchronized (this) {
                if (locationPolyline == null) {
                    locationPolyline = locationPolylineNew;
                }
            }
        }
        return locationPolyline;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 130258694)
    public synchronized void resetLocationPolyline() {
        locationPolyline = null;
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
    @Generated(hash = 414874698)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTripDao() : null;
    }

}
