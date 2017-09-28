package com.pitstop.interactors.get;

import com.pitstop.network.RequestCallback;
import com.pitstop.network.RequestError;
import com.pitstop.utils.NetworkHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.Handler;

/**
 * Created by ishan on 2017-09-28.
 */

public class GetCarImagesArrayUseCaseImpl implements GetCarImagesArrayUseCase{

    public static final String BASE_URL = "https://api.edmunds.com/v1/api/vehiclephoto/service/findphotosbystyleid?styleId=";
    public static final String FORMAT_URL ="&fmt=json&api_key=";
    public static final String API_KEY = "9mu2f8rw93jaxtsj9dqkbtsx";

    private android.os.Handler useCaseHandler;
    private android.os.Handler mainHandler;
    private NetworkHelper networkHelper;
    private String stylesID;
    private GetCarImagesArrayUseCase.Callback callback;

    public GetCarImagesArrayUseCaseImpl(android.os.Handler useCaseHandler, android.os.Handler mainHandler,
                                    NetworkHelper networkHelper) {
        this.useCaseHandler = useCaseHandler;
        this.mainHandler = mainHandler;
        this.networkHelper = networkHelper;
    }

    private void onImagesArrayGot(String imageLink){
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                callback.onArrayGot(imageLink);
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
    public void execute(String stylesID, Callback callback) {
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
