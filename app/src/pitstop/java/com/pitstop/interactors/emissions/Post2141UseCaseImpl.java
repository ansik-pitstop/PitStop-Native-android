package com.pitstop.interactors.emissions;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by Matt on 2017-08-28.
 */

public class Post2141UseCaseImpl implements Post2141UseCase {

    private String TAG = getClass().getSimpleName();

    private Callback callback;
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;
    private String pid;

    public Post2141UseCaseImpl(NetworkHelper networkHelper, Handler useCaseHandler, Handler mainHandler) {
        this.useCaseHandler = useCaseHandler;
        this.networkHelper = networkHelper;
        this.mainHandler = mainHandler;
    }

    private void onPIDPosted(JSONObject response){
        Logger.getInstance().logI(TAG, "Use case finished: result="+response
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onPIDPosted(response));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG, "Use case returned error: err="+error
                , DebugMessage.TYPE_USE_CASE);
        mainHandler.post( () -> callback.onError(error));
    }

    @Override
    public void execute(String pid,String deviceId, Callback callback) {
        Logger.getInstance().logI(TAG, "Use case execution started", DebugMessage.TYPE_USE_CASE);
        this.callback = callback;
        this.pid = pid;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {

        JSONObject body = new JSONObject();
        try{
            body.put("data",pid);
        }catch (JSONException e){
            e.printStackTrace();
        }
        networkHelper.post("demo/emissions/pid", (response, requestError) -> {

            if(response != null && requestError == null){
                System.out.println("Testing response " + response);
                try{
                    JSONObject responseJson = new JSONObject(response);
                    Post2141UseCaseImpl.this.onPIDPosted(responseJson);// I know this is bad, but its temporary
                }catch (JSONException e){
                    Post2141UseCaseImpl.this.onError(RequestError.getUnknownError());
                }

            }else if(response == null && requestError != null){
                System.out.println("Testing error "+requestError.getMessage());
                Post2141UseCaseImpl.this.onError(requestError);
            }
        },body);
    }
}
