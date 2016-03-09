package com.pitstop.DataAccessLayer;

import com.goebl.david.Webb;
import com.google.gson.Gson;
import java.util.HashMap;

/**
 * Created by Paul Soladoye  on 3/8/2016.
 */
public class FluentHttpClient {

    private static  final String BASE_ENDPOINT = "http://52.35.99.168:10010/";
    private static final Gson GSON = new Gson();
    private static Webb webClient;

    private HashMap<String, Object> bundle = new HashMap<>();
    private HashMap<String, String> headers = new HashMap<>();

    private FluentHttpClient() {
        webClient = Webb.create();
        webClient.setBaseUri(BASE_ENDPOINT);
    }

    public static FluentHttpClient getFluentHttpClient() {
        return new FluentHttpClient();
    }

    public FluentHttpClient buildUrl(String resource) {
        bundle.put("requestUrl",BASE_ENDPOINT+ resource);
        return this;
    }

    public FluentHttpClient addRequestHeader(String key, String value)  {
        headers.put(key,value);
        return this;
    }

    public FluentHttpClient addRequestBody(HashMap<String, String> body) {
        bundle.put("requestBody",body);
        return this;
    }

    public void get() {
        setHeaders();
        if(bundle.get("requestUrl") != null ) {

        }
    }

    private void setHeaders() {
        if(!headers.isEmpty()) {
            for(String key : headers.keySet()) {
                

            }
        }
    }
    
}
