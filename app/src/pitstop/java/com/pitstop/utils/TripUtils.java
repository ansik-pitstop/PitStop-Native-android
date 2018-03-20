package com.pitstop.utils;

import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.pitstop.models.snapToRoad.SnappedPoint;
import com.pitstop.models.trip.Location;
import com.pitstop.models.trip.LocationPolyline;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by David C. on 16/3/18.
 */

public class TripUtils {

    public static List<LatLng> locationPolylineToLatLngArray(List<LocationPolyline> polylineList) {

        ArrayList<LatLng> latLngArrayList = new ArrayList<>();

        for (LocationPolyline location : polylineList) {

            if (location.getLocation().size() > 2) { // First Array containing 4 objects inside

            } else { // Arrays that will only contain 2 objects

                double lat = 0f;
                double lng = 0f;

                Location obj1 = location.getLocation().get(0);
                if (obj1.getId().equalsIgnoreCase("latitude")) {
                    lat = Double.parseDouble(obj1.getData());
                } else if (obj1.getId().equalsIgnoreCase("longitude")) {
                    lng = Double.parseDouble(obj1.getData());
                }

                Location obj2 = location.getLocation().get(1);
                if (obj2.getId().equalsIgnoreCase("latitude")) {
                    lat = Double.parseDouble(obj2.getData());
                } else if (obj2.getId().equalsIgnoreCase("longitude")) {
                    lng = Double.parseDouble(obj2.getData());
                }

                LatLng latLng = new LatLng(lat, lng);

                latLngArrayList.add(latLng);

            }

        }

        return latLngArrayList;

    }

    public static String locationPolylineToLatLngString(List<LocationPolyline> polylineList) {

        String latLngStringList = "";

        for (LocationPolyline location : polylineList) {

            if (location.getLocation().size() > 2) { // TODO: First Array containing 4 objects inside. Still to assure whether this
                // object will be returned in the Production environment or not

            } else if (location.getLocation().size() == 2) { // Arrays that will only contain 2 objects (lat and lon)

                double lat = 0f;
                double lng = 0f;

                lat = getLatitudeValue(location);
                lng = getLongitudeValue(location);

//                Location obj1 = location.getLocation().get(0);
//                if (obj1.getId().equalsIgnoreCase("latitude")) {
//                    lat = Double.parseDouble(obj1.getData());
//                } else if (obj1.getId().equalsIgnoreCase("longitude")) {
//                    lng = Double.parseDouble(obj1.getData());
//                }
//
//                Location obj2 = location.getLocation().get(1);
//                if (obj2.getId().equalsIgnoreCase("latitude")) {
//                    lat = Double.parseDouble(obj2.getData());
//                } else if (obj2.getId().equalsIgnoreCase("longitude")) {
//                    lng = Double.parseDouble(obj2.getData());
//                }

                latLngStringList += lat + "," + lng + "|";

            }

        }

        if (latLngStringList.length() > 0) {
            latLngStringList = latLngStringList.substring(0, latLngStringList.length() - 1);// Remove the last "|"
        }

        return latLngStringList;

    }

    /**
     * Returns the Latitude value inside a LocationPolyline object
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

            if (location.getId().equalsIgnoreCase("latitude")) {
                latitude = Double.parseDouble(location.getData());
                found = true;
            }

            i++;

        }

        return latitude;

    }

    /**
     * Returns the Longitude value inside a LocationPolyline object
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

            if (location.getId().equalsIgnoreCase("longitude")) {
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
