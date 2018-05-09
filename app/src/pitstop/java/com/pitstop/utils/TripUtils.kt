package com.pitstop.utils

import android.graphics.Color
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.models.trip.LocationPolyline
import com.pitstop.models.trip.RecordedLocation
import com.pitstop.ui.MapView

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripUtils {

    companion object {

        fun polylineToLocationList(polyline: List<LocationPolyline>): List<RecordedLocation>{
            val locList = arrayListOf<RecordedLocation>()
            polyline.forEach {
                locList.add(RecordedLocation(time = it.timestamp.toLong() * 1000
                        , latitude = getLatitudeValue(it), longitude = getLongitudeValue(it),conf = 100))
            }
            return locList
        }

        fun getPolylineDistance(polyline: List<SnappedPoint>): Double{
            return polyline.filterIndexed({ index, _ -> polyline.lastIndex != index})
                    .sumByDouble {
                        val next = polyline[polyline.indexOf(it).plus(1)].location
                        distFrom(it.location.latitude.toDouble()
                                ,it.location.longitude.toDouble()
                                ,next.latitude.toDouble()
                                ,next.longitude.toDouble())/1000
            }
        }

        fun distFrom(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double{
            val earthRadius = 6371000.0 //meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLng = Math.toRadians(lng2 - lng1)
            val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                    Math.sin(dLng / 2) * Math.sin(dLng / 2)
            val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

            return earthRadius * c
        }

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
                    .width(MapView.POLY_WIDTH)
                    .geodesic(true)
                    .color(Color.BLUE)
                    .startCap(RoundCap())
                    .endCap(RoundCap())

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