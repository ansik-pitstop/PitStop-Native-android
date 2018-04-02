package com.pitstop.repositories

import android.util.Log
import com.pitstop.models.snapToRoad.SnappedPoint
import com.pitstop.retrofit.GoogleSnapToRoadApi
import io.reactivex.Observable

/**
 * Created by David C. on 16/3/18.
 */
class SnapToRoadRepository(private val snapToRoadApi: GoogleSnapToRoadApi) : Repository {

    private val tag = javaClass.simpleName

    fun getSnapToRoadFromLocations(listLatLng: String, interpolate: String, apiKey: String): Observable<RepositoryResponse<List<SnappedPoint>>> {

        Log.d(tag, "getSnapToRoadFromLocations() listLatLng: $listLatLng")

        val remoteResponse: Observable<RepositoryResponse<List<SnappedPoint>>> = snapToRoadApi.getSnapToRoadFromLatLng(listLatLng, interpolate, apiKey).map { snappedPointListResponse ->

            return@map RepositoryResponse(snappedPointListResponse.snappedPoints, false)

        }

        return remoteResponse

    }
}