package com.pitstop.interactors;

import android.os.Handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pitstop.models.Dealership;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matthew on 2017-06-19.
 */

public class GetPitstopShopsUseCaseImpl implements GetPitstopShopsUseCase {
    private NetworkHelper networkHelper;
    private GetPitstopShopsUseCase.Callback callback;
    private Handler handler;

    public GetPitstopShopsUseCaseImpl(NetworkHelper networkHelper, Handler handler){
        this.networkHelper = networkHelper;
        this.handler = handler;
    }

    @Override
    public void run() {
        if(!networkHelper.isConnected()){
            callback.onError();
        }
        networkHelper.getPitStopShops(new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {
                    if(response != null){
                        System.out.println("Testing Shops "+response);
                        try{
                            List<Dealership> list = Dealership.createDealershipList(response);
                            callback.onShopsGot(list);
                        }catch (JSONException e){
                            callback.onError();
                            e.printStackTrace();
                        }
                    }else if(requestError != null){
                        callback.onError();
                    }
                }
            }
        );
    }

    @Override
    public void execute(GetPitstopShopsUseCase.Callback callback) {
        this.callback = callback;
        handler.post(this);
    }
}
