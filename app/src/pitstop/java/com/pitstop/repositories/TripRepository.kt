package com.pitstop.repositories

import android.util.Log
import com.pitstop.application.Constants
import com.pitstop.database.LocalTripStorage
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
class TripRepository(private val localTripStorage: LocalTripStorage,
                     private val tripApi: PitstopTripApi) : Repository {

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

    fun storeTripData(trip: TripData): Observable<Boolean> {
        Log.d(tag, "storeTripData() trip.size = ${trip.locations.size}")
        val gson = Gson()
        //Todo: Parameters need to be changed, models used in local storage need to
        // be integrated or maybe seperate method made or maybe it should be directly referenced from the use case idk
        localPendingTripStorage.store(trip)
        tripApi.store(gson.toJsonTree(trip))
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe({ next ->
                    Log.d(tag, "next = $next")
                }, { err ->
                    Log.d(tag, "error = ${err.message}")
                })
        return Observable.just(true)
    }
}