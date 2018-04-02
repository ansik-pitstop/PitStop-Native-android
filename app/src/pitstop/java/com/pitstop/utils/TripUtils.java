package com.pitstop.utils;

import android.graphics.Color;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.LocationPolyline;

import java.util.List;

/**
 * Created by David C. on 16/3/18.
 */

public class TripUtils {

    public static String locationPolylineToLatLngString(List<LocationPolyline> polylineList) {

        String latLngStringList = "";

        double startLat = -500, startLng = -500, endLat = -500, endLng = -500;

        for (LocationPolyline locationPolyline : polylineList) {

            if (locationPolyline.getLocation().size() > 2) { // First Array containing 4 objects inside (start & end + lat & lng)

                for (Location location : locationPolyline.getLocation()) {

                    double data = Double.parseDouble(location.getData());

                    switch (location.getTypeId()) {
                        case "start_latitude":
                            startLat = data;
                            break;
                        case "start_longitude":
                            startLng = data;
                            break;
                        case "end_latitude":
                            endLat = data;
                            break;
                        case "end_longitude":
                            endLng = data;
                            break;
                    }

                }

            } else if (locationPolyline.getLocation().size() == 2) { // Arrays that will only contain 2 objects (lat and lng)

                double lat = getLatitudeValue(locationPolyline);
                double lng = getLongitudeValue(locationPolyline);

                latLngStringList += lat + "," + lng + "|";

            }

        }

        if (startLat != -500 && startLng != -500) { // Add the Start Location if exists

            latLngStringList = startLat + "," + startLng + "|" + latLngStringList;

        }

        if (endLat != -500 && endLng != -500) { // Add the End Location if exists

            latLngStringList += endLat + "," + endLng;

        }

        if (latLngStringList.length() > 0 && latLngStringList.endsWith("|")) {
            latLngStringList = latLngStringList.substring(0, latLngStringList.length() - 1); // Remove the last "|"
        }

        Log.d("jakarta", latLngStringList);

        return latLngStringList;

    }

    /**
     * Returns the Latitude value inside a LocationPolyline object
     *
     * @param locationPolyline
     * @return
     */
    private static double getLatitudeValue(LocationPolyline locationPolyline) {

        double latitude = 0;

        List<Location> locationList = locationPolyline.getLocation();

        int i = 0;
        boolean found = false;
        while (!found && i < locationList.size()) {

            Location location = locationList.get(i);

            if (location.getTypeId().equalsIgnoreCase("latitude")) {
                latitude = Double.parseDouble(location.getData());
                found = true;
            }

            i++;

        }

        return latitude;

    }

    /**
     * Returns the Longitude value inside a LocationPolyline object
     *
     * @param locationPolyline
     * @return
     */
    private static double getLongitudeValue(LocationPolyline locationPolyline) {

        double longitude = 0;

        List<Location> locationList = locationPolyline.getLocation();

        int i = 0;
        boolean found = false;
        while (!found && i < locationList.size()) {

            Location location = locationList.get(i);

            if (location.getTypeId().equalsIgnoreCase("longitude")) {
                longitude = Double.parseDouble(location.getData());
                found = true;
            }

            i++;

        }

        return longitude;

    }

    public static PolylineOptions snappedPointListToPolylineOptions(List<SnappedPoint> snappedPointList) {

        PolylineOptions polylineOptions = new PolylineOptions()
                .width(4)
                .geodesic(true)
                .color(Color.BLUE);

        for (SnappedPoint snappedPoint : snappedPointList) {

            LatLng latLng = new LatLng(snappedPoint.getLocation().getLatitude(), snappedPoint.getLocation().getLongitude());

            polylineOptions.add(latLng);

        }

        return polylineOptions;

    }
}
