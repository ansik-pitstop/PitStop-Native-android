package com.pitstop.repositories

import android.util.Log
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.retrofit.PitstopSnapToRoadApi
import io.reactivex.Observable

/**
 * Created by David C. on 16/3/18.
 */
class SnapToRoadRepository(private val snapToRoadApi: PitstopSnapToRoadApi) : Repository {

    private val tag = javaClass.simpleName

    fun getSnapToRoadFromLocations(listLatLng: String, interpolate: String): Observable<RepositoryResponse<List<SnappedPoint>>> {

        Log.d(tag, "getSnapToRoadFromLocations() listLatLng: $listLatLng")

        val remoteResponse: Observable<RepositoryResponse<List<SnappedPoint>>> = snapToRoadApi.getSnapToRoadFromLatLng(listLatLng, interpolate, "AIzaSyCD67x7-8vacAhDWMoarx245UKAcvbw5_c").map { snappedPointListResponse ->

            return@map RepositoryResponse(snappedPointListResponse.snappedPoints, false)

        }

        return remoteResponse

    }
}