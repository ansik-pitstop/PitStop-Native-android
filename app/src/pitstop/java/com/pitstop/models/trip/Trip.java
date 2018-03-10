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
public class Trip {

    @Id(autoincrement = true)
    @SerializedName("_id")
    @Expose
    private long id;
    @SerializedName("tripId")
    @Expose
    private String tripId;
    @NotNull
    private long carId;
    @ToOne//(joinProperty = "locationStartId")
    @SerializedName("locationStart")
    @Expose
    private Location locationStart;
    @ToOne//(joinProperty = "locationEndId")
    @SerializedName("locationEnd")
    @Expose
    private Location locationEnd;
    @NotNull
    @SerializedName("mileageStart")
    @Expose
    private double mileageStart;
    @NotNull
    @SerializedName("mileageAccum")
    @Expose
    private double mileageAccum;
    @NotNull
    @SerializedName("fuelConsumptionAccum")
    @Expose
    private double fuelConsumptionAccum;
    @NotNull
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

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1659297594)
    private transient TripDao myDao;
    @Generated(hash = 874799312)
    public Trip(long id, String tripId, long carId, double mileageStart,
            double mileageAccum, double fuelConsumptionAccum,
            double fuelConsumptionStart, String timeStart, String timeEnd,
            String vin) {
        this.id = id;
        this.tripId = tripId;
        this.carId = carId;
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
    public long getId() {
        return this.id;
    }
    public void setId(long id) {
        this.id = id;
    }
    public String getTripId() {
        return this.tripId;
    }
    public void setTripId(String tripId) {
        this.tripId = tripId;
    }
    public long getCarId() {
        return this.carId;
    }
    public void setCarId(long carId) {
        this.carId = carId;
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
    @Generated(hash = 1949177494)
    public Location getLocationStart() {
        if (locationStart != null || !locationStart__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationDao targetDao = daoSession.getLocationDao();
            targetDao.refresh(locationStart);
            locationStart__refreshed = true;
        }
        return locationStart;
    }
    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 357087003)
    public Location peakLocationStart() {
        return locationStart;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1487689477)
    public void setLocationStart(Location locationStart) {
        synchronized (this) {
            this.locationStart = locationStart;
            locationStart__refreshed = true;
        }
    }
    @Generated(hash = 1522237955)
    private transient boolean locationEnd__refreshed;
    /** To-one relationship, resolved on first access. */
    @Generated(hash = 1891548690)
    public Location getLocationEnd() {
        if (locationEnd != null || !locationEnd__refreshed) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationDao targetDao = daoSession.getLocationDao();
            targetDao.refresh(locationEnd);
            locationEnd__refreshed = true;
        }
        return locationEnd;
    }
    /** To-one relationship, returned entity is not refreshed and may carry only the PK property. */
    @Generated(hash = 1315415714)
    public Location peakLocationEnd() {
        return locationEnd;
    }
    /** called by internal mechanisms, do not call yourself. */
    @Generated(hash = 1101978280)
    public void setLocationEnd(Location locationEnd) {
        synchronized (this) {
            this.locationEnd = locationEnd;
            locationEnd__refreshed = true;
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
    @Generated(hash = 414874698)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTripDao() : null;
    }

}
