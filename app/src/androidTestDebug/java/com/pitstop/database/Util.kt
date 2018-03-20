package com.pitstop.database

import android.location.Location
import com.pitstop.models.trip.DataPoint
import com.pitstop.models.trip.LocationData
import com.pitstop.models.trip.TripData

/**
 * Created by Karol Zdebel on 3/20/2018.
 */
class Util {
    companion object {
        fun locationsToDataPoints(vin: String, trip: Set<Location>): TripData{
            val tripDataPoints: MutableSet<LocationData> = mutableSetOf()

            val vin = DataPoint(DataPoint.ID_VIN, vin)
            val tripId = DataPoint(DataPoint.ID_TRIP_ID, trip.first().time.toString())
            val deviceTimestamp = DataPoint(DataPoint.ID_DEVICE_TIMESTAMP, System.currentTimeMillis().toString())
            //Add everything but indicator, body of trip
            trip.forEach({
                val tripDataPoint: MutableSet<DataPoint> = mutableSetOf()
                val latitude = DataPoint(DataPoint.ID_LATITUDE, it.latitude.toString())
                val longitude = DataPoint(DataPoint.ID_LONGITUDE, it.longitude.toString())
                val indicator = DataPoint(DataPoint.ID_TRIP_INDICATOR, "false")
                tripDataPoint.add(latitude)
                tripDataPoint.add(longitude)
                tripDataPoint.add(deviceTimestamp)
                tripDataPoint.add(tripId)
                tripDataPoint.add(vin)
                tripDataPoint.add(indicator)
                tripDataPoints.add(LocationData(it.time,tripDataPoint))
            })

            //Add indicator
            val indicatorDataPoint: MutableSet<DataPoint> = mutableSetOf()
            val startLocation = DataPoint(DataPoint.ID_START_LOCATION,"start location")
            val endLocation = DataPoint(DataPoint.ID_END_LOCATION,"end location")
            val startStreetLocation = DataPoint(DataPoint.ID_START_STREET_LOCATION,"start street location")
            val endStreetLocation = DataPoint(DataPoint.ID_END_STREET_LOCATION,"end street location")
            val startCityLocation = DataPoint(DataPoint.ID_START_CITY_LOCATION,"start city location")
            val endCityLocation = DataPoint(DataPoint.ID_END_CITY_LOCATION,"end city location")
            val startLatitude = DataPoint(DataPoint.ID_START_LATITUDE,"88.8")
            val endLatitude = DataPoint(DataPoint.ID_END_LATITUDE,"78.9")
            val startLongitude = DataPoint(DataPoint.ID_START_LONGTITUDE, "33.3")
            val endLongitude = DataPoint(DataPoint.ID_END_LONGITUDE, "22.8")
            val mileageTrip = DataPoint(DataPoint.ID_MILEAGE_TRIP, "22.2") //Todo("Add mileage trip logic")
            val startTimestamp = DataPoint(DataPoint.ID_START_TIMESTAMP, trip.first().time.toString())
            val endTimestamp = DataPoint(DataPoint.ID_END_TIMESTAMP, trip.last().time.toString())
            val indicator = DataPoint(DataPoint.ID_TRIP_INDICATOR,"true")
            indicatorDataPoint.add(startLocation)
            indicatorDataPoint.add(endLocation)
            indicatorDataPoint.add(startStreetLocation)
            indicatorDataPoint.add(endStreetLocation)
            indicatorDataPoint.add(startCityLocation)
            indicatorDataPoint.add(endCityLocation)
            indicatorDataPoint.add(startLatitude)
            indicatorDataPoint.add(endLatitude)
            indicatorDataPoint.add(startLongitude)
            indicatorDataPoint.add(endLongitude)
            indicatorDataPoint.add(mileageTrip)
            indicatorDataPoint.add(startTimestamp)
            indicatorDataPoint.add(endTimestamp)
            indicatorDataPoint.add(indicator)
            indicatorDataPoint.add(vin)
            indicatorDataPoint.add(tripId)
            indicatorDataPoint.add(deviceTimestamp)
            tripDataPoints.add(LocationData(trip.last().time*4,indicatorDataPoint))
            return TripData(trip.first().time,tripDataPoints)
        }
    }
}