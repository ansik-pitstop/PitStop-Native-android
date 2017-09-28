package com.pitstop.interactors.get;

import android.os.Handler;
import android.util.Log;

import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.repositories.Repository;
import com.pitstop.repositories.UserRepository;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ishan on 2017-09-28.
 */

public class GetCarStyleIDUSeCaseImpl implements GetCarStyleIDUseCase {

    private static final String TAG = GetCarStyleIDUSeCaseImpl.class.getSimpleName();
    private static final String BASE_URL = "https://api.edmunds.com/api/vehicle/v2/vins/";
    private static final String EDMUNDS_API_KEY = "9mu2f8rw93jaxtsj9dqkbtsx";
    private static final String URI_FORMAT = "?&fmt=json&api_key=";



    private Handler useCaseHandler;
    private Handler mainHandler;
    private String vin;
    NetworkHelper networkHelper;

    private GetCarStyleIDUseCase.Callback callback;

    public GetCarStyleIDUSeCaseImpl(Handler useCaseHandler, Handler mainHandler,
                                    NetworkHelper networkHelper) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    @Override
    public void execute(String VIN, Callback callback) {
        this.vin = VIN;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    private void onStylesIDGot(String stylesID){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onStyleIDGot(stylesID);
            }
        });
    }

    private void onError(RequestError error){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onError(error);
            }
        });
    }

    @Override
    public void run() {
        String uri = this.vin + URI_FORMAT + EDMUNDS_API_KEY;
        networkHelper.getWithCustomUrl(BASE_URL, uri, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (response!= null){
                    try{
                        // parsing the response for styles ID
                        JSONObject jsonResponse = new JSONObject(response);
                        Log.d(TAG, response);
                        JSONArray years = jsonResponse.getJSONArray("years");
                        JSONObject first = years.getJSONObject(0);
                        JSONArray styles = first.getJSONArray("styles");
                        JSONObject obj = styles.getJSONObject(0);
                        String stylesID = Integer.toString(obj.getInt("id"));
                        GetCarStyleIDUSeCaseImpl.this.onStylesIDGot(stylesID);

                    }
                    catch (JSONException e) {
                        GetCarStyleIDUSeCaseImpl.this.onError(RequestError.getUnknownError());
                    }

                }
                else {
                    GetCarStyleIDUSeCaseImpl.this.callback.onError(requestError);

                }
            }
        });

    }



}
