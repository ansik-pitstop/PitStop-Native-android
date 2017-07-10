package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.Dealership;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by Matthew on 2017-06-30.
 */

public class GetPlaceDetailsUseCaseImpl implements GetPlaceDetailsUseCase {
    private static final String API_KEY = "AIzaSyAjUxXRoOW21-c-LDudqgOZLvBQpiXp58k";

    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

    private NetworkHelper networkHelper;
    private GetPlaceDetailsUseCase.Callback callback;
    private Handler handler;

    private Dealership dealership;


    public GetPlaceDetailsUseCaseImpl(NetworkHelper networkHelper,Handler handler) {
        this.networkHelper = networkHelper;
        this.handler = handler;
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
        this.dealership = dealership;
        this.callback = callback;
        handler.post(this);
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
                        callback.onDetailsGot(dealership);
                        }else{
                            callback.onError();
                        }

                    }catch(JSONException e){
                        callback.onError();
                    }

                }
            });
        }else {
            callback.onError();
        }
    }
}
