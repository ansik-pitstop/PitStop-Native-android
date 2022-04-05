package com.pitstop.network;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.castel.obd.util.Utils;
import com.goebl.david.Request;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.goebl.david.WebbException;
import com.google.gson.JsonObject;
import com.pitstop.R;
import com.pitstop.application.GlobalApplication;
import com.pitstop.models.DebugMessage;
import com.pitstop.retrofit.PitstopAuthApi;
import com.pitstop.retrofit.Token;
import com.pitstop.ui.login.LoginActivity;
import com.pitstop.utils.Logger;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.SecretUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class HttpRequest {

    private static final String TAG = HttpRequest.class.getSimpleName();
    private static Webb webClient;

    private static final int CONFLICT_CODE = 409;

    private final String BASE_ENDPOINT;
    private RequestCallback listener;
    private RequestType requestType;
    private String uri;
    private JSONObject body;
    private HashMap<String, String> headers = new HashMap<>();
    private GlobalApplication application;
    private Context context;
    private PitstopAuthApi pitstopAuthApi;
    public static Semaphore semaphore = new Semaphore(1);

    private HttpRequest(RequestType requestType,
                        String url,
                        String uri,
                        HashMap<String, String> headers,
                        RequestCallback listener,
                        JSONObject body,
                        PitstopAuthApi pitstopAuthApi,
                        Context context) {
        BASE_ENDPOINT = url == null ? SecretUtils.getEndpointUrl(context) : url;
        this.context = context;
        webClient = Webb.create();
        webClient.setBaseUri(BASE_ENDPOINT);
        this.uri = uri;
        this.requestType = requestType;
        this.headers = headers;
        this.listener = listener;
        this.body = body;
        this.pitstopAuthApi = pitstopAuthApi;

        Logger.getInstance().logD(TAG, requestType.type() + " REQUEST " + BASE_ENDPOINT + uri + (body != null ? ": " + body.toString() : ""),
                DebugMessage.TYPE_NETWORK);


        application = context == null ? null : (GlobalApplication) context.getApplicationContext();
    }

    private boolean isConnected(){
        ConnectivityManager connectivityManager
                = (ConnectivityManager) application.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void executeAsync() {
        if (Utils.isEmpty(uri)) {
            return;
        }

        if (listener == null) {
            listener = new RequestCallback() {
                @Override
                public void done(String response, RequestError requestError) {

                }
            };
        }

        //Check if connection exists before proceeding with request
        if (!isConnected()){
            listener.done(null, RequestError.getOfflineError());
            return;
        }

        HttpClientAsyncTask asyncRequest = new HttpClientAsyncTask();
        asyncRequest.setListener(listener);
        asyncRequest.execute(uri, requestType, body, headers);
    }

    private class HttpClientAsyncTask extends AsyncTask<Object, Object, Response<String>> {
        private RequestCallback listener;

        public void setListener(RequestCallback listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Response<String> doInBackground(Object[] params) {
            Response<String> response = null;
            HashMap<String, String> headers = new HashMap<>();
            try {
                switch ((RequestType) params[1]) {
                    case GET: {
                        Request request = webClient.get(params[0].toString())
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON);

                        if (params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for (String key : headers.keySet()) {
                                request.header(key, headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).readTimeout(12000).asString();
                        break;
                    }

                    case POST: {
                        Log.d(TAG,"Post request body: "+params[2]);
                        Request request = webClient.post(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                                .body(params[2]);

                        if (params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for (String key : headers.keySet()) {
                                request.header(key, headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).readTimeout(12000).asString();
                        break;
                    }

                    case PUT: {
                        Request request = webClient.put(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                                .body(params[2]);

                        if (params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for (String key : headers.keySet()) {
                                request.header(key, headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).readTimeout(12000).asString();
                        break;
                    }

                    case DELETE: {
                        Request request = webClient.delete(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON);

                        if (params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for (String key : headers.keySet()) {
                                request.header(key, headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).readTimeout(12000).asString();
                        break;
                    }
                }
                if (response.getStatusCode() == 401
                        && BASE_ENDPOINT.equals(SecretUtils.getEndpointUrl(context))) { // Unauthorized (must refresh)
                    try {
                        semaphore.acquire();

                        //In case different thread logged out session while waiting for sempahore
                        if (!application.isLoggedIn()) {
                            semaphore.release();
                            //Null so that the post execute doesn't return a response
                            return null;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    // Error handling
                    //Check if different thread already refreshed the token, if so don't refresh
                    if (!headers.get("Authorization").equals("Bearer " + application.getAccessToken())) {
                        Log.d(TAG, "Token has changed, sending request with new token without refresh");
                        headers.put("Authorization", "Bearer " + application.getAccessToken());
                        semaphore.release();
                        executeAsync();
                    }
                    //Otherwise refresh token and retry
                    else {
                        Log.d(TAG, "Token has not changed.");
                        Logger.getInstance().logD(TAG, "Access token refresh request being sent", DebugMessage.TYPE_NETWORK);
                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("refreshToken", application.getRefreshToken());
                        try {
                            retrofit2.Response<Token> accessTokenResponse = pitstopAuthApi.refreshAccessToken(jsonObject).execute();
                            if (accessTokenResponse.isSuccessful()) {
                                String token = accessTokenResponse.body().getAccessToken();
                                Log.d(TAG, "received new token: " + token);
                                application.setTokens(token, application.getRefreshToken());
                                executeAsync();
                            } else {
                                if (accessTokenResponse.code() == 400) {
                                    logOut();
                                } else {
                                    showNetworkFailure(RequestError.getOfflineError().getMessage());
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        semaphore.release();
                    }

                }
            } catch (WebbException e) {
                Logger.getInstance().logException(TAG,e,DebugMessage.TYPE_NETWORK);
                if (e.getCause() instanceof SocketTimeoutException){
                    Logger.getInstance().logE(TAG,"Network request timeout exception"
                            , DebugMessage.TYPE_NETWORK);
                    return null;
                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response<String> response) {
            if (response != null) {
                if(response.getStatusCode()== CONFLICT_CODE){// for shops
                    try{
                        JSONArray errorBody = new JSONArray(response.getErrorBody().toString());
                        listener.done(errorBody.get(0).toString(),null);//bear with me
                    }catch(JSONException e){
                        RequestError requestError = new RequestError();
                        requestError.setError(response.getErrorBody().toString());
                        listener.done(null,requestError);
                    }
                }
                else if (response.isSuccess()) {
                    String responseString;
                    try {
                        JSONObject responseJson = new JSONObject(response.getBody());
                        responseJson.remove("installationId");
                        responseString = responseJson.toString(4);
                    } catch (JSONException e) {
                        responseString = response.getBody();
                    }

                    Logger.getInstance().logD(TAG, requestType.type() + " RESPONSE " + BASE_ENDPOINT + uri + ": " + responseString,
                            DebugMessage.TYPE_NETWORK);

                    listener.done(response.getBody(), null);
                } else {
                    Logger.getInstance().logE(TAG, requestType.type() + " ERROR " + BASE_ENDPOINT + uri + ": "
                                    + response.getStatusLine() + " - " + response.getResponseMessage() + " - " + response.getErrorBody(),
                            DebugMessage.TYPE_NETWORK);

                    RequestError error = RequestError.getUnknownError();

                    error.setStatusCode(response.getStatusCode());
                    if (error.getMessage() != null)
                        error.setError(response.getResponseMessage());
                    if (response.getErrorBody() != null){
                        Log.e(TAG,"error body: "+response.getErrorBody());
                        try{
                            JSONObject errorBody = new JSONObject(response.getErrorBody().toString());
                            error.setMessage(errorBody.get("message").toString());
                        }catch(Exception e){
                            e.printStackTrace();
                        }

                    }

                    listener.done(null, error);
                }
            } else{
              listener.done(null,RequestError.getOfflineError());
            }
        }

        private void logOut() {
            //Logger.getInstance().LOGD(TAG, "Refresh failed, logging out");
            application.logOutUser();
            Toast.makeText(application, application.getString(R.string.log_in_again_toast), Toast.LENGTH_LONG).show();
            Intent intent = new Intent(application, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }

        private void showNetworkFailure(String message) {
            //Logger.getInstance().LOGD(TAG, "Refresh failed");
            // Track in mixpanel
            try {
                JSONObject properties = new JSONObject();
                properties.put("Alert Name", "Poor Server Connection");
                properties.put("Message", message);
                properties.put("View", "");
                application.getMixpanelAPI().track(MixpanelHelper.EVENT_ALERT_APPEARED, properties);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            listener.done(null, RequestError.getUnknownError());
        }
    }

    public static class Builder {

        private HashMap<String, String> headers;
        private String url;
        private String uri;
        private JSONObject body;
        private RequestType requestType;
        private RequestCallback callback;
        private Context context;
        private PitstopAuthApi pitstopAuthApi;

        public Builder() {
            this.headers = new HashMap<>();
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        public Builder uri(String uri) {
            this.uri = uri;
            return this;
        }

        public Builder requestType(RequestType type) {
            this.requestType = type;
            return this;
        }

        public Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        public Builder headers(HashMap<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder body(JSONObject body) {
            this.body = body;
            return this;
        }

        public Builder requestCallBack(RequestCallback callBack) {
            this.callback = callBack;
            return this;
        }

        public Builder context(Context context) {
            this.context = context;
            return this;
        }

        public Builder pitstopAuthApi(PitstopAuthApi pitstopAuthApi){
            this.pitstopAuthApi = pitstopAuthApi;
            return this;
        }

        public HttpRequest createRequest() {
            return new HttpRequest(requestType, url, uri, headers, callback, body
                    , pitstopAuthApi, context);
        }
    }
}
