package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Matthew on 2017-06-30.
 */

public class GetPlaceDetailsUseCaseImpl implements GetPlaceDetailsUseCase {

    private final String TAG = getClass().getSimpleName();

    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";
    private static final String API_KEY = "AIzaSyAjUxXRoOW21-c-LDudqgOZLvBQpiXp58k";

    private NetworkHelper networkHelper;
    private GetPlaceDetailsUseCase.Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private Dealership dealership;


    public GetPlaceDetailsUseCaseImpl(NetworkHelper networkHelper
            ,Handler useCaseHandler, Handler mainHandler) {
        this.networkHelper = networkHelper;
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
    }

    public String addressFormat(String address){
        try {
            String[] commaSplit = address.split(",");
            String[] spaceSplit = commaSplit[2].split(" ");
            commaSplit[2] = spaceSplit[0] + spaceSplit[1] +", "+spaceSplit[2]+" "+spaceSplit[3];
            address = commaSplit[0] + ", " + commaSplit[1]+", "+commaSplit[2]+", "+commaSplit[3];
        }catch (IndexOutOfBoundsException e){
            return address;
        }
        return address;
    }

    @Override
    public void execute(Dealership dealership, GetPlaceDetailsUseCase.Callback callback) {
        Logger.getInstance().logI(TAG, "Use case started execution"
                , false, DebugMessage.TYPE_USE_CASE);
        this.dealership = dealership;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onDetailsGot(Dealership dealership){
        Logger.getInstance().logI(TAG, "Use case finished execution: dealership="+dealership
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onDetailsGot(dealership));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void run() {
        if(dealership.getGooglePlaceId() != null){
            String uri = "&place_id=" + dealership.getGooglePlaceId()  + "&key=" + API_KEY ;
            networkHelper.getWithCustomUrl(PLACES_DETAILS_URL, uri, new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    try {
                        if (response != null && requestError == null) {
                        JSONObject responseJson = new JSONObject(response);
                        JSONObject results = responseJson.getJSONObject("result");
                        dealership.setPhoneNumber(results.getString("formatted_phone_number"));
                        GetPlaceDetailsUseCaseImpl.this.onDetailsGot(dealership);
                        }else{
                            GetPlaceDetailsUseCaseImpl.this.onError(requestError);
                        }

                    }catch(JSONException e){
                        GetPlaceDetailsUseCaseImpl.this.onError(RequestError.getUnknownError());
                    }

                }
            });
        }else {
            GetPlaceDetailsUseCaseImpl.this.onError(RequestError.getUnknownError());
        }
    }
}
