package com.pitstop.network;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.castel.obd.util.Utils;
import com.goebl.david.Request;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.pitstop.BuildConfig;
import com.pitstop.ui.LoginActivity;
import com.pitstop.application.GlobalApplication;
import com.pitstop.utils.MixpanelHelper;
import com.pitstop.utils.NetworkHelper;

import static com.pitstop.utils.LogUtils.LOGD;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class HttpRequest {

    private static final String TAG = HttpRequest.class.getSimpleName();

    private static final String BASE_ENDPOINT = BuildConfig.SERVER_URL;
    private static Webb webClient;
    private RequestCallback listener;
    private RequestType requestType;
    private String uri;
    private JSONObject body;
    private HashMap<String, String> headers = new HashMap<>();
    private GlobalApplication application;

    private int retryAttempts = 0;

    private HttpRequest(RequestType requestType,
                        String uri,
                        HashMap<String, String> headers,
                        RequestCallback listener,
                        JSONObject body,
                        Context context
    ) {
        webClient = Webb.create();
        webClient.setBaseUri(BASE_ENDPOINT);
        this.uri = uri;
        this.requestType = requestType;
        this.headers = headers;
        this.listener = listener;
        this.body = body;

        application = context == null ? null : (GlobalApplication) context.getApplicationContext();
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
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response<String> response) {
            if (response != null) {
                if (response.isSuccess()) {
                    LOGD(TAG, response.getBody());
                    LOGD(TAG, response.getResponseMessage());
                    listener.done(response.getBody(), null);
                } else {
                    LOGD(TAG, "Error: " + response.getStatusLine());
                    LOGD(TAG, response.getResponseMessage());
                    LOGD(TAG, (String) response.getErrorBody());

                    if (response.getStatusCode() == 401) { // Unauthorized (must refresh)
                        // Error handling
                        NetworkHelper.refreshToken(application.getRefreshToken(), new RequestCallback() {
                            @Override
                            public void done(String response, RequestError requestError) {
                                if (requestError == null) {
                                    // try to parse the refresh token, if success then good, otherwise retry
                                    try {
                                        String newAccessToken = new JSONObject(response).getString("accessToken");
                                        application.setTokens(newAccessToken, application.getRefreshToken());
                                        headers.put("Authorization", "Bearer " + newAccessToken);
                                        executeAsync();
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                        // show failure
                                        showNetworkFailure(e.getMessage());
                                    }
                                } else {
                                    // show failure
                                    if (requestError.getStatusCode() == 400 && requestError.getError().contains("Invalid input")){
                                        logOut();
                                    }else {
                                        showNetworkFailure(requestError.getMessage());
                                    }
                                }
                            }
                        });
                    } else {
                        listener.done(null, RequestError
                                .jsonToRequestErrorObject((String) response.getErrorBody()));
                        //.setStatusCode(response.getStatusCode()));
                    }
                }
            } else {
                listener.done(null, RequestError.getUnknownError());
            }
        }

        private void logOut() {
            LOGD(TAG, "Refresh failed, logging out");
            application.logOutUser();
            Toast.makeText(application, "Your session has expired.  Please log in again.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(application, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            application.startActivity(intent);
        }

        private void showNetworkFailure(String message) {
            LOGD(TAG, "Refresh failed");
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
            // Show alert
            Toast.makeText(application, "Sorry, something weird is going on with our servers." +
                    "Please retry or email us at info@getpitstop.io", Toast.LENGTH_LONG).show();

            listener.done(null, RequestError.getUnknownError());
        }
//        private void retryGettingRefreshToken(){
//            NetworkHelper.refreshToken(application.getRefreshToken(), new RequestCallback() {
//                @Override
//                public void done(String response, RequestError requestError) {
//                    if (requestError == null) {
//                        // try to parse the refresh token, if success then good, otherwise retry
//                        try {
//                            String newAccessToken = new JSONObject(response).getString("accessToken");
//                            application.setTokens(newAccessToken, application.getRefreshToken());
//                            headers.put("Authorization", "Bearer " + newAccessToken);
//                            executeAsync();
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                            //retry
//                            retryAttempts++;
//                            retryGettingRefreshToken();
//                        }
//                    } else{
//                        showNetworkFailure();
//                    }
//                }
//            });
//        }
    }

    public static class Builder {

        private HashMap<String, String> headers;
        private String uri;
        private JSONObject body;
        private RequestType requestType;
        private RequestCallback callback;
        private Context context;

        public Builder() {
            this.headers = new HashMap<>();
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

        public HttpRequest createRequest() {
            return new HttpRequest(requestType, uri, headers, callback, body, context);
        }
    }
}
