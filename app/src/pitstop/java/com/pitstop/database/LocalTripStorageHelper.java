package com.pitstop.database;

import android.content.Context;

import com.orm.SugarRecord;
import com.orm.query.Condition;
import com.orm.query.Select;
import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.LocationEnd;
import com.pitstop.models.trip.LocationPolyline;
import com.pitstop.models.trip.LocationStart;
import com.pitstop.models.trip.Trip;

import java.util.List;

/**
 * Created by David C. on 30/3/18.
 */

public class LocalTripStorageHelper {

    private Context context;

    public LocalTripStorageHelper(Context context) {
        this.context = context;
    }

    private void createTrip(Trip trip) {

        // Set tripId to Trip's LocationStart
        if (trip.getLocationStart() != null) {
            trip.getLocationStart().setTripId(trip.getTripId());
            SugarRecord.saveInTx(trip.getLocationStart());
        }

        // Set tripId to Trip's LocationEnd
        if (trip.getLocationEnd() != null) {
            trip.getLocationEnd().setTripId(trip.getTripId());
            SugarRecord.saveInTx(trip.getLocationEnd());
        }

        List<LocationPolyline> locationPolylineList = trip.getLocationPolyline();

        for (LocationPolyline locationPolyline : locationPolylineList) {

            if (locationPolyline == null) {
                return;
            }

            // Set tripId to every Trip's LocationPolyline
            locationPolyline.setTripId(trip.getTripId());

            // Store LocationPolyline
            SugarRecord.saveInTx(locationPolyline);

            List<Location> locationList = locationPolyline.getLocation();

            for (Location location : locationList) {

                // Set locationPolylineId to every LocationPolyline's Location
                if (location != null) {
                    location.setLocationPolylineId(locationPolyline.getId().intValue());
                }

            }

            // Store Location List
            SugarRecord.saveInTx(locationList);
            //locationPolyline.save();

        }

        // Store Trip
        SugarRecord.saveInTx(trip);
        //trip.save();

    }

    public List<Trip> getAllTripsFromCarVin(String carVin) {

        List<Trip> tripList = Select.from(Trip.class).where(Condition.prop("vin").eq(carVin)).list();

        for (Trip trip : tripList) {

            if (trip != null) {

//                // Set LocationStart Object
//                trip.setLocationStart(getLocationStartByTripId(trip.getTripId()));
//                // Set LocationEnd Object
//                trip.setLocationEnd(getLocationEndByTripId(trip.getTripId()));

                // Set LocationPolyline Array
                trip.setLocationPolyline(getAllLocationPolylineByTripId(trip.getTripId()));
            }

        }

        return tripList;

    }

    private Trip getTripByTripIdAndCarVin(String tripId, String carVin) {

        Trip trip = Select.from(Trip.class).where(Condition.prop("trip_id").eq(tripId), Condition.prop("vin").eq(carVin)).first();

        if (trip != null) {
            trip.setLocationPolyline(getAllLocationPolylineByTripId(trip.getTripId()));
        }

        return trip;

    }

    private List<LocationPolyline> getAllLocationPolylineByTripId(String tripId) {

        List<LocationPolyline> locationPolylineList = Select.from(LocationPolyline.class).where(Condition.prop("trip_id").eq(tripId)).list();

        for (LocationPolyline locationPolyline : locationPolylineList) {

            if (locationPolyline != null) {
                locationPolyline.setLocation(getAllLocationByLocationPolylineId(locationPolyline.getId().intValue()));
            }

        }

        return locationPolylineList;

    }

    private List<Location> getAllLocationByLocationPolylineId(int locationPolylineId) {

        return Select.from(Location.class).where(Condition.prop("location_polyline_id").eq(locationPolylineId)).list();

    }

    private LocationStart getLocationStartByTripId(String tripId) {

        return Select.from(LocationStart.class).where(Condition.prop("trip_id").eq(tripId)).first();

    }

    private LocationEnd getLocationEndByTripId(String tripId) {

        return Select.from(LocationEnd.class).where(Condition.prop("trip_id").eq(tripId)).first();

    }

    public void deleteAndStoreTrips(List<Trip> tripList) {

        // Delete trips
        for (Trip trip : tripList) {

            Trip tripObj = getTripByTripIdAndCarVin(trip.getTripId(), trip.getVin());

            if (tripObj != null) {
                deleteTrip(tripObj);
            }

        }

        // Store trips
        for (Trip trip : tripList) {

            if (trip != null) {
                createTrip(trip);
            }

        }

    }

    public void deleteTripsFromCarVin(String carVin) {

        List<Trip> tripList = Select.from(Trip.class).where(Condition.prop("vin").eq(carVin)).list();

        for (Trip trip : tripList) {
            deleteTrip(trip);
        }

    }

    public void deleteTripByTripIdAndCarVin(String tripId, String carVin) {

        Trip trip = getTripByTripIdAndCarVin(tripId, carVin);

        deleteTrip(trip);

    }

    private void deleteTrip(Trip trip) {

        // Delete the corresponding LocationStart
        LocationStart locationStart = Select.from(LocationStart.class).where(Condition.prop("trip_id").eq(trip.getTripId())).first();
        SugarRecord.deleteInTx(locationStart);

        // Delete the corresponding LocationEnd
        LocationEnd locationEnd = Select.from(LocationEnd.class).where(Condition.prop("trip_id").eq(trip.getTripId())).first();
        SugarRecord.deleteInTx(locationEnd);

        // Delete all the corresponding LocationPolyline objects
        List<LocationPolyline> locationPolylineList = Select.from(LocationPolyline.class).where(Condition.prop("trip_id").eq(trip.getTripId())).list();

        for (LocationPolyline locationPolyline : locationPolylineList) {
            deleteLocationPolylineAndItsLocation(locationPolyline);
        }

        // Delete the Trip itself
        SugarRecord.deleteInTx(trip);

    }

    private void deleteLocationPolylineAndItsLocation(LocationPolyline locationPolyline) {

        List<Location> locationList = Select.from(Location.class).where(Condition.prop("location_polyline_id").eq(locationPolyline.getId().intValue())).list();

        // Delete all Location objects from a LocationPolyline
        SugarRecord.deleteInTx(locationList);

        // Delete the LocationPolyline itself
        SugarRecord.deleteInTx(locationPolyline);

    }

    public void deleteAllRows() {

        //SugarRecord.deleteAll(Location.class);
        Location.deleteAll(Location.class);
        LocationStart.deleteAll(LocationStart.class);
        LocationEnd.deleteAll(LocationEnd.class);
        LocationPolyline.deleteAll(LocationPolyline.class);
        Trip.deleteAll(Trip.class);

    }

}
