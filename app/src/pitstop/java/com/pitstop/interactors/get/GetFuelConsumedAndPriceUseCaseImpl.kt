package com.pitstop.interactors.get

import android.os.Handler
import android.util.Log
import com.pitstop.database.LocalFuelConsumptionStorage
import com.pitstop.network.RequestError
import com.pitstop.repositories.Repository
import com.pitstop.utils.NetworkHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Created by ishan on 2017-11-29.
 */
class GetFuelConsumedAndPriceUseCaseImpl(val useCaseHandler: Handler, val mainHandler: Handler,
                                         val networkHelper: NetworkHelper, val localFuelConsumptionStorage: LocalFuelConsumptionStorage): GetFuelConsumedAndPriceUseCase {
    private val TAG = GetFuelConsumedAndPriceUseCase::class.java.simpleName

    private val BASE_URL = "https://www.gasbuddy.com/home"
    private var scannerID: String? = null;
    private var mCallback: GetFuelConsumedAndPriceUseCase.Callback? = null;
    private var lastKnownLocation: String? = null;




    override fun execute(lastknowLocation: String, scannerId: String, callback: GetFuelConsumedAndPriceUseCase.Callback) {
        this.mCallback = callback;
        this.scannerID = scannerId
        this.lastKnownLocation = lastknowLocation;
        useCaseHandler.post(this);
    }

    override fun run() {
        var Uri:String = "";
        if (lastKnownLocation!=null && lastKnownLocation!!.length >=6){
            lastKnownLocation = lastKnownLocation?.substring(0,4) + lastKnownLocation?.substring(((lastKnownLocation?.length)!!-3), lastKnownLocation?.length!!)
            Uri = "?search=" + lastKnownLocation!!
            Log.d(TAG, Uri);
        }

        try {
            var doc: Document = Jsoup.connect(BASE_URL + Uri).get();
            var docString = doc.text();
            var fuelPrice: Double =109.0 ;
            if (docString == null || docString.equals("", true)) {
               fuelPrice = 109.0;
            }
            else {
                try {
                    val index = docString.indexOf("Average Price ");
                    fuelPrice = (docString.substring(index + 14, index + 19)).toDouble();

                }
                catch (e: NumberFormatException){
                    fuelPrice  = 109.0;
                }
            }
            localFuelConsumptionStorage.getFuelConsumed(this.scannerID, object: Repository.Callback<Double>{
                override fun onSuccess(data: Double?) {
                    mainHandler.post({mCallback?.onGotFuelConsumedAndPrice(fuelPrice, data!!)})

                }

                override fun onError(error: RequestError?) {
                    mainHandler.post({mCallback?.onError(error!!)})
                }
            })
        }
        catch (e: Exception){
            localFuelConsumptionStorage.getFuelConsumed(this.scannerID, object: Repository.Callback<Double>{
                override fun onSuccess(data: Double?) {
                    mainHandler.post({mCallback?.onGotFuelConsumedAndPrice(109.0, data!!)})

                }

                override fun onError(error: RequestError?) {
                    mainHandler.post({mCallback?.onError(error!!)})
                }
            })
        }



    }
}