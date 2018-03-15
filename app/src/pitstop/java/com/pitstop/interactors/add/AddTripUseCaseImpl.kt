package com.pitstop.interactors.add

import android.location.Geocoder
import android.location.Location
import android.os.Handler
import android.util.Log
import com.pitstop.models.DebugMessage
import com.pitstop.models.Settings
import com.pitstop.models.trip.DataPoint
import com.pitstop.network.RequestError
import com.pitstop.repositories.CarRepository
import com.pitstop.repositories.Repository
import com.pitstop.repositories.TripRepository
import com.pitstop.repositories.UserRepository
import com.pitstop.utils.Logger
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException

/**
 * Created by Karol Zdebel on 3/6/2018.
 */
class AddTripUseCaseImpl(private val geocoder: Geocoder, private val tripRepository: TripRepository
                         ,private val userRepository: UserRepository , private val carRepository: CarRepository
                         , private val useCaseHandler: Handler
                         , private val mainHandler: Handler): AddTripUseCase {

    private val TAG = javaClass.simpleName
    private lateinit var trip: List<Location>
    private lateinit var callback: AddTripUseCase.Callback

    override fun execute(trip: List<Location>, callback: AddTripUseCase.Callback) {
        Logger.getInstance().logI(TAG, "Use case execution started, trip: " + trip, DebugMessage.TYPE_USE_CASE)
        this.trip = trip
        this.callback = callback
        useCaseHandler.post(this)
    }

    override fun run() {
        try{
            val startAddress = geocoder
                    .getFromLocation(trip.first().latitude,trip.first().longitude,1).first()
            val endAddress = geocoder
                    .getFromLocation(trip.last().latitude,trip.last().longitude,1).first()

            val tripDataPoints: MutableList<List<DataPoint>> = arrayListOf()

            userRepository.getCurrentUserSettings(object: Repository.Callback<Settings>{
                override fun onSuccess(data: Settings?) {
                    Log.d(TAG,"got settings with carId: ${data!!.carId}")
                    carRepository.get(data!!.carId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.from(useCaseHandler.looper))
                            .subscribe({ car ->
                                Log.d(TAG,"got car vin: ${car.data!!.vin}")
                                //Add everything but indicator, body of trip
                                trip.forEach({
                                    val tripDataPoint: MutableList<DataPoint> = arrayListOf()
                                    val latitude = DataPoint(DataPoint.ID_LATITUDE, it.latitude.toString())
                                    val longitude = DataPoint(DataPoint.ID_LONGITUDE, it.longitude.toString())
                                    val deviceTimestamp = DataPoint(DataPoint.ID_DEVICE_TIMESTAMP, System.currentTimeMillis().toString())
                                    val tripId = DataPoint(DataPoint.ID_TRIP_ID, trip.first().time.toString())
                                    val vin = DataPoint(DataPoint.ID_VIN, car.data.vin)
                                    val indicator = DataPoint(DataPoint.ID_TRIP_INDICATOR, "false")
                                    tripDataPoint.add(latitude)
                                    tripDataPoint.add(longitude)
                                    tripDataPoint.add(deviceTimestamp)
                                    tripDataPoint.add(tripId)
                                    tripDataPoint.add(vin)
                                    tripDataPoint.add(indicator)
                                    tripDataPoints.add(tripDataPoint)
                                })

                                //Add indicator
                                val indicatorDataPoint: MutableList<DataPoint> = arrayListOf()
                                val startLocation = DataPoint(DataPoint.ID_START_LOCATION, startAddress.getAddressLine(0))
                                val endLocation = DataPoint(DataPoint.ID_END_LOCATION, endAddress.getAddressLine(0))
                                val startStreetLocation = DataPoint(DataPoint.ID_START_STREET_LOCATION, startAddress.getAddressLine(0))
                                val endStreetLocation = DataPoint(DataPoint.ID_END_STREET_LOCATION, endAddress.getAddressLine(0))
                                val startCityLocation = DataPoint(DataPoint.ID_START_CITY_LOCATION, startAddress.locality)
                                val endCityLocation = DataPoint(DataPoint.ID_END_CITY_LOCATION, endAddress.locality)
                                val startLatitude = DataPoint(DataPoint.ID_START_LATITUDE, startAddress.latitude.toString())
                                val endLatitude = DataPoint(DataPoint.ID_END_LATITUDE, endAddress.latitude.toString())
                                val startLongitude = DataPoint(DataPoint.ID_START_LONGTITUDE, startAddress.longitude.toString())
                                val endLongitude = DataPoint(DataPoint.ID_END_LONGITUDE, startAddress.longitude.toString())
                                val mileageTrip = DataPoint(DataPoint.ID_MILEAGE_TRIP, "22.2") //Todo("Add mileage trip logic")
                                val startTimestamp = DataPoint(DataPoint.ID_START_TIMESTAMP, trip.first().time.toString())
                                val endTimestamp = DataPoint(DataPoint.ID_END_TIMESTAMP, trip.last().time.toString())
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
                                tripDataPoints.add(indicatorDataPoint)

                                tripRepository.storeTripData(tripDataPoints)

                            }, { err ->
                                Log.d(TAG, "Error: " + err)
                                onError(RequestError(err))
                            })
                }
                override fun onError(error: RequestError?) {
                    onError(error)
                }
            })

            if (tripRepository.localTripStorage.store(trip) > 0){
                Log.d(TAG,"local trips stored")
                AddTripUseCaseImpl@this.onAddedTrip()
            }else{
                AddTripUseCaseImpl@this.onError(RequestError.getUnknownError())
            }
        }catch(e: IOException){
            e.printStackTrace()
            onError(RequestError.getUnknownError())
        }

    }

    private fun onAddedTrip() {
        Logger.getInstance().logI(TAG, "Use case finished: trip added!", DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onAddedTrip()})
    }

    private fun onError(err: RequestError){
        Logger.getInstance().logE(TAG, "Use case returned error: "+err, DebugMessage.TYPE_USE_CASE)
        mainHandler.post({callback.onError(err)})
    }
}