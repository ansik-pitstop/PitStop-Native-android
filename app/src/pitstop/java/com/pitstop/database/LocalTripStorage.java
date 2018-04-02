package com.pitstop.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.LocationEnd;
import com.pitstop.models.trip.LocationPolyline;
import com.pitstop.models.trip.LocationStart;
import com.pitstop.models.trip.Trip;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David C. on 30/3/18.
 */

public class LocalTripStorage {

    // TRIP table create statement
    public static final String CREATE_TABLE_TRIP = "CREATE TABLE IF NOT EXISTS "
            + TABLES.TRIP.TABLE_NAME + "(" + TABLES.TRIP.TRIP_ID + " TEXT PRIMARY KEY,"
            + TABLES.TRIP.OLD_ID + " INTEGER, "
//            + TABLES.TRIP.FK_LOCATION_START_ID + " INTEGER, "
//            + TABLES.TRIP.FK_LOCATION_END_ID + " INTEGER, "
            + TABLES.TRIP.MILEAGE_START + " REAL, "
            + TABLES.TRIP.MILEAGE_ACCUM + " REAL, "
            + TABLES.TRIP.FUEL_CONSUMPTION_START + " REAL, "
            + TABLES.TRIP.FUEL_CONSUMPTION_ACCUM + " REAL, "
            + TABLES.TRIP.TIME_START + " TEXT, "
            + TABLES.TRIP.TIME_END + " TEXT, "
            + TABLES.TRIP.VIN + " TEXT" + ")";

    // LOCATION_START table create statement
    public static final String CREATE_TABLE_LOCATION_START = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCATION_START.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.LOCATION_START.ALTITUDE + " TEXT, "
            + TABLES.LOCATION_START.LATITUDE + " TEXT, "
            + TABLES.LOCATION_START.LONGITUDE + " TEXT, "
            + TABLES.LOCATION_START.START_LOCATION + " TEXT, "
            + TABLES.LOCATION_START.START_CITY_LOCATION + " TEXT, "
            + TABLES.LOCATION_START.START_STREET_LOCATION + " TEXT, "
            + TABLES.LOCATION_START.FK_TRIP_ID + " TEXT, "
            + TABLES.LOCATION_START.FK_CAR_VIN + " TEXT" + ")";

    // LOCATION_END table create statement
    public static final String CREATE_TABLE_LOCATION_END = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCATION_END.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.LOCATION_END.ALTITUDE + " TEXT, "
            + TABLES.LOCATION_END.LATITUDE + " TEXT, "
            + TABLES.LOCATION_END.LONGITUDE + " TEXT, "
            + TABLES.LOCATION_END.END_LOCATION + " TEXT, "
            + TABLES.LOCATION_END.END_CITY_LOCATION + " TEXT, "
            + TABLES.LOCATION_END.END_STREET_LOCATION + " TEXT, "
            + TABLES.LOCATION_END.FK_TRIP_ID + " TEXT, "
            + TABLES.LOCATION_END.FK_CAR_VIN + " TEXT" + ")";

    // LOCATION_POLYLINE table create statement
    public static final String CREATE_TABLE_LOCATION_POLYLINE = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCATION_POLYLINE.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.LOCATION_POLYLINE.TIMESTAMP + " TEXT, "
            + TABLES.LOCATION_POLYLINE.FK_TRIP_ID + " TEXT, "
            + TABLES.LOCATION_POLYLINE.FK_CAR_VIN + " TEXT" + ")";

    // LOCATION table create statement
    public static final String CREATE_TABLE_LOCATION = "CREATE TABLE IF NOT EXISTS "
            + TABLES.LOCATION.TABLE_NAME + "(" + TABLES.COMMON.KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TABLES.LOCATION.TYPE_ID + " TEXT, "
            + TABLES.LOCATION.DATA + " TEXT, "
            + TABLES.LOCATION.FK_LOCATION_POLYLINE_ID + " INTEGER, "
            + TABLES.LOCATION.FK_TRIP_ID + " TEXT, "
            + TABLES.LOCATION.FK_CAR_VIN + " TEXT" + ")";

