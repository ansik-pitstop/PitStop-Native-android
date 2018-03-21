package com.pitstop.repositories

import android.util.Log
import com.pitstop.application.Constants
import com.pitstop.database.LocalTripStorage
import com.pitstop.models.trip.DataPoint
import com.pitstop.models.trip.TripData
import com.pitstop.models.trip.Trip
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopResponse
import com.pitstop.retrofit.PitstopTripApi
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

/**
 * Created by Karol Zdebel on 3/12/2018.
 */
open class TripRepository(private val tripApi: PitstopTripApi
                          , private val localPendingTripStorage: LocalPendingTripStorage
                          , val localTripStorage: LocalTripStorage
                          , private val connectionObservable: Observable<Boolean>) {

    private val tag = javaClass.simpleName
    private val gson: Gson = Gson()
    private var dumping: Boolean = false
    private val tag = javaClass.simpleName

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

                        if (trip != null && trip.locationPolyline != null) { // seems to be a problem here when trip.locationPolyline == null
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

            val localResponse = Observable.just(localTripStorage.deleteTripByTripIdAndCarVin(tripId, vin)).map { next ->
                return@map "success"
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

        val data: MutableSet<Set<DataPoint>> = mutableSetOf()
        trip.locations.forEach({
            data.add(it.data)
        })
        localPendingTripStorage.store(trip)
        return dumpData()
    }

    //Dumps data from local database to server
    fun dumpData(): Observable<Int>{
        Log.d(tag,"dumpData()")
        val localPendingData = localPendingTripStorage.get()
        Log.d(tag,"dumping ${localPendingData.size} data points")
        if (localPendingData.isEmpty()) return Observable.just(0)

        val observableList = arrayListOf<Observable<Int>>()
        /*Go through each trip and chunk the location data points to not overload the network layer
        * , remove the data from the local storage chunk by chunk depending on if the request
        * succeeded or failed*/
        localPendingData.forEach({
            it.locations.chunked(SIZE_CHUNK).forEach({locationChunk ->
                val tripData: MutableSet<Set<DataPoint>> = mutableSetOf()
                locationChunk.forEach({location ->
                    tripData.add(location.data)
                })
                val remote = tripApi.store(gson.toJsonTree(tripData))
                remote.subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .subscribe({ next ->
                            Log.d(tag, "successfully stored chunk = $next")
                            localPendingTripStorage.delete(locationChunk)
                        }, { err ->
                            Log.d(tag, "error storing chunk = ${err.message}")
                        })
                observableList.add(
                        remote.cache()
                                .map({ SIZE_CHUNK })
                                .onErrorResumeNext { t:Throwable ->
                                    Log.d(tag,"observableList.add().onErrorResumeNext() returning 0")
                                    Observable.just(0)
                                })
            })
        })

        return Observable.zip(observableList, {
            (it as Array<Int>).sumBy { num: Int -> num}
        }).onErrorResumeNext({ t:Throwable ->
            Log.d(tag,"Obervable.zip().onErrorResumeNext() err $t")
            Observable.just(0)
        })
    }
}