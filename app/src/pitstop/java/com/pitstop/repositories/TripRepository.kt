package com.pitstop.repositories

import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.gson.Gson
import com.pitstop.application.Constants
import com.pitstop.database.LocalPendingTripStorage
import com.pitstop.database.LocalTripStorage
import com.pitstop.models.DebugMessage
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.models.trip.Trip
import com.pitstop.models.trip_k.DataPoint
import com.pitstop.models.trip_k.LocationDataFormatted
import com.pitstop.models.trip_k.TripData
import com.pitstop.network.RequestError
import com.pitstop.retrofit.GoogleSnapToRoadApi
import com.pitstop.retrofit.PitstopResponse
import com.pitstop.retrofit.PitstopTripApi
import com.pitstop.retrofit.SnapToRoadResponse
import com.pitstop.utils.Logger
import com.pitstop.utils.TripUtils
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Created by David C. on 9/3/18.
 */
open class TripRepository(private val tripApi: PitstopTripApi
                          , val localPendingTripStorage: LocalPendingTripStorage
                          , private val localTripStorage: LocalTripStorage
                          , private val snapToRoadApi: GoogleSnapToRoadApi
                          , private val geocoder: Geocoder
                          , private val connectionObservable: Observable<Boolean>) {

    private val tag = javaClass.simpleName
    private val gson: Gson = Gson()
    private var dumping: Boolean = false

    fun getTripsByCarVin(vin: String, whatToReturn: String): Observable<RepositoryResponse<List<Trip>>> {

        Log.d(tag, "getTripsByCarVin() vin: $vin")

//        if (isLocal) {
//            return getLocalTripsByCarVin(vin)
//        } else {
//            return getRemoteTripsByCarVin(vin)
//        }

        when (whatToReturn) {

            Constants.TRIP_REQUEST_LOCAL -> return getLocalTripsByCarVin(vin)

            Constants.TRIP_REQUEST_REMOTE -> return getRemoteTripsByCarVin(vin)

            Constants.TRIP_REQUEST_BOTH -> {
                var list: MutableList<Observable<RepositoryResponse<List<Trip>>>> = mutableListOf()
                list.add(getLocalTripsByCarVin(vin))
                list.add(getRemoteTripsByCarVin(vin))

                return Observable.concatDelayError(list)
            }

        }

        return Observable.empty()

    }

    private fun getLocalTripsByCarVin(vin: String): Observable<RepositoryResponse<List<Trip>>> {

        return Observable.just(RepositoryResponse(localTripStorage.getAllTripsFromCarVin(vin), true)).map { tripList ->

            Log.d(tag, "getLocalTripsByCarVin() next: $tripList")
            Log.d("jakarta", "GETTING LOCAL DATA, ${tripList.data.orEmpty().size} Trips")

            tripList

        }

    }

    private fun getRemoteTripsByCarVin(vin: String): Observable<RepositoryResponse<List<Trip>>> {

        val remoteResponse: Observable<RepositoryResponse<List<Trip>>> = tripApi.getTripListFromCarVin(vin)
                .map { tripListResponse ->

                    // This logic will ignore those Trips without LocationPolyline content
                    var tempList = tripListResponse.response
                    val deffList: MutableList<Trip> = mutableListOf()

                    tempList.forEach { trip: Trip? ->

                        // Make sure at least two points are present in the trip
                        if (trip != null && trip.locationPolyline != null) {
                            deffList.add(trip)
                        }

                    }

                    return@map RepositoryResponse(deffList.orEmpty(), false)

                }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io(), true)
                .doOnNext({ next ->
                    if (next == null) return@doOnNext
                    Log.d(tag, "getRemoteTripsByCarVin() local store update trips: " + next.data)
                    localTripStorage.deleteAndStoreTripList(next.data.orEmpty())
                }).onErrorReturn { error ->
                    Log.d(tag, "getRemoteTripsByCarVin() remote error: $error caused by: ${error.cause}")
                    RepositoryResponse(null, false)
                }

        return remoteResponse

    }

    fun deleteAllTripsFromCarVin(carVin: String, callback: Repository.Callback<Any>) {

        var tripList = getLocalTripsByCarVin(carVin)

        tripList.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation(), true)
                .subscribe({ next ->
                    Log.d(tag, "tripRepository.deleteTripsFromCarVin() data: $next")
                    localTripStorage.deleteTripsFromCarVin(carVin)
                    callback.onSuccess(next)
                }, { error ->
                    Log.d(tag, "tripRepository.onErrorDeleteTripsFromCarVin() error: " + error)
                    //this@TripRepository.onError(com.pitstop.network.RequestError(error))
                    callback.onError(RequestError.jsonToRequestErrorObject(error.message))
                })

    }

    fun deleteTrip(tripId: String, vin: String): Observable<PitstopResponse<String>> {

        Log.d(tag, "delete() tripId: $tripId, vin: $vin")

        val remoteResponse: Observable<PitstopResponse<String>> = tripApi.deleteTripById(tripId, vin)

        remoteResponse.subscribe({ response ->

            Observable.just(localTripStorage.deleteTripByTripIdAndCarVin(tripId, vin)).map { next ->
                return@map "Success"
            }

        }, { error ->

            Log.d(tag, "TRIP delete() remote error: $error caused by: ${error.cause}")
            RepositoryResponse(null, false)

        })

        return remoteResponse//Observable.concat(remoteResponse, localResponse) TODO: pass local?

    }

    private val SIZE_CHUNK = 30

    fun dumpDataOnConnectedToNetwork(){
        //Begins dumping data on connected to internet
        if (dumping) return
        dumping = true
        connectionObservable.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({next ->
                    Log.d(tag,"connectionObservable onNext(): $next")
                    if (next){
                        dumpData().subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribe({next ->
                                    Log.d(tag,"dump data response: "+next)
                                })
                    }
                }, {error ->
                    Log.d(tag,"connectionObservable onError() err: $error")
                })
    }

    //Stores data in server, or local database if fails to upload to server
    //Returns number of location data points uploaded to server
    fun storeTripData(trip: TripData): Observable<Int> {
        Log.d(tag, "storeTripData() trip.size = ${trip.locations.size}")
        return Observable.just(localPendingTripStorage.store(trip).toInt())
    }

    fun storeTripDataAndDump(trip: TripData): Observable<Int>{
        Log.d(tag,"storeTripDataAndDump() trip.size = ${trip.locations.size}")
        localPendingTripStorage.store(trip)
        return dumpData()
    }

    //Returns -1 if no current trip data points have been stored yet, returns trip id otherwise
    fun getIncompleteTripId(): Long = localPendingTripStorage.getIncompleteTripId()

    fun removeIncompleteTripData(): Observable<Int>{
        Log.d(tag, "removeIncompleteTripData()")
        return Observable.just(localPendingTripStorage.deleteIncomplete())
    }

    fun completeTripData(): Observable<Int>{
        Log.d(tag, "completeTripData")
        return Observable.just(localPendingTripStorage.completeAll())
    }

    //Dumps data from local database to server
    fun dumpData(): Observable<Int> {
        Log.d(tag,"dumpData()")
        val localPendingData = localPendingTripStorage.getCompleted(false)
        Log.d(tag,"dumping ${localPendingData.size} data points")
        if (localPendingData.isEmpty()) return Observable.just(0)

        val formattedData = formatTripData(localPendingData) ?: return Observable.just(0)

        //Check for error, likely due to geocoder

        val observableList = arrayListOf<Observable<Int>>()
        /*Go through each trip and chunk the location data points to not overload the network layer
        * , remove the data from the local storage chunk by chunk depending on if the request
        * succeeded or failed*/

        formattedData.forEach({
            it.chunked(SIZE_CHUNK).forEach({ locationChunk ->
                Log.d(tag,"locationChunk: ${locationChunk.size}")
                val tripData: MutableSet<Set<DataPoint>> = mutableSetOf()
                locationChunk.forEach({location ->
                    tripData.add(location.data)
                })
                val remote = tripApi.store(gson.toJsonTree(tripData))
                Log.d(tag,"body: "+gson.toJsonTree(tripData))
                remote.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe({ next ->
                            Log.d(tag, "successfully stored chunk = $next")
                            val rows = localPendingTripStorage.markAsSent(locationChunk)
                            Log.d(tag,"marked $rows as sent after storing chunk")
                        }, { err ->
                            Logger.getInstance().logE(tag, "Error storing chunk = $err"
                                    , DebugMessage.TYPE_REPO)
                        })
                observableList.add(
                        remote.cache()
                                .map({ locationChunk.size })
                                .onErrorResumeNext { t:Throwable ->
                                    Log.d(tag,"observableList.add().onErrorResumeNext() returning 0")
                                    Observable.just(0)
                                })
            })
        })
        return Observable.combineLatest(observableList,{ list ->
            Log.d(tag,"observable.combineLatest()")
            list.sumBy { (it as Int) }
        })
    }

    private fun formatTripData(tripData: List<TripData>): Set<Set<LocationDataFormatted>>?{

        val allTripData = hashSetOf<Set<LocationDataFormatted>>()

        //Go through each trip
        tripData.forEach{

            val snappedPoints = arrayListOf<SnappedPoint>()
            it.locations.chunked(100).forEach{
                //calculate mileage
                var locString = ""
                it.map { loc -> loc.data }.forEach({ loc ->
                    locString += "${loc.latitude},${loc.longitude}"
                    if (it.last().data != loc) locString += "|"
                })
                lateinit var response: Response<SnapToRoadResponse<List<SnappedPoint>>>
                try{
                    response = snapToRoadApi.getSnapToRoadFromLatLngCall(locString,"true"
                            , "AIzaSyCD67x7-8vacAhDWMoarx245UKAcvbw5_c").execute()
                    if (response.isSuccessful){
                        snappedPoints.addAll(response.body()?.snappedPoints ?: emptyList())
                    }
                }catch(e: SocketTimeoutException){
                    e.printStackTrace()
                    return null
                }
            }

            val mileageTrip = if (snappedPoints.isEmpty()) DataPoint(DataPoint.ID_MILEAGE_TRIP, "0")
            else DataPoint(DataPoint.ID_MILEAGE_TRIP
                    , TripUtils.Companion.getPolylineDistance(snappedPoints).toString())

            //Reverse geocode lat and long info
            var startAddress: Address? = null
            var endAddress: Address? = null
            try{
                startAddress = geocoder
                        .getFromLocation(it.locations.first().data.latitude,it.locations.first().data.longitude
                                ,1).firstOrNull()
                endAddress = geocoder
                        .getFromLocation(it.locations.last().data.latitude,it.locations.last().data.longitude
                                ,1).firstOrNull()
                Log.d(tag,"startAddress: $startAddress, endAddress: $endAddress")
            }catch (e: IOException){
                e.printStackTrace()
                Logger.getInstance().logE(tag
                        ,"Unable to reverse geocode due to geocoder service unavailability"
                        ,DebugMessage.TYPE_REPO)
                //Go to next trip if geocoder unavailable
                return@forEach
            }

            //Get car and device info
            val vin = DataPoint(DataPoint.ID_VIN, it.vin)
            val tripId = DataPoint(DataPoint.ID_TRIP_ID, it.id.toString())
            val locationDataSet = hashSetOf<LocationDataFormatted>()

            Log.d(tag,"formatTripData() tripId: $tripId vin: $vin")

            //Store each GPS point from this particular trip
            it.locations
                    .forEach({ locationData ->
                        val deviceTimestamp = DataPoint(DataPoint.ID_DEVICE_TIMESTAMP
                                , locationData.data.time.toString())
                        val tripDataPoint: MutableSet<DataPoint> = mutableSetOf()
                        val latitude = DataPoint(DataPoint.ID_LATITUDE, locationData.data.latitude.toString())
                        val longitude = DataPoint(DataPoint.ID_LONGITUDE, locationData.data.longitude.toString())
                        val indicator = DataPoint(DataPoint.ID_TRIP_INDICATOR, "false")
                        tripDataPoint.add(latitude)
                        tripDataPoint.add(longitude)
                        tripDataPoint.add(deviceTimestamp)
                        tripDataPoint.add(tripId)
                        tripDataPoint.add(vin)
                        tripDataPoint.add(indicator)
                        locationDataSet.add(LocationDataFormatted(locationData.data.time,tripDataPoint))
            })

            //Add indicator data point which marks trip end
            val indicatorDataPoint: MutableSet<DataPoint> = mutableSetOf()
            val startLocation = DataPoint(DataPoint.ID_START_LOCATION
                    , if (startAddress == null || startAddress.locality == null || startAddress.countryCode == null) "Unknown"
                        else "${startAddress.locality} ${startAddress.adminArea}, ${startAddress.countryCode}")
            val endLocation = DataPoint(DataPoint.ID_END_LOCATION
                    , if (endAddress == null || endAddress.locality == null || endAddress.countryCode == null) "Unknown"
                        else "${endAddress.locality} ${endAddress.adminArea}, ${endAddress.countryCode}")
            val startStreetLocation = DataPoint(DataPoint.ID_START_STREET_LOCATION
                    , if (startAddress == null || startAddress.subThoroughfare == null || startAddress.thoroughfare == null) "Unknown"
                        else "${startAddress.subThoroughfare} ${startAddress.thoroughfare}")
            val endStreetLocation = DataPoint(DataPoint.ID_END_STREET_LOCATION
                    , if (endAddress == null || endAddress.subThoroughfare == null || endAddress.thoroughfare == null) "Unknown"
                        else "${endAddress.subThoroughfare} ${endAddress.thoroughfare}")
            val startCityLocation = DataPoint(DataPoint.ID_START_CITY_LOCATION
                    , if (startAddress == null) "Unknown" else startAddress.locality ?: "Unknown")
            val endCityLocation = DataPoint(DataPoint.ID_END_CITY_LOCATION
                    , if (endAddress == null) "Unknown" else  endAddress.locality ?: "Unknown")
            val startLatitude = DataPoint(DataPoint.ID_START_LATITUDE
                    ,it.locations.first().data.latitude.toString())
            val endLatitude = DataPoint(DataPoint.ID_END_LATITUDE
                    , it.locations.last().data.latitude.toString())
            val startLongitude = DataPoint(DataPoint.ID_START_LONGTITUDE, it.locations.first().data.longitude.toString())
            val endLongitude = DataPoint(DataPoint.ID_END_LONGITUDE, it.locations.last().data.longitude.toString())
            val startTimestamp = DataPoint(DataPoint.ID_START_TIMESTAMP, it.locations.first().data.time.toString())
            val endTimestamp = DataPoint(DataPoint.ID_END_TIMESTAMP, it.locations.last().data.time.toString())
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
            indicatorDataPoint.add(DataPoint(DataPoint.ID_DEVICE_TIMESTAMP
                    ,it.locations.last().data.time.toString()))
            locationDataSet.add(LocationDataFormatted(it.locations.first().data.time*4,indicatorDataPoint))

            allTripData.add(locationDataSet)
        }

        return allTripData
    }
}