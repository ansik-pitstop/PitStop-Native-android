package com.pitstop.interactors.get
import android.os.Handler
import android.util.Log
import com.pitstop.network.RequestCallback
import com.pitstop.network.RequestError
import com.pitstop.utils.NetworkHelper
import org.jsoup.Jsoup
import org.jsoup.nodes.Document


/**
 * Created by ishan on 2017-11-16.
 */
class GetFuelPricesUseCaseImpl(val useCaseHandler: Handler, val mainHandler: Handler,
                            val networkHelper: NetworkHelper):GetFuelPricesUseCase {

    private val TAG = GetFuelConsumedUseCaseImpl::class.java.simpleName
    private val BASE_URL = "https://www.gasbuddy.com/home"

  private var mPostalCode:String? = null;
    private var mCallback: GetFuelPricesUseCase.Callback?  = null
    override fun execute(postalCode:String?, callback: GetFuelPricesUseCase.Callback) {
        this.mPostalCode = postalCode;
        this.mCallback = callback;
        useCaseHandler.post(this);

    }

    override fun run() {
        var Uri:String = "";
        if (mPostalCode!=null){
            mPostalCode = mPostalCode?.substring(0,4) + mPostalCode?.substring(((mPostalCode?.length)!!-3), mPostalCode?.length!!)
            Uri = "?search=" + mPostalCode!!
            Log.d(TAG, Uri);
        }

        try{
        val doc: Document = Jsoup.connect(BASE_URL+Uri).get();
        val docString = doc.text();
        if (docString == null || docString.equals("", true)){
            mainHandler?.post({mCallback?.onError(RequestError.getOfflineError())})
            return;
        }
        val index = docString.indexOf("Average Price ");
            val price = (docString.substring(index + 14, index+19)).toDouble();
            mainHandler.post({mCallback?.onFuelPriceGot(price)})
        }

        catch (e: Exception){
            mainHandler?.post({mCallback?.onError(RequestError.getOfflineError())})
        }

    }
}