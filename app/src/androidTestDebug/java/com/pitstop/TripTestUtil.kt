package com.pitstop

import com.pitstop.models.sensor_data.DataPoint
import com.pitstop.models.sensor_data.trip.LocationData
import com.pitstop.models.sensor_data.trip.LocationDataFormatted
import com.pitstop.models.sensor_data.trip.PendingLocation
import com.pitstop.models.sensor_data.trip.TripData
import com.pitstop.models.trip.CarActivity
import com.pitstop.models.trip.CarLocation
import com.pitstop.models.trip.RecordedLocation
import java.util.*

/**
 * Created by Karol Zdebel on 3/20/2018.
 */
class TripTestUtil {
    companion object {

        val TAG = javaClass.simpleName


        fun getRandomRoute(num: Int, maxConf: Int): List<RecordedLocation> {

            val route = arrayListOf<RecordedLocation>()
            var prevLat = 52.2440835
            var prevLng = 21.079464
            val random = Random()
            route.add(RecordedLocation(time = System.currentTimeMillis()
                    , longitude = prevLng
                    , latitude = prevLat
                    , conf = (random.nextDouble()*maxConf).toInt()))
            for (i in 2..num){
                val lng = prevLng + (random.nextDouble())/100
                val lat = prevLat + (random.nextDouble())/100
                Thread.sleep(100)
                route.add(RecordedLocation(time = System.currentTimeMillis()
                        , longitude = lng
                        , latitude = lat
                        , conf = (random.nextDouble()*maxConf).toInt()))
            }
            return route
        }

        fun getRandomCarActivity(vin: String, timeOffsetIndex: Int): CarActivity {
            val r = Random()
            return CarActivity(vin, System.currentTimeMillis() + 1000*timeOffsetIndex
                    , (r.nextDouble()*8).toInt(), (r.nextDouble()*100).toInt())
        }

        fun getRandomCarLocation(vin: String, timeOffsetIndex: Int): CarLocation {
            val r = Random()
            return CarLocation(vin, (1000*timeOffsetIndex).toLong()
                    , r.nextDouble() * 90, r.nextDouble() * 90)
        }

        fun getRandomLocation(): RecordedLocation {
            val r = Random()
            return RecordedLocation(time = System.currentTimeMillis() - Math.abs(r.nextInt()) * 1000
                    , longitude = r.nextDouble() * 90
                    , latitude = r.nextDouble() * 90
                    , conf = 100)
        }

        fun generateTripData(locNum: Int, inVin:String, deviceTimestampIn: Long): TripData {
            val trip: MutableSet<LocationData> = hashSetOf()

            for (i in 1..locNum){
                val loc = getRandomLocation()
                trip.add(LocationData(loc.time/10000, PendingLocation(loc.longitude,loc.latitude,loc.time/1000)))
            }

            return TripData(trip.first().id,inVin,trip,(trip.first().data.time/1000).toInt()
                    ,(trip.last().data.time/1000).toInt())
        }

        //This will actually return a TripData object with locNum+1 locations since trip indicator data point is added
        fun generateTripDataFormtted(locNum: Int, inVin: String, deviceTimestampIn: Long): Set<LocationDataFormatted>{

            val trip = hashSetOf<RecordedLocation>()
            for (i in 1..locNum){
                trip.add(getRandomLocation())
            }

            val tripDataPoints: MutableSet<LocationDataFormatted> = mutableSetOf()

            val vin = DataPoint(DataPoint.ID_VIN, inVin)
            val tripId = DataPoint(DataPoint.ID_TRIP_ID, trip.first().time.toString())
            val deviceTimestamp = DataPoint(DataPoint.ID_DEVICE_TIMESTAMP, deviceTimestampIn.toString())
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
                tripDataPoints.add(LocationDataFormatted(it.time,tripDataPoint))
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
            tripDataPoints.add(LocationDataFormatted(trip.last().time*4,indicatorDataPoint))

            return tripDataPoints
        }

    }
}