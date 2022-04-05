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
    private val key = "AIzaSyDW84AecyYE0rvSFHregjW-a0tRE0-nzFU"

    fun getSnapToRoadFromLocations(listLatLng: String): Observable<RepositoryResponse<List<SnappedPoint>>> {

        Log.d(tag, "getSnapToRoadFromLocations() listLatLng: $listLatLng")

        val remoteResponse: Observable<RepositoryResponse<List<SnappedPoint>>> = snapToRoadApi
            .getSnapToRoadFromLatLng(listLatLng, "false", key)
                .doOnNext {
                    print(it)
                }
                .doOnError {
                    print(it)
                }
                .map { snappedPointListResponse ->
                    Log.d(tag,"snapped points response: ${snappedPointListResponse.snappedPoints}")
                    return@map RepositoryResponse(snappedPointListResponse.snappedPoints, false)
                }

        return remoteResponse

    }
}