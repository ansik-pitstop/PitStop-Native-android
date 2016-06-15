package com.pitstop.DataAccessLayer.ServerAccess;

import android.os.AsyncTask;
import android.util.Log;

import com.castel.obd.util.Utils;
import com.goebl.david.Request;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.pitstop.MainActivity;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class HttpRequest {

    private static final String TAG = HttpRequest.class.getSimpleName();

    private static final boolean staging = !!!false;

    private static final String BASE_ENDPOINT = !!!!staging ? "http://staging.api.getpitstop.io:10010/" : "http://snapshot.api.getpitstop.io:10011/";
    private static Webb webClient;
    private RequestCallback listener;
    private RequestType requestType;
    private String uri;
    private JSONObject body;
    private HashMap<String, String> headers = new HashMap<>();

    private HttpRequest(RequestType requestType,
                        String uri,
                        HashMap<String, String> headers,
                        RequestCallback listener,
                        JSONObject body
                        ) {
        webClient = Webb.create();
        webClient.setBaseUri(BASE_ENDPOINT);
        this.uri = uri;
        this.requestType = requestType;
        this.headers = headers;
        this.listener = listener;
        this.body = body;
    }

    public void executeAsync() {
        if(Utils.isEmpty(uri)) {
            return;
        }

        if(listener == null) {
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

    public static class HttpClientAsyncTask extends AsyncTask<Object, Object, Response<String> > {
        private RequestCallback listener;

        public void setListener(RequestCallback listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute () {
            super.onPreExecute();
        }

        @Override
        protected Response<String> doInBackground (Object[] params) {
            Response<String> response = null;
            HashMap<String, String> headers = new HashMap<>();
            try {
                switch ((RequestType)params[1]) {
                    case GET: {
                        Request request = webClient.get(params[0].toString())
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON);

                        if(params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for(String key : headers.keySet()) {
                                request.header(key ,headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).asString();
                        break;
                    }

                    case POST: {
                        Request request = webClient.post(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                                .body(params[2]);

                        if(params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for(String key : headers.keySet()) {
                                request.header(key ,headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).asString();
                        break;
                    }

                    case PUT: {
                        Request request = webClient.put(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                                .body(params[2]);

                        if(params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for(String key : headers.keySet()) {
                                request.header(key ,headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).asString();
                        break;
                    }

                    case DELETE: {
                        Request request = webClient.delete(params[0].toString())
                                .header(Webb.HDR_CONTENT_TYPE, Webb.APP_JSON)
                                .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                                .body(params[2]);

                        if(params[3] instanceof HashMap) {
                            headers = (HashMap<String, String>) params[3];
                            for(String key : headers.keySet()) {
                                request.header(key ,headers.get(key));
                            }
                        }

                        response = request.connectTimeout(12000).asString();
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
            if(response != null) {
                if(response.isSuccess()) {
                    Log.i(TAG, response.getBody());
                    Log.i(TAG, response.getResponseMessage());
                    listener.done(response.getBody(),null);
                } else {
                    Log.i(TAG,"Error: "+response.getStatusLine());
                    Log.i(TAG, response.getResponseMessage());
                    Log.i(TAG, (String) response.getErrorBody());

                    listener.done(null,RequestError
                            .jsonToRequestErrorObject((String)response.getErrorBody()));
                }
            } else {
                listener.done(null, RequestError.getUnknownError());
            }
        }
    }

    public static class Builder {

        private HashMap<String, String> headers;
        private String uri;
        private JSONObject body;
        private RequestType requestType;
        private RequestCallback callback;

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

        public HttpRequest createRequest() {
            return new HttpRequest(requestType,uri,headers,callback,body);
        }
    }
}
