package com.pitstop.DataAccessLayer;

import android.os.AsyncTask;
import android.util.Log;

import com.goebl.david.Request;
import com.goebl.david.Response;
import com.goebl.david.Webb;
import com.google.gson.Gson;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class FluentHttpClient {

    private static  final String BASE_ENDPOINT = "https://crackling-inferno-1642.firebaseio.com/";  //"http://52.35.99.168:10010/";
    private static Webb webClient;
    private HttpClientAsyncTask.onRequestExecutedListener listener;

    private HashMap<String, Object> bundle = new HashMap<>();

    private FluentHttpClient() {
        webClient = Webb.create();
        webClient.setBaseUri(BASE_ENDPOINT);
    }

    public static FluentHttpClient getFluentHttpClient() {
        return new FluentHttpClient();
    }

    public FluentHttpClient buildUrl(String resource) {
        bundle.put("resource",resource);
        return this;
    }

    public FluentHttpClient addRequestBody(HashMap<String, String> body) {
        bundle.put("requestBody", body);
        return this;
    }

    public FluentHttpClient setCallback(HttpClientAsyncTask.onRequestExecutedListener listener) {
        this.listener = listener;
        return this;
    }

    public void get() {

        String resource = bundle.get("resource").toString();
        if(resource == null || resource.equals("")) {
            return ;
        }

        HttpClientAsyncTask getAsync = new HttpClientAsyncTask();
        getAsync.setListener(listener);
        getAsync.execute(resource);
    }

    public static class HttpClientAsyncTask extends AsyncTask<Object, Object,Response<JSONObject> > {
        private onRequestExecutedListener listener;

        public void setListener(onRequestExecutedListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onPreExecute () {
            super.onPreExecute();
        }

        @Override
        protected Response<JSONObject> doInBackground (Object[] params) {
            Response<JSONObject> response = null;
            try {
                response = webClient.get(params[0].toString())
                        .header(Webb.HDR_ACCEPT, Webb.APP_JSON)
                        .asJsonObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(Response<JSONObject> jsonObjectResponse) {
            if(jsonObjectResponse!=null) {
                Log.i("GET", jsonObjectResponse.getBody().toString());
                listener.onSuccess(jsonObjectResponse.getBody());
            }
        }

        public interface onRequestExecutedListener {
            public void onSuccess(JSONObject obj);

            public void onError(JSONObject error);
        }
    }
}
