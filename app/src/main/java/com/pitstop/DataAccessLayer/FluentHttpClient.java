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

    private static  final String BASE_ENDPOINT = "http://52.35.99.168:10010/";
    private Webb webClient;

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

    public void get() {

        String resource = bundle.get("resource").toString();
        if(resource == null || resource.equals("")) {
            return ;
        }

        HttpClientAsyncTask getAsync = new HttpClientAsyncTask();
        getAsync.execute(resource);
        
    }

    public class HttpClientAsyncTask extends AsyncTask {
        @Override
        protected void onPreExecute () {
            super.onPreExecute();
        }

        @Override
        protected Object doInBackground (Object[] params) {
            Response<JSONObject> response = webClient.get(params[0].toString())
                    .header(Webb.HDR_ACCEPT,Webb.APP_JSON)
                    .asJsonObject();
            return response;
        }

        @Override
        protected void onPostExecute (Object o) {
            super.onPostExecute(o);
            Response<JSONObject> response = (Response<JSONObject>) o;
            Log.i("GET", response.toString());
        }

        @Override
        protected void onCancelled (Object o) {
            super.onCancelled(o);
        }
    }
}
