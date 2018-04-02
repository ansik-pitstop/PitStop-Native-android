package com.pitstop.utils

import android.graphics.Color
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.models.trip.LocationPolyline

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripUtils {

    companion object {
        fun locationPolylineToLatLngString(polylineList: List<LocationPolyline>): String {

            var latLngStringList = ""

            var startLat = -500.0
            var startLng = -500.0
            var endLat = -500.0
            var endLng = -500.0

            for (locationPolyline in polylineList) {

                if (locationPolyline.location.size > 2) { // First Array containing 4 objects inside (start & end + lat & lng)

                    for (location in locationPolyline.location) {

                        val data = java.lang.Double.parseDouble(location.data)

                        when (location.typeId) {
                            "start_latitude" -> startLat = data
                            "start_longitude" -> startLng = data
                            "end_latitude" -> endLat = data
                            "end_longitude" -> endLng = data
                        }

                    }

                } else if (locationPolyline.location.size == 2) { // Arrays that will only contain 2 objects (lat and lng)

                    val lat = getLatitudeValue(locationPolyline)
                    val lng = getLongitudeValue(locationPolyline)

                    latLngStringList += lat.toString() + "," + lng + "|"

                }

            }

            if (startLat != -500.0 && startLng != -500.0) { // Add the Start Location if exists

                latLngStringList = startLat.toString() + "," + startLng + "|" + latLngStringList

            }

            if (endLat != -500.0 && endLng != -500.0) { // Add the End Location if exists

                latLngStringList += endLat.toString() + "," + endLng

            }

            if (latLngStringList.length > 0 && latLngStringList.endsWith("|")) {
                latLngStringList = latLngStringList.substring(0, latLngStringList.length - 1) // Remove the last "|"
            }

            Log.d("jakarta", latLngStringList)

            return latLngStringList

        }

        /**
         * Returns the Latitude value inside a LocationPolyline object
         *
         * @param locationPolyline
         * @return
         */
        private fun getLatitudeValue(locationPolyline: LocationPolyline): Double {

            var latitude = 0.0

            val locationList = locationPolyline.location

            var i = 0
            var found = false
            while (!found && i < locationList.size) {

                val location = locationList[i]

                if (location.typeId.equals("latitude", ignoreCase = true)) {
                    latitude = java.lang.Double.parseDouble(location.data)
                    found = true
                }

                i++

            }

            return latitude

        }

        /**
         * Returns the Longitude value inside a LocationPolyline object
         *
         * @param locationPolyline
         * @return
         */
        private fun getLongitudeValue(locationPolyline: LocationPolyline): Double {

            var longitude = 0.0

            val locationList = locationPolyline.location

            var i = 0
            var found = false
            while (!found && i < locationList.size) {

                val location = locationList[i]

                if (location.typeId.equals("longitude", ignoreCase = true)) {
                    longitude = java.lang.Double.parseDouble(location.data)
                    found = true
                }

                i++

            }

            return longitude

        }

        fun snappedPointListToPolylineOptions(snappedPointList: List<SnappedPoint>): PolylineOptions {

            val polylineOptions = PolylineOptions()
                    .width(4f)
                    .geodesic(true)
                    .color(Color.BLUE)

            for (snappedPoint in snappedPointList) {

                val latLng = LatLng(snappedPoint.location.latitude!!.toDouble(), snappedPoint.location.longitude!!.toDouble())

                polylineOptions.add(latLng)

            }

            return polylineOptions

        }

        fun isLocationPriorityValid(priority: Int): Boolean{
            return when (priority){
                LocationRequest.PRIORITY_HIGH_ACCURACY -> true
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> true
                LocationRequest.PRIORITY_LOW_POWER -> true
                LocationRequest.PRIORITY_NO_POWER -> true
                else -> false
            }
        }

        fun locationPriorityToString(priority: Int): String{
            return when (priority){
                LocationRequest.PRIORITY_HIGH_ACCURACY -> "High accuracy"
                LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY -> "Balanced power accuracy"
                LocationRequest.PRIORITY_LOW_POWER -> "Low power"
                LocationRequest.PRIORITY_NO_POWER -> "No power"
                else -> "Unknown"
            }
        }

        fun activityToString(activity: Int): String{
            return when (activity) {
                DetectedActivity.ON_FOOT -> "On foot"
                DetectedActivity.ON_BICYCLE -> "On bicycle"
                DetectedActivity.IN_VEHICLE -> "In vehicle"
                DetectedActivity.RUNNING -> "Running"
                DetectedActivity.STILL -> "Still"
                DetectedActivity.TILTING -> "Tilting"
                DetectedActivity.WALKING -> "Walking"
                DetectedActivity.UNKNOWN -> "Unknown"
                else -> "Unknown"
            }
        }

        fun isActivityValid(activity: Int): Boolean{
            return when (activity) {
                DetectedActivity.ON_FOOT -> true
                DetectedActivity.ON_BICYCLE -> true
                DetectedActivity.IN_VEHICLE -> true
                DetectedActivity.RUNNING -> true
                DetectedActivity.STILL -> true
                DetectedActivity.TILTING -> true
                DetectedActivity.WALKING -> true
                DetectedActivity.UNKNOWN -> true
                else -> false
            }
        }
    }
}