package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.models.Dealership;
import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;



/**
 * Created by Matthew on 2017-06-29.
 */

public class GetGooglePlacesShopsUseCaseImpl implements GetGooglePlacesShopsUseCase {
    private final String TAG = getClass().getSimpleName();

    private static final String API_KEY = "AIzaSyAjUxXRoOW21-c-LDudqgOZLvBQpiXp58k";
    private static final String PLACES_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/textsearch/json?";
    private static final String PLACES_TEXT_SEARCH_URL = "https://maps.googleapis.com/maps/api/place/search/json?";
    private static final String PLACES_DETAILS_URL = "https://maps.googleapis.com/maps/api/place/details/json?";

    private NetworkHelper networkHelper;
    private GetGooglePlacesShopsUseCase.CallbackShops callback;
    private Handler useCaseHandler;
    private Handler mainHandler;

    private String query;

    private String latitude;
    private String longitude;


    public GetGooglePlacesShopsUseCaseImpl(NetworkHelper networkHelper
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

    private void onShopsGot(List<Dealership> dealerships){
        Logger.getInstance().logI(TAG, "Use case finished: dealerships="+dealerships
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onShopsGot(dealerships));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void execute(double latitude, double longitude, String query, GetGooglePlacesShopsUseCase.CallbackShops callback) {
        Logger.getInstance().logI(TAG, "Use case execution started: latitude="+latitude+", longitude="+longitude+", query"+query
                , false, DebugMessage.TYPE_USE_CASE);   this.callback = callback;
        this.longitude = Double.toString(longitude);
        this.latitude = Double.toString(latitude);
        this.query = query;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run(){
        String uri = "&query="+query+"&key="+API_KEY+"&type=car_repair|car_dealer&location="+latitude+","+longitude+"&radius=10000";
        networkHelper.getWithCustomUrl(PLACES_SEARCH_URL, uri, (response, requestError) -> {
            if(response != null){
                Log.d(TAG,"response: "+response);
                try {
                    JSONObject responseJson = new JSONObject(response);
                    if(responseJson.getString("status").equals("OK")){
                        JSONArray shops = responseJson.getJSONArray("results");
                        List<Dealership> dealerships = new ArrayList<Dealership>();
                        for(int i = 0 ; i<shops.length() ; i++){
                            JSONObject shop = shops.getJSONObject(i);
                            Dealership dealership = new Dealership();
                            dealership.setCustom(true);
                            dealership.setName(shop.getString("name"));
                            dealership.setAddress(addressFormat(shop.getString("formatted_address")));
                            dealership.setGooglePlaceId(shop.getString("place_id"));
                            if (shop.has("rating"))
                                dealership.setRating(shop.getDouble("rating"));
                            else
                                dealership.setRating(0);
                            dealerships.add(dealership);
                        }
                        GetGooglePlacesShopsUseCaseImpl.this.onShopsGot(dealerships);
                    }else if(responseJson.getString("status").equals("ZERO_RESULTS")){
                        GetGooglePlacesShopsUseCaseImpl.this.onShopsGot(new ArrayList<Dealership>());
                    }else{
                        GetGooglePlacesShopsUseCaseImpl.this.onError(RequestError.getUnknownError());
                    }
                }catch (JSONException e){
                    e.printStackTrace();
                    GetGooglePlacesShopsUseCaseImpl.this.onError(RequestError.getUnknownError());
                }
            }
            else{
                GetGooglePlacesShopsUseCaseImpl.this.onError(requestError);
            }
        });
    }
}
