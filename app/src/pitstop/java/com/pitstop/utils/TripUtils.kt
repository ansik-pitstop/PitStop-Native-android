package com.pitstop.utils

import android.graphics.Color
import android.util.Log
import com.google.android.gms.location.DetectedActivity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.RoundCap
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.models.trip.*
import com.pitstop.ui.MapView

/**
 * Created by Karol Zdebel on 3/7/2018.
 */
class TripUtils {

    companion object {

        val LOW_FOOT_CONF = 40
        val LOW_VEH_CONF = 30
        val HIGH_VEH_CONF = 70
        val HIGH_FOOT_CONF = 90
        val HIGH_STILL_CONF = 99
        val STILL_TIMEOUT = 600000

        fun getCarActivityType(activity: DetectedActivity): Int{
            return when (activity.type){
                DetectedActivity.ON_FOOT -> CarActivity.TYPE_ON_FOOT
                DetectedActivity.IN_VEHICLE -> CarActivity.TYPE_DRIVING
                DetectedActivity.STILL -> CarActivity.TYPE_STILL
                else -> CarActivity.TYPE_OTHER
            }
        }

        //This method is used to calculate the next state of the trip given a set of recent car activities and the current trip state
        fun getNextTripState(currentTripState: TripState, detectedActivities: List<CarActivity>): TripState{
            //Do not let the same detected activity override the time

            detectedActivities.forEach {

                //If in a still state and 10 mins passed return soft end
                if (currentTripState.tripStateType == TripStateType.TRIP_STILL_SOFT
                        && it.time - currentTripState.time > STILL_TIMEOUT){
                    return TripState(TripStateType.TRIP_END_SOFT, System.currentTimeMillis())
                }else if (currentTripState.tripStateType == TripStateType.TRIP_STILL_HARD
                        && it.time - currentTripState.time > STILL_TIMEOUT){
                    return TripState(TripStateType.TRIP_END_HARD, System.currentTimeMillis())
                }

                when (it.type){
                    //user manually ended trip, return this state
                    CarActivity.TYPE_MANUAL_END -> {
                        if (currentTripState.tripStateType == TripStateType.TRIP_MANUAL
                                || currentTripState.tripStateType == TripStateType.TRIP_MANUAL){
                            return TripState(TripStateType.TRIP_MANUAL_END, it.time)
                        }
                    }
                    //user manually started trip, return this state
                    CarActivity.TYPE_MANUAL_START -> {
                        if (currentTripState.tripStateType != TripStateType.TRIP_MANUAL){
                            return TripState(TripStateType.TRIP_MANUAL, it.time)
                        }
                    }
                    //user is still
                    CarActivity.TYPE_STILL -> {
                        if (it.conf >= HIGH_STILL_CONF){
                            if (currentTripState.tripStateType == TripStateType.TRIP_DRIVING_HARD
                                    || currentTripState.tripStateType == TripStateType.TRIP_MANUAL){
                                return TripState(TripStateType.TRIP_STILL_HARD, it.time)
                            }else if (currentTripState.tripStateType == TripStateType.TRIP_DRIVING_SOFT){
                                return TripState(TripStateType.TRIP_STILL_SOFT, it.time)
                            }
                        }
                    }
                    CarActivity.TYPE_DRIVING -> {
                        val walkingActivity = detectedActivities.find { it.type == CarActivity.TYPE_ON_FOOT }
                        //If definitely driving but not already in a driving state(to not override time) return new state
                        if (it.conf > HIGH_VEH_CONF
                                && currentTripState.tripStateType != TripStateType.TRIP_DRIVING_HARD
                                && currentTripState.tripStateType != TripStateType.TRIP_MANUAL){
                            return TripState(TripStateType.TRIP_DRIVING_HARD, it.time)
                        }
                        //If surely not walking and not already driving(to not override time) driving soft state returned
                        else if (it.conf > LOW_VEH_CONF
                                && currentTripState.tripStateType != TripStateType.TRIP_MANUAL
                                && currentTripState.tripStateType != TripStateType.TRIP_DRIVING_HARD
                                && currentTripState.tripStateType != TripStateType.TRIP_DRIVING_SOFT
                                && (walkingActivity == null || walkingActivity.conf < LOW_FOOT_CONF) ){

                            //still hard timer resumes to hard driving after low veh conf, because we are likely back to driving
                            // and we cannot risk the trip ending
                            return if (currentTripState.tripStateType == TripStateType.TRIP_STILL_HARD){
                                TripState(TripStateType.TRIP_DRIVING_HARD, it.time)
                            }
                            //Trip started by driving soft or resumed after soft still state
                            else{
                                TripState(TripStateType.TRIP_DRIVING_SOFT,it.time)
                            }

                        }
                    }
                    CarActivity.TYPE_ON_FOOT -> {
                        //End trip hard only if driving hard or soft
                        if (it.conf > HIGH_FOOT_CONF){
                            if ( currentTripState.tripStateType == TripStateType.TRIP_DRIVING_HARD
                                    || currentTripState.tripStateType == TripStateType.TRIP_DRIVING_SOFT){
                                return TripState(TripStateType.TRIP_END_HARD, it.time)
                            }else if (currentTripState.tripStateType == TripStateType.TRIP_STILL_SOFT){
                                    return TripState(TripStateType.TRIP_NONE, it.time)
                            }
                        }
                    }
                }
            }

            //None state with time of last end
            if (currentTripState.tripStateType == TripStateType.TRIP_END_SOFT
                    || currentTripState.tripStateType == TripStateType.TRIP_END_HARD
                    || currentTripState.tripStateType == TripStateType.TRIP_MANUAL_END){
                return TripState(TripStateType.TRIP_NONE, currentTripState.time)
            }

            return currentTripState
        }

        fun polylineToLocationList(polyline: List<LocationPolyline>): List<RecordedLocation>{
            val locList = arrayListOf<RecordedLocation>()
            polyline.forEach {
                locList.add(RecordedLocation(time = it.timestamp.toLong() * 1000
                        , latitude = getLatitudeValue(it), longitude = getLongitudeValue(it),conf = 100))
            }
            return locList
        }

        //Calculate polyline length
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

        //Calculate distance between two points, used for calculating mileage for a trip
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