package com.pitstop.repositories

import android.util.Log
import com.pitstop.application.Constants
import com.pitstop.models.trip.*
import com.pitstop.network.RequestError
import com.pitstop.retrofit.PitstopResponse
import com.pitstop.retrofit.PitstopTripApi
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.greenrobot.greendao.DaoException

/**
 * Created by David C. on 9/3/18.
 */
class TripRepository(private val daoSession: DaoSession,
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

        return Observable.just(RepositoryResponse(daoSession.tripDao.queryBuilder().where(TripDao.Properties.Vin.eq(vin)).list(), true)).map { next ->

            Log.d(tag, "getLocalTripsByCarVin() next: $next")
            Log.d("jakarta", "GETTING LOCAL DATA, ${next.data.orEmpty().size} Trips")
            next

        }

    }

    private fun getRemoteTripsByCarVin(vin: String): Observable<RepositoryResponse<List<Trip>>> {

        val remoteResponse: Observable<RepositoryResponse<List<Trip>>> = tripApi.getTripListFromCarVin(vin).map { tripListResponse ->

            // This logic will ignore those Trips without LocationPolyline content
            var tempList = tripListResponse.response
            val deffList: MutableList<Trip> = mutableListOf()

            tempList.forEach { trip: Trip? ->

                try {
                    if (trip != null && trip.locationPolyline != null) { // seems to be a problem here when trip.locationPolyline == null
                        deffList.add(trip)
                    }
                } catch (ex: DaoException) {
                    ex.stackTrace
                }

            }

            return@map RepositoryResponse(deffList, false)

        }

        remoteResponse.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io(), true)
                .doOnNext({ next ->
                    if (next == null) return@doOnNext
                    Log.d(tag, "getRemoteTripsByCarVin() local store update trips: " + next.data)
                    //insertOrReplaceTripsFromCarVin(vin, next.data.orEmpty())
                    deleteAndStoreTrips(vin, next.data.orEmpty())
                }).onErrorReturn { error ->
                    Log.d(tag, "getRemoteTripsByCarVin() remote error: $error caused by: ${error.cause}")
                    RepositoryResponse(null, false)
                }
                .subscribe()

        return remoteResponse

    }

    fun deleteTripsFromCarVin(carVin: String, callback: Repository.Callback<Any>) {

        var tripList = getLocalTripsByCarVin(carVin)

        tripList.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.computation(), true)
                .subscribe({ next ->
                    Log.d(tag, "tripRepository.deleteTripsFromCarVin() data: $next")
                    this@TripRepository.deleteTrips(carVin, next.data.orEmpty())
                    callback.onSuccess(next)
                }, { error ->
                    Log.d(tag, "tripRepository.onErrorDeleteTripsFromCarVin() error: " + error)
                    //this@GetTripsUseCaseImpl.onError(com.pitstop.network.RequestError(error))
                    callback.onError(RequestError.jsonToRequestErrorObject(error.message))
                })

    }

    private fun insertOrReplaceTripsFromCarVin(carVin: String, tripList: List<Trip>) {

        for (trip in tripList) {

            if (trip.locationPolyline != null) {

                for (locationPolyline in trip.locationPolyline) {

                    locationPolyline.tripId = trip.tripId

                    if (locationPolyline.location != null) {

                        for (location in locationPolyline.location) {

                            location.locationPolylineId = locationPolyline.timestamp

                            daoSession.locationDao.insertOrReplaceInTx(location)
                            //daoSession.insertOrReplace(location)

                        }

                    }

                    daoSession.locationPolylineDao.insertOrReplaceInTx(locationPolyline)
                    //daoSession.insertOrReplace(locationPolyline)

                }

            }

            daoSession.tripDao.insertOrReplaceInTx(trip)
            //daoSession.insertOrReplace(trip)

        }

    }

    private fun deleteTrips(carVin: String, tripList: List<Trip>) {

        // REMOVE
        for (trip in tripList) {

            if (trip.locationPolyline != null) {

                for (locationPolyline in trip.locationPolyline) {

                    val locationDeleteList = daoSession.queryBuilder(Location::class.java)
                            .where(LocationDao.Properties.LocationPolylineId.eq(locationPolyline.timestamp))
                            .list()

                    daoSession.locationDao.deleteInTx(locationDeleteList)

                }

            }

            val locationPolylineDeleteList = daoSession.queryBuilder(LocationPolyline::class.java)
                    .where(LocationPolylineDao.Properties.TripId.eq(trip.tripId))
                    .list()
            daoSession.locationPolylineDao.deleteInTx(locationPolylineDeleteList)

        }

        val tripDeleteList = daoSession.queryBuilder(Trip::class.java)
                .where(TripDao.Properties.Vin.eq(carVin))
                .list()
        daoSession.tripDao.deleteInTx(tripDeleteList)

        daoSession.clear()

    }

    private fun deleteAndStoreTrips(carVin: String, tripList: List<Trip>) {

        //// Delete previous Trips ////

        // REMOVE
        deleteTrips(carVin, tripList)

        // All Car's trips deleted

        // STORE
        insertOrReplaceTripsFromCarVin(carVin, tripList)

    }

    fun delete(tripId: String, vin: String): Observable<PitstopResponse<String>> {

        Log.d(tag, "delete() tripId: $tripId, vin: $vin")

        val remoteResponse: Observable<PitstopResponse<String>> = tripApi.deleteTripById(tripId, vin)

        val deleteQuery = daoSession.tripDao.queryBuilder().where(TripDao.Properties.TripId.eq(tripId), TripDao.Properties.Vin.eq(vin)).buildDelete()

        val localResponse = Observable.just(deleteQuery.executeDeleteWithoutDetachingEntities()).map { next ->
            return@map "success"
        }

        return remoteResponse//Observable.concat(remoteResponse, localResponse) TODO: pass local?

    }

}