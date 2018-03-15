package com.pitstop.models.trip;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import org.greenrobot.greendao.DaoException;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.JoinProperty;
import org.greenrobot.greendao.annotation.ToMany;

import java.util.List;

/**
 * Created by David C. on 13/3/18.
 */

@Entity
public class LocationPolyline {

    @Id(autoincrement = true)
    private Long id;
    @SerializedName("location")
    @Expose
    @ToMany(joinProperties = {
            @JoinProperty(name = "timestamp", referencedName = "locationPolylineId")
    })
    private List<Location> location = null;
    @SerializedName("timestamp")
    @Expose
    private String timestamp;
    private String tripId;

    /** Used to resolve relations */
    @Generated(hash = 2040040024)
    private transient DaoSession daoSession;
    /** Used for active entity operations. */
    @Generated(hash = 1444330853)
    private transient LocationPolylineDao myDao;
    @Generated(hash = 144688920)
    public LocationPolyline(Long id, String timestamp, String tripId) {
        this.id = id;
        this.timestamp = timestamp;
        this.tripId = tripId;
    }
    @Generated(hash = 1156315372)
    public LocationPolyline() {
    }
    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getTimestamp() {
        return this.timestamp;
    }
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
    public String getTripId() {
        return this.tripId;
    }
    public void setTripId(String tripId) {
        this.tripId = tripId;
    }
    /**
     * To-many relationship, resolved on first access (and after reset).
     * Changes to to-many relations are not persisted, make changes to the target entity.
     */
    @Generated(hash = 1883164829)
    public List<Location> getLocation() {
        if (location == null) {
            final DaoSession daoSession = this.daoSession;
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            LocationDao targetDao = daoSession.getLocationDao();
            List<Location> locationNew = targetDao
                    ._queryLocationPolyline_Location(timestamp);
            synchronized (this) {
                if (location == null) {
                    location = locationNew;
                }
            }
        }
        return location;
    }
    /** Resets a to-many relationship, making the next get call to query for a fresh result. */
    @Generated(hash = 1873558322)
    public synchronized void resetLocation() {
        location = null;
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
    @Generated(hash = 145294503)
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getLocationPolylineDao() : null;
    }

}
