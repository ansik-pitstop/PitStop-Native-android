package com.pitstop.repositories

import android.util.Log
import com.pitstop.models.trip.*
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

    fun getTripsByCarVin(vin: String): Observable<RepositoryResponse<List<Trip>>> {

        Log.d(tag, "getTripsByCarVin() vin: $vin")

        val localResponse = Observable.just(RepositoryResponse(daoSession.tripDao.queryBuilder().where(TripDao.Properties.Vin.eq(vin)).list(), true)).map { next ->

            Log.d(tag, "remote.replay() next: $next")
            Log.d("jakarta", "GETTING LOCAL DATA, ${next.data.orEmpty().size} Trips")
            next

        }

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
                    Log.d(tag, "remote.cache() local store update trips: " + next.data)
                    deleteAndStoreTrips(vin, next.data.orEmpty())
                }).onErrorReturn { error ->
                    Log.d(tag, "getTripByTripId() remote error: $error caused by: ${error.cause}")
                    RepositoryResponse(null, false)
                }
                .subscribe()


        var list: MutableList<Observable<RepositoryResponse<List<Trip>>>> = mutableListOf()
        list.add(localResponse)
        list.add(remoteResponse)

        return Observable.concatDelayError(list)

    }

    fun deleteTripsFromCarVin(carVin: String, callback: Repository.Callback<Any>) {

        // TODO: implement this method

    }

    private fun deleteAndStoreTrips(carVin: String, tripList: List<Trip>) {

        //// Delete previous Trips ////

        // REMOVE
        for (trip in tripList) {

            if (trip.locationPolyline != null) {

                for (locationPolyline in trip.locationPolyline) {

                    val locationDeleteQuery = daoSession.queryBuilder(Location::class.java)
                            .where(LocationDao.Properties.LocationPolylineId.eq(locationPolyline.timestamp))
                            .buildDelete()
                    locationDeleteQuery.executeDeleteWithoutDetachingEntities()

                }

            }

            val locationPolylineDeleteQuery = daoSession.queryBuilder(LocationPolyline::class.java)
                    .where(LocationPolylineDao.Properties.TripId.eq(trip.tripId))
                    .buildDelete()
            locationPolylineDeleteQuery.executeDeleteWithoutDetachingEntities()

        }

        val tripDeleteQuery = daoSession.queryBuilder(Trip::class.java)
                .where(TripDao.Properties.Vin.eq(carVin))
                .buildDelete()
        tripDeleteQuery.executeDeleteWithoutDetachingEntities()

        daoSession.clear()

        // All Car's trips deleted

        // STORE
        for (trip in tripList) {

            if (trip.locationPolyline != null) {

                for (locationPolyline in trip.locationPolyline) {

                    locationPolyline.tripId = trip.tripId

                    if (locationPolyline.location != null) {

                        for (location in locationPolyline.location) {

                            location.locationPolylineId = locationPolyline.timestamp

                            daoSession.insertOrReplace(location)

                        }

                    }

                    daoSession.insertOrReplace(locationPolyline)

                }

            }

            daoSession.insertOrReplace(trip)

        }

    }

    fun delete(tripId: String, vin: String) : Observable<PitstopResponse<String>> {

        Log.d(tag, "delete() tripId: $tripId, vin: $vin")

        val remoteResponse : Observable<PitstopResponse<String>> = tripApi.deleteTripById(tripId, vin)

        val deleteQuery = daoSession.tripDao.queryBuilder().where(TripDao.Properties.TripId.eq(tripId), TripDao.Properties.Vin.eq(vin)).buildDelete()

        val localResponse = Observable.just(deleteQuery.executeDeleteWithoutDetachingEntities()).map { next ->
            return@map "success"
        }

        return remoteResponse//Observable.concat(remoteResponse, localResponse) TODO: pass local?

    }

}