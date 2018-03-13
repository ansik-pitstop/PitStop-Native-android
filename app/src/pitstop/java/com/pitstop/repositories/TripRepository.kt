package com.pitstop.repositories

import android.util.Log
import com.pitstop.models.trip.DaoSession
import com.pitstop.models.trip.Trip
import com.pitstop.models.trip.TripDao
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
                }).onErrorReturn { error ->
                    Log.d(tag, "getTripByTripId() remote error: $error caused by: ${error.cause}")
                    RepositoryResponse(null, false)
                }
                .subscribe()

        return Observable.concat(localResponse, remoteResponse.cache())

    }

    fun getTripByTripId(tripId: Int): Observable<RepositoryResponse<Trip>> {

        Log.d(tag, "getTripByTripId() userId: $tripId")

        val localResponse = Observable.just(RepositoryResponse(daoSession.tripDao.queryBuilder().where(TripDao.Properties.Id.eq(tripId)).unique(), true))
        val remoteResponse = tripApi.getTrip(tripId)

        remoteResponse.map { tripListResponse -> RepositoryResponse(tripListResponse, false) }
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .doOnNext({ next ->
                    if (next.data == null) return@doOnNext
                }).onErrorReturn { error ->
                    Log.d(tag, "getTripByTripId() remote error: $error")
                    RepositoryResponse(null, false)
                }
                .subscribe()

        val returnRemote = remoteResponse.cache().map { pitstopResponse ->
            val tripList = pitstopResponse
            RepositoryResponse(tripList, false)
        }

        return Observable.concat(localResponse, localResponse) // TODO: change the 2nd concat param

    }

//    fun getTripPolyline(tripId: Int): Observable<RepositoryResponse<List<Location2>>> {
//
//        var daoSession = GlobalApplication().daoSession
//
//        Log.d(tag,"getTripsByCarId() userId: $tripId")
//
//        val desiredTrip = daoSession.tripDao.queryBuilder().where(TripDao.Properties.Id.eq(tripId))
//
//        val localResponse = Observable.just(RepositoryResponse(daoSession.locationDao.queryBuilder().where(LocationDao.Properties.Trip.eq(desiredTrip)).list(), true)).map { next ->
//
//            Log.d(tag,"remote.replay() next: $next")
//            next//.data
//            //.orEmpty()
//
//        }
//
//        val remoteResponse: Observable<RepositoryResponse<List<Location2>>> = tripApi.getTripPolyline(tripId).map { locationListResponse ->
//            return@map RepositoryResponse(locationListResponse, false)
//        }
//
//        remoteResponse.subscribeOn(Schedulers.io())
//                .observeOn(Schedulers.io())
//                .doOnNext({ next ->
//                    if (next == null) return@doOnNext
//                }).onErrorReturn { error ->
//                    Log.d(tag, "getTripByTripId() remote error: $error caused by: ${error.cause}")
//                    RepositoryResponse(null, false)
//                }
//                .subscribe()
//
//        return Observable.concat(localResponse, remoteResponse.cache())
//
//    }

}