    private LocalDatabaseHelper databaseHelper;
    private final String TAG = getClass().getSimpleName();

    public LocalTripStorage(Context context) {
        databaseHelper = LocalDatabaseHelper.getInstance(context);
    }

    public boolean storeTrip(Trip trip) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = tripObjectToContentValues(trip);

        db.insert(TABLES.TRIP.TABLE_NAME, null, values);

        return true;

    }

    public boolean storeLocationStart(LocationStart locationStart) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = locationStartObjectToContentValues(locationStart);

        db.insert(TABLES.LOCATION_START.TABLE_NAME, null, values);

        return true;

    }

    public boolean storeLocationEnd(LocationEnd locationEnd) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = locationEndObjectToContentValues(locationEnd);

        db.insert(TABLES.LOCATION_END.TABLE_NAME, null, values);

        return true;

    }

    public boolean storeLocationPolyline(LocationPolyline locationPolyline) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = locationPolylineObjectToContentValues(locationPolyline);

        db.insert(TABLES.LOCATION_POLYLINE.TABLE_NAME, null, values);

        return true;

    }

    public boolean storeLocation(Location location) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        ContentValues values = locationObjectToContentValues(location);

        db.insert(TABLES.LOCATION.TABLE_NAME, null, values);

        return true;

    }

    public List<Trip> getAllTripsFromCarVin(String carVin) {

        List<Trip> tripList = new ArrayList<>();

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursor = db.query(TABLES.TRIP.TABLE_NAME, null,
                TABLES.TRIP.VIN + "=?", new String[]{String.valueOf(carVin)},
                null, null, null);

        if (cursor.moveToFirst()) {

            while (!cursor.isAfterLast()) {

                Trip trip = cursorToTrip(cursor);

                tripList.add(trip);

                cursor.moveToNext();

            }

        }

        cursor.close();

        return tripList;

    }

    public void deleteAndStoreTripList(List<Trip> tripList) {
        Log.d(TAG, "deleteAndStoreTripList() tripList: " + tripList);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        db.beginTransaction();

        for (Trip trip : tripList) {

            // Delete & Store TRIP
            db.delete(TABLES.TRIP.TABLE_NAME, TABLES.TRIP.TRIP_ID + "=?",
                    new String[]{String.valueOf(trip.getTripId())});
            ContentValues tripValues = tripObjectToContentValues(trip);
            db.insert(TABLES.TRIP.TABLE_NAME, null, tripValues);

            // Delete & Store LOCATION_START
            LocationStart locationStart = trip.getLocationStart();
            locationStart.setTripId(trip.getTripId()); // set FK
            locationStart.setCarVin(trip.getVin()); // set FK
            db.delete(TABLES.LOCATION_START.TABLE_NAME, TABLES.LOCATION_START.FK_TRIP_ID + "=?",
                    new String[]{String.valueOf(trip.getTripId())});
            ContentValues locStartValues = locationStartObjectToContentValues(locationStart);
            db.insert(TABLES.LOCATION_START.TABLE_NAME, null, locStartValues);

            // Delete & Store LOCATION_END
            LocationEnd locationEnd = trip.getLocationEnd();
            locationEnd.setTripId(trip.getTripId()); // set FK
            locationEnd.setCarVin(trip.getVin()); // set FK
            db.delete(TABLES.LOCATION_END.TABLE_NAME, TABLES.LOCATION_END.FK_TRIP_ID + "=?",
                    new String[]{String.valueOf(trip.getTripId())});
            ContentValues locEndValues = locationEndObjectToContentValues(locationEnd);
            db.insert(TABLES.LOCATION_END.TABLE_NAME, null, locEndValues);

            // Delete LOCATION_POLYLINE's
            db.delete(TABLES.LOCATION_POLYLINE.TABLE_NAME, TABLES.LOCATION_POLYLINE.FK_TRIP_ID + "=?",
                    new String[]{String.valueOf(trip.getTripId())});

            // Delete LOCATION's
            db.delete(TABLES.LOCATION.TABLE_NAME, TABLES.LOCATION.FK_TRIP_ID + "=?",
                    new String[]{String.valueOf(trip.getTripId())});

            // Store LOCATION_POLYLINE
            for (LocationPolyline locationPolyline : trip.getLocationPolyline()) {

                locationPolyline.setTripId(trip.getTripId()); // set FK
                locationPolyline.setCarVin(trip.getVin()); // set FK
                ContentValues locPolyValues = locationPolylineObjectToContentValues(locationPolyline);
                long locPolyId = db.insert(TABLES.LOCATION_POLYLINE.TABLE_NAME, null, locPolyValues);

                // Store LOCATION
                for (Location location : locationPolyline.getLocation()) {

                    location.setLocationPolylineId((int) locPolyId); // set FK
                    location.setTripId(trip.getTripId()); // set FK
                    location.setCarVin(trip.getVin()); // set FK

                    ContentValues locationValues = locationObjectToContentValues(location);
                    db.insert(TABLES.LOCATION.TABLE_NAME, null, locationValues);

                }

            }

        }

        db.setTransactionSuccessful();
        db.endTransaction();

    }

    public void deleteTripsFromCarVin(String carVin) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // Delete Trip's
        db.delete(TABLES.TRIP.TABLE_NAME, TABLES.TRIP.VIN + "=?",
                new String[]{carVin});

        // Delete LocationStart's
        db.delete(TABLES.LOCATION_START.TABLE_NAME, TABLES.LOCATION_START.FK_CAR_VIN + "=?", new String[]{carVin});

        // Delete LocationEnd's
        db.delete(TABLES.LOCATION_END.TABLE_NAME, TABLES.LOCATION_END.FK_CAR_VIN + "=?", new String[]{carVin});

        // Delete LocationPolyline's
        db.delete(TABLES.LOCATION_POLYLINE.TABLE_NAME, TABLES.LOCATION_POLYLINE.FK_CAR_VIN + "=?", new String[]{carVin});

        // Delete Location's
        db.delete(TABLES.LOCATION.TABLE_NAME, TABLES.LOCATION.FK_CAR_VIN + "=?", new String[]{carVin});

    }

    public void deleteTripByTripIdAndCarVin(String tripId, String carVin) {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        // Delete Trip
        db.delete(TABLES.TRIP.TABLE_NAME, TABLES.TRIP.TRIP_ID + "=?" + " AND " + TABLES.TRIP.VIN + "=?",
                new String[]{String.valueOf(tripId), carVin});

        // Delete LocationStart
        db.delete(TABLES.LOCATION_START.TABLE_NAME, TABLES.LOCATION_START.FK_TRIP_ID + "=?"
                + " AND " + TABLES.LOCATION_START.FK_CAR_VIN + "=?", new String[]{String.valueOf(tripId), carVin});

        // Delete LocationEnd
        db.delete(TABLES.LOCATION_END.TABLE_NAME, TABLES.LOCATION_END.FK_TRIP_ID + "=?"
                + " AND " + TABLES.LOCATION_END.FK_CAR_VIN + "=?", new String[]{String.valueOf(tripId), carVin});

        // Delete LocationPolyline's
        db.delete(TABLES.LOCATION_POLYLINE.TABLE_NAME, TABLES.LOCATION_POLYLINE.FK_TRIP_ID + "=?"
                + " AND " + TABLES.LOCATION_POLYLINE.FK_CAR_VIN + "=?", new String[]{String.valueOf(tripId), carVin});

        // Delete Location's
        db.delete(TABLES.LOCATION.TABLE_NAME, TABLES.LOCATION.FK_TRIP_ID + "=?"
                + " AND " + TABLES.LOCATION.FK_CAR_VIN + "=?", new String[]{String.valueOf(tripId), carVin});

    }

    public void deleteAllTrips() {

        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        db.delete(TABLES.LOCATION.TABLE_NAME, null, null);
        db.delete(TABLES.LOCATION_POLYLINE.TABLE_NAME, null, null);
        db.delete(TABLES.LOCATION_END.TABLE_NAME, null, null);
        db.delete(TABLES.LOCATION_START.TABLE_NAME, null, null);
        db.delete(TABLES.TRIP.TABLE_NAME, null, null);

    }

    /**
     * Conversion Methods
     */

    private ContentValues tripObjectToContentValues(Trip trip) {
        ContentValues values = new ContentValues();
        values.put(TABLES.TRIP.TRIP_ID, trip.getTripId());
        values.put(TABLES.TRIP.OLD_ID, trip.getOldId());
//        values.put(TABLES.TRIP.FK_LOCATION_START_ID, trip.getLocationStart().getId());
//        values.put(TABLES.TRIP.FK_LOCATION_END_ID, trip.getLocationEnd().getId());
        values.put(TABLES.TRIP.MILEAGE_START, trip.getMileageStart());
        values.put(TABLES.TRIP.MILEAGE_ACCUM, trip.getMileageAccum());
        values.put(TABLES.TRIP.FUEL_CONSUMPTION_START, trip.getFuelConsumptionStart());
        values.put(TABLES.TRIP.FUEL_CONSUMPTION_ACCUM, trip.getFuelConsumptionAccum());
        values.put(TABLES.TRIP.TIME_START, trip.getTimeStart());
        values.put(TABLES.TRIP.TIME_END, trip.getTimeEnd());
        values.put(TABLES.TRIP.VIN, trip.getVin());

        return values;
    }

    private ContentValues locationStartObjectToContentValues(LocationStart locationStart) {
        ContentValues values = new ContentValues();
        //values.put(TABLES.COMMON.KEY_ID, locationStart.getId());
        values.put(TABLES.LOCATION_START.ALTITUDE, locationStart.getAltitude());
        values.put(TABLES.LOCATION_START.LATITUDE, locationStart.getLatitude());
        values.put(TABLES.LOCATION_START.LONGITUDE, locationStart.getLongitude());
        values.put(TABLES.LOCATION_START.START_LOCATION, locationStart.getStartLocation());
        values.put(TABLES.LOCATION_START.START_CITY_LOCATION, locationStart.getStartCityLocation());
        values.put(TABLES.LOCATION_START.START_STREET_LOCATION, locationStart.getStartStreetLocation());
        values.put(TABLES.LOCATION_START.FK_TRIP_ID, locationStart.getTripId());
        values.put(TABLES.LOCATION_START.FK_CAR_VIN, locationStart.getCarVin());

        return values;
    }

    private ContentValues locationEndObjectToContentValues(LocationEnd locationEnd) {
        ContentValues values = new ContentValues();
        //values.put(TABLES.COMMON.KEY_ID, locationEnd.getId());
        values.put(TABLES.LOCATION_END.ALTITUDE, locationEnd.getAltitude());
        values.put(TABLES.LOCATION_END.LATITUDE, locationEnd.getLatitude());
        values.put(TABLES.LOCATION_END.LONGITUDE, locationEnd.getLongitude());
        values.put(TABLES.LOCATION_END.END_LOCATION, locationEnd.getEndLocation());
        values.put(TABLES.LOCATION_END.END_CITY_LOCATION, locationEnd.getEndCityLocation());
        values.put(TABLES.LOCATION_END.END_STREET_LOCATION, locationEnd.getEndStreetLocation());
        values.put(TABLES.LOCATION_END.FK_TRIP_ID, locationEnd.getTripId());
        values.put(TABLES.LOCATION_END.FK_CAR_VIN, locationEnd.getCarVin());

        return values;
    }

    private ContentValues locationPolylineObjectToContentValues(LocationPolyline locationPolyline) {
        ContentValues values = new ContentValues();
        //values.put(TABLES.COMMON.KEY_ID, locationPolyline.getId());
        values.put(TABLES.LOCATION_POLYLINE.TIMESTAMP, locationPolyline.getTimestamp());
        values.put(TABLES.LOCATION_POLYLINE.FK_TRIP_ID, locationPolyline.getTripId());
        values.put(TABLES.LOCATION_POLYLINE.FK_CAR_VIN, locationPolyline.getCarVin());

        return values;
    }

    private ContentValues locationObjectToContentValues(Location location) {
        ContentValues values = new ContentValues();
        //values.put(TABLES.COMMON.KEY_ID, location.getRealId());
        values.put(TABLES.LOCATION.TYPE_ID, location.getTypeId());
        values.put(TABLES.LOCATION.DATA, location.getData());
        values.put(TABLES.LOCATION.FK_LOCATION_POLYLINE_ID, location.getLocationPolylineId());
        values.put(TABLES.LOCATION.FK_TRIP_ID, location.getTripId());
        values.put(TABLES.LOCATION.FK_CAR_VIN, location.getCarVin());

        return values;
    }

    private Trip cursorToTrip(Cursor cursor) {

        Trip trip = new Trip();

        trip.setOldId(cursor.getInt(cursor.getColumnIndex(TABLES.TRIP.OLD_ID)));
        trip.setTripId(cursor.getString(cursor.getColumnIndex(TABLES.TRIP.TRIP_ID)));
        trip.setMileageStart(cursor.getDouble(cursor.getColumnIndex(TABLES.TRIP.MILEAGE_START)));
        trip.setMileageAccum(cursor.getDouble(cursor.getColumnIndex(TABLES.TRIP.MILEAGE_ACCUM)));
        trip.setFuelConsumptionStart(cursor.getDouble(cursor.getColumnIndex(TABLES.TRIP.FUEL_CONSUMPTION_START)));
        trip.setFuelConsumptionAccum(cursor.getDouble(cursor.getColumnIndex(TABLES.TRIP.FUEL_CONSUMPTION_ACCUM)));
        trip.setTimeStart(cursor.getString(cursor.getColumnIndex(TABLES.TRIP.TIME_START)));
        trip.setTimeEnd(cursor.getString(cursor.getColumnIndex(TABLES.TRIP.TIME_END)));
        trip.setVin(cursor.getString(cursor.getColumnIndex(TABLES.TRIP.VIN)));

        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        // Set LocationStart Object
        Cursor cursorLocationStart = db.query(TABLES.LOCATION_START.TABLE_NAME, null,
                TABLES.LOCATION_START.FK_TRIP_ID + "=?", new String[]{String.valueOf(trip.getTripId())},
                null, null, null);
        if (cursorLocationStart.moveToFirst()) {
            LocationStart locationStart = cursorToLocationStart(cursorLocationStart);
            trip.setLocationStart(locationStart);
        }
        cursorLocationStart.close();

        // Set LocationEnd Object
        Cursor cursorLocationEnd = db.query(TABLES.LOCATION_END.TABLE_NAME, null,
                TABLES.LOCATION_END.FK_TRIP_ID + "=?", new String[]{String.valueOf(trip.getTripId())},
                null, null, null);
        if (cursorLocationEnd.moveToFirst()) {
            LocationEnd locationEnd = cursorToLocationEnd(cursorLocationEnd);
            trip.setLocationEnd(locationEnd);
        }
        cursorLocationEnd.close();

        // Set the LocationPolyline List
        Cursor cursorLocationPoly = db.query(TABLES.LOCATION_POLYLINE.TABLE_NAME, null,
                TABLES.LOCATION.FK_TRIP_ID + "=?", new String[]{String.valueOf(trip.getTripId())},
                null, null, null);

        if (cursorLocationPoly.moveToFirst()) {

            List<LocationPolyline> locationPolylineList = new ArrayList<>();

            while (!cursorLocationPoly.isAfterLast()) {

                LocationPolyline locationPolyline = cursorToLocationPolyline(cursorLocationPoly);

                locationPolylineList.add(locationPolyline);

                cursorLocationPoly.moveToNext();

            }

            trip.setLocationPolyline(locationPolylineList);

        }
        cursorLocationPoly.close();

        return trip;

    }

    private LocationStart cursorToLocationStart(Cursor cursor) {

        LocationStart locationStart = new LocationStart();

        locationStart.setId(cursor.getInt(cursor.getColumnIndex(TABLES.COMMON.KEY_ID)));
        locationStart.setAltitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.ALTITUDE)));
        locationStart.setLatitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.LATITUDE)));
        locationStart.setLongitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.LONGITUDE)));
        locationStart.setStartLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.START_LOCATION)));
        locationStart.setStartStreetLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.START_STREET_LOCATION)));
        locationStart.setStartCityLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.START_CITY_LOCATION)));
        locationStart.setTripId(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.FK_TRIP_ID)));
        locationStart.setCarVin(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_START.FK_CAR_VIN)));

        return locationStart;

    }

    private LocationEnd cursorToLocationEnd(Cursor cursor) {

        LocationEnd locationEnd = new LocationEnd();

        locationEnd.setId(cursor.getInt(cursor.getColumnIndex(TABLES.COMMON.KEY_ID)));
        locationEnd.setAltitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.ALTITUDE)));
        locationEnd.setLatitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.LATITUDE)));
        locationEnd.setLongitude(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.LONGITUDE)));
        locationEnd.setEndLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.END_LOCATION)));
        locationEnd.setEndStreetLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.END_STREET_LOCATION)));
        locationEnd.setEndCityLocation(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.END_CITY_LOCATION)));
        locationEnd.setTripId(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.FK_TRIP_ID)));
        locationEnd.setCarVin(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.FK_CAR_VIN)));

        return locationEnd;

    }

    private LocationPolyline cursorToLocationPolyline(Cursor cursor) {

        LocationPolyline locationPolyline = new LocationPolyline();

        locationPolyline.setId(cursor.getInt(cursor.getColumnIndex(TABLES.COMMON.KEY_ID)));
        locationPolyline.setTimestamp(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_POLYLINE.TIMESTAMP)));
        locationPolyline.setTripId(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.FK_TRIP_ID)));
        locationPolyline.setCarVin(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION_END.FK_CAR_VIN)));

        // Set the Location List
        SQLiteDatabase db = databaseHelper.getReadableDatabase();

        Cursor cursorLocation = db.query(TABLES.LOCATION.TABLE_NAME, null,
                TABLES.LOCATION.FK_LOCATION_POLYLINE_ID + "=?", new String[]{String.valueOf(locationPolyline.getId())},
                null, null, null);

        if (cursorLocation.moveToFirst()) {

            List<Location> locationList = new ArrayList<>();

            while (!cursorLocation.isAfterLast()) {

                Location location = cursorToLocation(cursorLocation);

                locationList.add(location);

                cursorLocation.moveToNext();

            }

            locationPolyline.setLocation(locationList);

        }
        cursorLocation.close();

        return locationPolyline;

    }

    private Location cursorToLocation(Cursor cursor) {

        Location location = new Location();

        location.setRealId(cursor.getInt(cursor.getColumnIndex(TABLES.COMMON.KEY_ID)));
        location.setTypeId(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION.TYPE_ID)));
        location.setData(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION.DATA)));
        location.setLocationPolylineId(cursor.getInt(cursor.getColumnIndex(TABLES.LOCATION.FK_LOCATION_POLYLINE_ID)));
        location.setTripId(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION.FK_TRIP_ID)));
        location.setCarVin(cursor.getString(cursor.getColumnIndex(TABLES.LOCATION.FK_CAR_VIN)));

        return location;

    }

}
