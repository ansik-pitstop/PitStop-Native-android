package com.pitstop.interactors.get;

import android.os.Handler;

import com.pitstop.models.DebugMessage;
import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.Logger;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * Created by ishan on 2017-09-28.
 */

public class GetCarImagesArrayUseCaseImpl implements GetCarImagesArrayUseCase{

    private final String TAG = getClass().getSimpleName();

    public static final String BASE_URL = "https://api.edmunds.com/v1/api/vehiclephoto/service/findphotosbystyleid?styleId=";
    public static final String FORMAT_URL ="&fmt=json&api_key=";
    public static final String API_KEY = "9mu2f8rw93jaxtsj9dqkbtsx";
    private Handler useCaseHandler;
    private Handler mainHandler;
    private NetworkHelper networkHelper;
    private String stylesID;
    private GetCarImagesArrayUseCase.Callback callback;

    public GetCarImagesArrayUseCaseImpl(Handler useCaseHandler, Handler mainHandler,
                                    NetworkHelper networkHelper) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    private void onImagesArrayGot(String imageLink){
        Logger.getInstance().logI(TAG,"Use case execution finished: imageLink="+imageLink
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onArrayGot(imageLink));
    }

    private void onError(RequestError error){
        Logger.getInstance().logE(TAG,"Use case returned error: err="+error
                ,false, DebugMessage.TYPE_USE_CASE);
        mainHandler.post(() -> callback.onError(error));
    }

    @Override
    public void execute(String stylesID, Callback callback) {
        Logger.getInstance().logI(TAG,"Use case execution started: stylesID="+stylesID
                ,false, DebugMessage.TYPE_USE_CASE);
        this.stylesID = stylesID;
        this.callback = callback;
        useCaseHandler.post(this);
    }

    @Override
    public void run() {
        String uri = stylesID + FORMAT_URL + API_KEY;
        networkHelper.getWithCustomUrl(BASE_URL, uri, new RequestCallback() {
            @Override
            public void done(String response, RequestError requestError) {
                if (response!= null){
                    try {
                        JSONArray array = new JSONArray(response);
                        // look for children where subType = "exterior" and shottypeAbbreviation = "FQ"
                        for(int i = 0; i<array.length(); i++){
                            if ((array.getJSONObject(i).getString("subType").equalsIgnoreCase("exterior")) &&
                                    array.getJSONObject(i).getString("shotTypeAbbreviation").equalsIgnoreCase("FQ")){

                                JSONArray photoSrc= array.getJSONObject(i).getJSONArray("photoSrcs");
                                if (photoSrc.length() == 0)
                                    GetCarImagesArrayUseCaseImpl.this.onError(RequestError.getUnknownError());
                                else {
                                    for (int k = 0; i<photoSrc.length(); k++){
                                        if(photoSrc.getString(k).contains("500.jpg")) {
                                            GetCarImagesArrayUseCaseImpl.this.onImagesArrayGot(photoSrc.getString(k));
                                            return;
                                        }
                                    }
                                }
                            }
                        }
                        GetCarImagesArrayUseCaseImpl.this.onError(requestError);
                    } catch (JSONException e) {
                        GetCarImagesArrayUseCaseImpl.this.onError(RequestError.getUnknownError());
                    }
                }
                else {
                    GetCarImagesArrayUseCaseImpl.this.onError(requestError);
                }
            }
        });

    }
}
