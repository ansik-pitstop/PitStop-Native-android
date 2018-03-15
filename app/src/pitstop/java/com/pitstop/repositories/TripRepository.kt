package com.pitstop.repositories

import android.util.Log
import com.pitstop.models.trip.*
import com.pitstop.retrofit.PitstopTripApi
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers

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
            next//.data
            //.orEmpty()

        }

        val remoteResponse: Observable<RepositoryResponse<List<Trip>>> = tripApi.getTripListFromCarVin(vin).map { tripListResponse ->

            return@map RepositoryResponse(tripListResponse.response, false)

        }

        remoteResponse.subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext({ next ->
                    if (next == null) return@doOnNext
                    Log.d(tag, "remote.cache() local store update trips: " + next.data)
                    deleteAndStoreTrips(vin, next.data.orEmpty())
                }).onErrorReturn { error ->
                    Log.d(tag, "getTripByTripId() remote error: $error caused by: ${error.cause}")
                    RepositoryResponse(null, false)
                }
                .subscribe()

        return Observable.concat(localResponse, remoteResponse.cache())

    }

    private fun deleteAndStoreTrips(carVin: String, tripList: List<Trip>) {

        //// Delete previous Trips ////

        // REMOVE
        for (trip in tripList) {

            for (locationPolyline in trip.locationPolyline) {

                val locationDeleteQuery = daoSession.queryBuilder(Location::class.java)
                        .where(LocationDao.Properties.LocationPolylineId.eq(locationPolyline.timestamp))
                        .buildDelete()
                locationDeleteQuery.executeDeleteWithoutDetachingEntities()

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

            for (locationPolyline in trip.locationPolyline) {

                locationPolyline.tripId = trip.tripId

                for (location in locationPolyline.location) {

                    location.locationPolylineId = locationPolyline.timestamp

                    daoSession.insertOrReplace(location)

                }

                daoSession.insertOrReplace(locationPolyline)

            }

            daoSession.insertOrReplace(trip)

        }

    }

}